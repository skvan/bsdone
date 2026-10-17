package com.bsball.api;

import com.bsball.common.Result;
import com.bsball.config.WechatProperties;
import com.bsball.exception.BusinessException;
import com.bsball.service.wechat.WechatTokenService;
import com.bsball.utils.WechatSignatureUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WechatJssdkApi：参数校验与签名返回")
class WechatJssdkApiTest {

    @Mock
    WechatTokenService tokenService;

    WechatProperties props;
    WechatJssdkApi api;

    @BeforeEach
    void setUp() {
        props = new WechatProperties();
        props.setAppId("wx-app");
        props.setAppSecret("secret");
        props.setAllowedHosts(List.of("www.bsdone.com"));
        api = new WechatJssdkApi(tokenService, props);
    }

    @Test
    @DisplayName("缺参/超长/ftp 协议/非白名单主机 → 400 且不调 service")
    void invalidUrls() {
        assertEquals(400, api.jssdkConfig(null).getCode());
        assertEquals(400, api.jssdkConfig("").getCode());
        assertEquals(400, api.jssdkConfig("ftp://www.bsdone.com/a").getCode());
        assertEquals(400, api.jssdkConfig("https://evil.example.com/x").getCode());
        assertEquals(400, api.jssdkConfig("https://www.bsdone.com/" + "a".repeat(1024)).getCode());
        verifyNoInteractions(tokenService);
    }

    @Test
    @DisplayName("未配置凭证 → 503")
    void notConfigured() {
        props.setAppSecret("");
        Result<Map<String, Object>> r = api.jssdkConfig("https://www.bsdone.com/a");
        assertEquals(503, r.getCode());
        verifyNoInteractions(tokenService);
    }

    @Test
    @DisplayName("happy path：字段齐全且签名可按算法复算")
    void happyPath() {
        when(tokenService.getJsapiTicket()).thenReturn("TK");
        String url = "https://www.bsdone.com/bs-ball-next/team1/players/9?from=share";
        Result<Map<String, Object>> r = api.jssdkConfig(url);
        assertEquals(200, r.getCode());
        Map<String, Object> data = r.getData();
        assertEquals("wx-app", data.get("appId"));
        String nonce = (String) data.get("nonceStr");
        long ts = ((Number) data.get("timestamp")).longValue();
        assertTrue(nonce.matches("[A-Za-z0-9]{16}"));
        assertEquals(WechatSignatureUtil.sign("TK", nonce, ts, url), data.get("signature"));
    }

    @Test
    @DisplayName("主机校验忽略大小写")
    void hostCaseInsensitive() {
        when(tokenService.getJsapiTicket()).thenReturn("TK");
        assertEquals(200, api.jssdkConfig("https://WWW.BSDONE.COM/a").getCode());
    }

    @Test
    @DisplayName("service 失败（业务/网络异常）→ 502 不抛出")
    void serviceFails() {
        when(tokenService.getJsapiTicket()).thenThrow(new BusinessException("x"));
        assertEquals(502, api.jssdkConfig("https://www.bsdone.com/a").getCode());
        reset(tokenService);
        when(tokenService.getJsapiTicket()).thenThrow(new RuntimeException("network"));
        assertEquals(502, api.jssdkConfig("https://www.bsdone.com/a").getCode());
    }
}
