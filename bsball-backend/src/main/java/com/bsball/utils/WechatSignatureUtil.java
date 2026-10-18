package com.bsball.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * 微信 JSSDK 签名工具：SHA1(jsapi_ticket=..&noncestr=..&timestamp=..&url=..)，官方算法、字段序固定，输出小写 hex。
 */
public final class WechatSignatureUtil {
    private static final char[] HEX = "0123456789abcdef".toCharArray();
    private static final String NONCE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private WechatSignatureUtil() {
    }

    /** 计算分享签名（40 位小写 hex）。url 为当前页面完整 URL（不含 # 片段）。 */
    public static String sign(String ticket, String nonceStr, long timestamp, String url) {
        String raw = "jsapi_ticket=" + ticket + "&noncestr=" + nonceStr + "&timestamp=" + timestamp + "&url=" + url;
        return sha1Hex(raw);
    }

    /** 生成 16 位随机串（字母数字） */
    public static String randomNonceStr() {
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            sb.append(NONCE_CHARS.charAt(RANDOM.nextInt(NONCE_CHARS.length())));
        }
        return sb.toString();
    }

    private static String sha1Hex(String text) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-1").digest(text.getBytes(StandardCharsets.UTF_8));
            char[] out = new char[bytes.length * 2];
            for (int i = 0; i < bytes.length; i++) {
                int v = bytes[i] & 0xFF;
                out[i * 2] = HEX[v >>> 4];
                out[i * 2 + 1] = HEX[v & 0x0F];
            }
            return new String(out);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-1 不可用", e);
        }
    }
}
