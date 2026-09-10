/*
 * Decompiled with CFR 0.152.
 */
package org.springframework.boot.loader.net.protocol.jar;

import java.net.URL;
import java.util.Objects;

final class JarFileUrlKey {
    private final String protocol;
    private final String host;
    private final int port;
    private final String file;
    private final boolean runtimeRef;

    JarFileUrlKey(URL url) {
        this.protocol = url.getProtocol();
        this.host = url.getHost();
        this.port = url.getPort() != -1 ? url.getPort() : url.getDefaultPort();
        this.file = url.getFile();
        this.runtimeRef = "runtime".equals(url.getRef());
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        JarFileUrlKey other = (JarFileUrlKey)obj;
        return Objects.equals(this.file, other.file) && this.equalsIgnoringCase(this.protocol, other.protocol) && this.equalsIgnoringCase(this.host, other.host) && this.port == other.port && this.runtimeRef == other.runtimeRef;
    }

    public int hashCode() {
        return Objects.hashCode(this.file);
    }

    private boolean equalsIgnoringCase(String s1, String s2) {
        return s1 == s2 || s1 != null && s1.equalsIgnoreCase(s2);
    }
}

