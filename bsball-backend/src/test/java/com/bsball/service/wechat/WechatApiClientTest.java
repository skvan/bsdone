package com.bsball.service.wechat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("WechatApiClient：微信响应解析")
class WechatApiClientTest {

    @Test
    @DisplayName("token 成功响应")
    void tokenOk() {
        WechatApiClient.WechatApiResult r = WechatApiClient.parseResult(
                "{\"access_token\":\"ACCESS_TOKEN\",\"expires_in\":7200}", "access_token");
        assertTrue(r.ok());
        assertEquals(0, r.errcode());
        assertEquals("ACCESS_TOKEN", r.value());
        assertEquals(7200, r.expiresIn());
    }

    @Test
    @DisplayName("ticket 成功响应")
    void ticketOk() {
        WechatApiClient.WechatApiResult r = WechatApiClient.parseResult(
                "{\"errcode\":0,\"errmsg\":\"ok\",\"ticket\":\"TICKET\",\"expires_in\":7200}", "ticket");
        assertTrue(r.ok());
        assertEquals("TICKET", r.value());
    }

    @Test
    @DisplayName("errcode!=0：ok=false 且携带错误码与信息")
    void err() {
        WechatApiClient.WechatApiResult r = WechatApiClient.parseResult(
                "{\"errcode\":40013,\"errmsg\":\"invalid appid\"}", "access_token");
        assertFalse(r.ok());
        assertEquals(40013, r.errcode());
        assertEquals("invalid appid", r.errmsg());
        assertEquals("", r.value());
    }

    @Test
    @DisplayName("非法 JSON：不抛异常，ok=false")
    void badJson() {
        WechatApiClient.WechatApiResult r = WechatApiClient.parseResult("not-json", "ticket");
        assertFalse(r.ok());
    }
}
