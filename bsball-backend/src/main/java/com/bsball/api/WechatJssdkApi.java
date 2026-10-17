package com.bsball.api;

import com.bsball.common.Result;
import com.bsball.config.WechatProperties;
import com.bsball.service.wechat.WechatTokenService;
import com.bsball.utils.WechatSignatureUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信 JSSDK 签名端点（免登录：application.yml app.api.guest-whitelist 前缀 /wechat）。
 * 幂等、无副作用；只返回公开信息（appId + 签名），不返回任何凭证；前端对非成功一律静默降级。
 */
@RestController
@RequestMapping({"/wechat"})
public class WechatJssdkApi {
    private static final int MAX_URL_LENGTH = 1024;

    private final WechatTokenService tokenService;
    private final WechatProperties props;

    public WechatJssdkApi(WechatTokenService tokenService, WechatProperties props) {
        this.tokenService = tokenService;
        this.props = props;
    }

    /**
     * 获取当前页面的 JSSDK 签名配置。
     *
     * @param url 当前页面完整 URL（不含 # 片段，由前端负责）
     * @return 成功时 data 含 appId/timestamp(秒)/nonceStr/signature；参数非法 400、未配置 503、签名失败 502
     */
    @GetMapping({"/jssdk-config"})
    public Result<Map<String, Object>> jssdkConfig(@RequestParam(value = "url", required = false) String url) {
        if (!isValidUrl(url)) {
            return Result.fail(400, "invalid url");
        }
        if (!props.isConfigured()) {
            return Result.fail(503, "wechat not configured");
        }
        try {
            String ticket = tokenService.getJsapiTicket();
            long timestamp = System.currentTimeMillis() / 1000L;
            String nonceStr = WechatSignatureUtil.randomNonceStr();
            String signature = WechatSignatureUtil.sign(ticket, nonceStr, timestamp, url);
            Map<String, Object> data = new HashMap<>();
            data.put("appId", props.getAppId());
            data.put("timestamp", timestamp);
            data.put("nonceStr", nonceStr);
            data.put("signature", signature);
            return Result.ok(data);
        } catch (Exception e) {
            // 网络/微信侧任何失败均降级为 Result.fail，绝不 500；不记录异常原文，避免凭证或请求 URI 入日志
            return Result.fail(502, "wechat signature unavailable");
        }
    }

    /** url 校验：非空、≤1024、http(s)、host 命中 allowedHosts（忽略大小写） */
    private boolean isValidUrl(String url) {
        if (url == null || url.isBlank() || url.length() > MAX_URL_LENGTH) {
            return false;
        }
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) {
                return false;
            }
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                return false;
            }
            return props.getAllowedHosts() != null && props.getAllowedHosts().stream()
                    .filter(h -> h != null && !h.isBlank())
                    .anyMatch(h -> h.trim().equalsIgnoreCase(host));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
