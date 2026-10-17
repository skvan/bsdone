package com.bsball.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("WechatSignatureUtil：JSSDK 签名算法")
class WechatSignatureUtilTest {

    @Test
    @DisplayName("官方文档固定向量（JS-SDK 说明页示例）")
    void officialVector() {
        // 期望值经 sha1sum 实证：见微信 JS-SDK 官方文档示例
        String ticket = "sM4AOVdWfPE4DxkXGEs8VMCPGGVi4C3VM0P37wVUCFvkVAy_90u5h9nbSlYy3-Sl-HhTdfl2fzFy1AOcHKP7qg";
        assertEquals("0f9de62fce790f9a083d5c99e95740ceb90c27ed",
                WechatSignatureUtil.sign(ticket, "Wm3WZYTPz0wzccnW", 1414587457L, "http://mp.weixin.qq.com?params=value"));
    }

    @Test
    @DisplayName("含 query 的 https URL：字段序固定、原样拼接")
    void queryUrlVector() {
        assertEquals("c188101a97f571a42de0ac93a2734a85ad3d7d9b",
                WechatSignatureUtil.sign("TICKET", "AbCd1234EfGh5678", 1700000000L,
                        "https://www.bsdone.com/bs-ball-next/team1/players/9?from=share"));
    }

    @Test
    @DisplayName("randomNonceStr 输出 16 位字母数字")
    void nonceStr() {
        String n = WechatSignatureUtil.randomNonceStr();
        assertEquals(16, n.length());
        assertTrue(n.matches("[A-Za-z0-9]{16}"), "实际=" + n);
    }
}
