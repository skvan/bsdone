/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.core.StartupAccessUrlLogger
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.boot.context.event.ApplicationReadyEvent
 *  org.springframework.context.ApplicationListener
 *  org.springframework.core.env.Environment
 *  org.springframework.stereotype.Component
 *  org.springframework.util.StringUtils
 */
package com.bsball.core;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class StartupAccessUrlLogger
implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger log = LoggerFactory.getLogger(StartupAccessUrlLogger.class);
    private final Environment env;

    public StartupAccessUrlLogger(Environment env) {
        this.env = env;
    }

    public void onApplicationEvent(ApplicationReadyEvent event) {
        String protocol = "http";
        int port = this.env.getProperty("local.server.port", Integer.class, 8080);
        String contextPath = this.normalizePath(this.env.getProperty("server.servlet.context-path", "/"));
        String localUrl = protocol + "://localhost:" + port + contextPath;
        log.info("Local:   {}", (Object)localUrl);
        for (String host : this.listNetworkHosts()) {
            log.info("Network: {}://{}:{}{}", new Object[]{protocol, host, port, contextPath});
        }
    }

    private String normalizePath(String path) {
        String value;
        value = StringUtils.hasText(path) ? path.trim() : "/";
        if (!value.startsWith("/")) {
            value = "/" + value;
        }
        if (!value.endsWith("/")) {
            value = value + "/";
        }
        return value;
    }

    private List<String> listNetworkHosts() {
        ArrayList<String> hosts = new ArrayList<String>();
        try {
            for (NetworkInterface nif : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!this.isUsable(nif)) continue;
                for (InetAddress addr : Collections.list(nif.getInetAddresses())) {
                    if (!(addr instanceof Inet4Address) || addr.isLoopbackAddress()) continue;
                    hosts.add(addr.getHostAddress());
                }
            }
        }
        catch (SocketException e) {
            log.warn("Failed to list network interfaces", (Throwable)e);
        }
        return hosts;
    }

    private boolean isUsable(NetworkInterface nif) throws SocketException {
        return nif.isUp() && !nif.isLoopback() && !nif.isVirtual();
    }
}

