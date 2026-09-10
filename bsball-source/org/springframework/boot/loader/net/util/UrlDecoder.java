/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  java.util.HexFormat
 */
package org.springframework.boot.loader.net.util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

public final class UrlDecoder {
    private UrlDecoder() {
    }

    public static String decode(String source) {
        return UrlDecoder.decode(source, StandardCharsets.UTF_8);
    }

    public static String decode(String source, Charset charset) {
        int length = source.length();
        int firstPercentIndex = source.indexOf(37);
        if (length == 0 || firstPercentIndex < 0) {
            return source;
        }
        StringBuilder output = new StringBuilder(length);
        output.append(source, 0, firstPercentIndex);
        byte[] bytes = null;
        int i = firstPercentIndex;
        while (i < length) {
            char ch = source.charAt(i);
            if (ch == '%') {
                try {
                    if (bytes == null) {
                        bytes = new byte[(length - i) / 3];
                    }
                    int pos = 0;
                    while (i + 2 < length && ch == '%') {
                        bytes[pos++] = (byte)HexFormat.fromHexDigits((CharSequence)source, (int)(i + 1), (int)(i + 3));
                        if ((i += 3) >= length) continue;
                        ch = source.charAt(i);
                    }
                    if (i < length && ch == '%') {
                        throw new IllegalArgumentException("Incomplete trailing escape (%) pattern");
                    }
                    output.append(new String(bytes, 0, pos, charset));
                    continue;
                }
                catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Invalid encoded sequence \"" + source.substring(i) + "\"");
                }
            }
            output.append(ch);
            ++i;
        }
        return output.toString();
    }
}

