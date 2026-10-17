package com.bsball.service.wechat;

import com.bsball.config.WechatProperties;
import com.bsball.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WechatTokenService：缓存/单飞/自愈")
class WechatTokenServiceTest {

    @Mock
    WechatApiClient client;

    WechatProperties props;
    WechatTokenService service;
    final AtomicLong now = new AtomicLong(1_000_000L);

    private static WechatApiClient.WechatApiResult ok(String value, int expiresIn) {
        return new WechatApiClient.WechatApiResult(true, 0, "ok", value, expiresIn);
    }

    private static WechatApiClient.WechatApiResult err(int code) {
        return new WechatApiClient.WechatApiResult(false, code, "err", "", 0);
    }

    @BeforeEach
    void setUp() {
        props = new WechatProperties();
        props.setAppId("wx-test");
        props.setAppSecret("secret");
        service = new WechatTokenService(client, props);
        service.clock = now::get;
    }

    @Test
    @DisplayName("首次获取：token 与 ticket 各请求一次")
    void firstFetch() {
        when(client.fetchAccessToken()).thenReturn(ok("T1", 7200));
        when(client.fetchJsapiTicket("T1")).thenReturn(ok("TK1", 7200));
        assertEquals("TK1", service.getJsapiTicket());
        verify(client).fetchAccessToken();
        verify(client).fetchJsapiTicket("T1");
    }

    @Test
    @DisplayName("缓存命中：未临期不重复请求")
    void cacheHit() {
        when(client.fetchAccessToken()).thenReturn(ok("T1", 7200));
        when(client.fetchJsapiTicket("T1")).thenReturn(ok("TK1", 7200));
        service.getJsapiTicket();
        now.addAndGet(100_000L);
        assertEquals("TK1", service.getJsapiTicket());
        verify(client, times(1)).fetchAccessToken();
        verify(client, times(1)).fetchJsapiTicket("T1");
    }

    @Test
    @DisplayName("临期（剩余<10 分钟）触发刷新")
    void refreshWhenNearExpiry() {
        when(client.fetchAccessToken()).thenReturn(ok("T1", 7200), ok("T2", 7200));
        when(client.fetchJsapiTicket("T1")).thenReturn(ok("TK1", 7200));
        when(client.fetchJsapiTicket("T2")).thenReturn(ok("TK2", 7200));
        service.getJsapiTicket();
        now.addAndGet(7200_000L - 599_000L);
        assertEquals("TK2", service.getJsapiTicket());
        verify(client, times(2)).fetchAccessToken();
    }

    @Test
    @DisplayName("getticket 40001：清 token 缓存重试一次成功（自愈）")
    void selfHealOn40001() {
        when(client.fetchAccessToken()).thenReturn(ok("T1", 7200), ok("T2", 7200));
        when(client.fetchJsapiTicket("T1")).thenReturn(err(40001));
        when(client.fetchJsapiTicket("T2")).thenReturn(ok("TK2", 7200));
        assertEquals("TK2", service.getJsapiTicket());
        verify(client, times(2)).fetchAccessToken();
        verify(client, times(2)).fetchJsapiTicket(anyString());
    }

    @Test
    @DisplayName("重试仍失败：抛 BusinessException")
    void retryStillFails() {
        when(client.fetchAccessToken()).thenReturn(ok("T1", 7200), ok("T2", 7200));
        when(client.fetchJsapiTicket(anyString())).thenReturn(err(40001));
        assertThrows(BusinessException.class, () -> service.getJsapiTicket());
    }

    @Test
    @DisplayName("未配置凭证：抛 BusinessException 且不触网")
    void notConfigured() {
        props.setAppSecret("");
        assertThrows(BusinessException.class, () -> service.getJsapiTicket());
        verifyNoInteractions(client);
    }

    @Test
    @DisplayName("token 获取失败（40013）：抛 BusinessException")
    void tokenFetchFails() {
        when(client.fetchAccessToken()).thenReturn(err(40013));
        assertThrows(BusinessException.class, () -> service.getJsapiTicket());
    }

    @Test
    @DisplayName("token 传输异常：降级为 BusinessException，消息不含凭证/URI，无 cause")
    void tokenTransportFailure() {
        when(client.fetchAccessToken()).thenThrow(new RuntimeException(
                "I/O error on GET request for \"https://api.weixin.qq.com/cgi-bin/token"
                        + "?secret=SECRET_VALUE&access_token=TOKEN_VALUE\""));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.getJsapiTicket());
        assertSanitized(ex);
    }

    @Test
    @DisplayName("ticket 传输异常：降级为 BusinessException，消息不含凭证/URI，无 cause")
    void ticketTransportFailure() {
        when(client.fetchAccessToken()).thenReturn(ok("T1", 7200));
        when(client.fetchJsapiTicket("T1")).thenThrow(new RuntimeException(
                "I/O error on GET request for \"https://api.weixin.qq.com/cgi-bin/ticket/getticket"
                        + "?access_token=TOKEN_VALUE&secret=SECRET_VALUE\""));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.getJsapiTicket());
        assertSanitized(ex);
    }

    private static void assertSanitized(BusinessException ex) {
        assertEquals("微信服务暂不可用", ex.getMessage());
        assertFalse(ex.getMessage().contains("secret"));
        assertFalse(ex.getMessage().contains("http"));
        assertFalse(ex.getMessage().contains("TOKEN_VALUE"));
        assertNull(ex.getCause());
    }
}
