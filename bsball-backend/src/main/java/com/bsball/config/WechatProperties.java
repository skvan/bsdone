package com.bsball.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 微信服务号配置（app.wechat.*）。appSecret 真实值由服务器 systemd EnvironmentFile 注入，禁止写入仓库文件与日志。
 */
@Component
@ConfigurationProperties(prefix = "app.wechat")
public class WechatProperties {
    /** 服务号 AppID（非机密） */
    private String appId = "";
    /** 服务号 AppSecret（机密：仅 env 注入） */
    private String appSecret = "";
    /** JSSDK 签名 URL 允许的站点主机（忽略大小写） */
    private List<String> allowedHosts = new ArrayList<>();

    public boolean isConfigured() {
        return appId != null && !appId.isBlank() && appSecret != null && !appSecret.isBlank();
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public List<String> getAllowedHosts() {
        return allowedHosts;
    }

    public void setAllowedHosts(List<String> allowedHosts) {
        this.allowedHosts = allowedHosts;
    }
}
