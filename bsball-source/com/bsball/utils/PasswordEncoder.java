/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.utils.PasswordEncoder
 *  org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
 */
package com.bsball.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class PasswordEncoder {
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    public static String encode(CharSequence rawPassword) {
        return rawPassword == null ? null : ENCODER.encode((CharSequence)rawPassword.toString());
    }

    public static boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null && encodedPassword == null) {
            return true;
        }
        if (rawPassword == null || encodedPassword == null || encodedPassword.isEmpty()) {
            return false;
        }
        return ENCODER.matches((CharSequence)rawPassword.toString(), encodedPassword);
    }

    public static boolean isEncoded(String value) {
        return value != null && value.startsWith("$2");
    }
}

