package com.bsball.service.wechat;

import com.bsball.config.WechatProperties;
import com.bsball.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.util.function.LongSupplier;

/**
 * access_token / jsapi_ticket 内存缓存：TTL 取微信 expires_in；剩余 <10 分钟视为临期刷新；
 * synchronized 单飞防并发互踢；getticket 40001/42001 → 清 token 重试一次自愈。
 * 生产单实例部署，无需分布式锁。凭证与 token 不得写入日志。
 * 传输层异常统一降级为 BusinessException，原始异常（可能含带凭证的请求 URI）不保留、不记录。
 */
@Service
public class WechatTokenService {
    private static final long REFRESH_AHEAD_MS = 10 * 60 * 1000L;

    private final WechatApiClient client;
    private final WechatProperties props;

    /** 测试可注入时钟（生产默认系统时间） */
    LongSupplier clock = System::currentTimeMillis;

    private String cachedToken = "";
    private long tokenExpireAtMs = 0L;
    private String cachedTicket = "";
    private long ticketExpireAtMs = 0L;

    public WechatTokenService(WechatApiClient client, WechatProperties props) {
        this.client = client;
        this.props = props;
    }

    /** 获取 jsapi_ticket；未配置或微信侧失败抛 BusinessException（调用方降级处理） */
    public synchronized String getJsapiTicket() {
        requireConfigured();
        String token = accessToken();
        if (isFresh(cachedTicket, ticketExpireAtMs)) {
            return cachedTicket;
        }
        WechatApiClient.WechatApiResult first = safeFetchTicket(token);
        if (first.ok()) {
            cacheTicket(first);
            return cachedTicket;
        }
        if (isTokenInvalid(first.errcode())) {
            clearTokenCache();
            WechatApiClient.WechatApiResult retry = safeFetchTicket(accessToken());
            if (retry.ok()) {
                cacheTicket(retry);
                return cachedTicket;
            }
            throw new BusinessException("微信 jsapi_ticket 获取失败：" + retry.errcode());
        }
        throw new BusinessException("微信 jsapi_ticket 获取失败：" + first.errcode());
    }

    private String accessToken() {
        if (isFresh(cachedToken, tokenExpireAtMs)) {
            return cachedToken;
        }
        WechatApiClient.WechatApiResult r = safeFetchToken();
        if (!r.ok()) {
            throw new BusinessException("微信 access_token 获取失败：" + r.errcode());
        }
        cachedToken = r.value();
        tokenExpireAtMs = clock.getAsLong() + r.expiresIn() * 1000L;
        return cachedToken;
    }

    /** 传输层异常降级：不保留原始异常，避免含 appSecret/token 的请求 URI 进入日志 */
    private WechatApiClient.WechatApiResult safeFetchToken() {
        try {
            return client.fetchAccessToken();
        } catch (RuntimeException e) {
            throw new BusinessException("微信服务暂不可用");
        }
    }

    /** 传输层异常降级：不保留原始异常，避免含 access_token 的请求 URI 进入日志 */
    private WechatApiClient.WechatApiResult safeFetchTicket(String token) {
        try {
            return client.fetchJsapiTicket(token);
        } catch (RuntimeException e) {
            throw new BusinessException("微信服务暂不可用");
        }
    }

    private void cacheTicket(WechatApiClient.WechatApiResult r) {
        cachedTicket = r.value();
        ticketExpireAtMs = clock.getAsLong() + r.expiresIn() * 1000L;
    }

    private boolean isFresh(String value, long expireAtMs) {
        return value != null && !value.isEmpty() && clock.getAsLong() < expireAtMs - REFRESH_AHEAD_MS;
    }

    private void clearTokenCache() {
        cachedToken = "";
        tokenExpireAtMs = 0L;
    }

    private static boolean isTokenInvalid(int errcode) {
        return errcode == 40001 || errcode == 42001;
    }

    private void requireConfigured() {
        if (!props.isConfigured()) {
            throw new BusinessException("微信服务号未配置");
        }
    }
}
