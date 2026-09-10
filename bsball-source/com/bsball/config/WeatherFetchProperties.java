/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.config.WeatherFetchProperties
 *  lombok.Generated
 *  org.springframework.boot.context.properties.ConfigurationProperties
 *  org.springframework.stereotype.Component
 */
package com.bsball.config;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Generated;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="app.weather-fetch")
public class WeatherFetchProperties {
    private boolean enabled = false;
    private String cron = "0 40 5 * * *";
    private String saveDir = "resources/weather";
    private int diluting = 15;
    private int connectTimeoutMs = 8000;
    private int readTimeoutMs = 15000;
    private Map<String, String> jsonDataMap = WeatherFetchProperties.defaultJsonDataMap();
    private Map<String, String> arrayDataMap = WeatherFetchProperties.defaultArrayDataMap();
    private Map<String, String> jsonArrayDataMap = WeatherFetchProperties.defaultJsonArrayDataMap();

    private static Map<String, String> defaultJsonDataMap() {
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        map.put("rainfall24", "http://typhoon.nmc.cn/weatherservice/diamond14/rainfall/24.json");
        map.put("rainfall48", "http://typhoon.nmc.cn/weatherservice/diamond14/rainfall/48.json");
        map.put("rainfall72", "http://typhoon.nmc.cn/weatherservice/diamond14/rainfall/72.json");
        map.put("fog", "http://typhoon.nmc.cn/weatherservice/diamond14/fog/json");
        map.put("haze", "http://typhoon.nmc.cn/weatherservice/diamond14/haze/json");
        map.put("dust", "http://typhoon.nmc.cn/weatherservice/diamond14/disastrous/dust.json");
        map.put("windAndTemperature", "http://typhoon.nmc.cn/weatherservice/diamond14/disastrous/windAndTemperature.json");
        return map;
    }

    private static Map<String, String> defaultArrayDataMap() {
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        map.put("warning", "http://typhoon.nmc.cn/weatherservice/fetch_json/warning/json");
        map.put("inland24", "http://typhoon.nmc.cn/weatherservice/fetch_json/weather_station/1");
        map.put("inland48", "http://typhoon.nmc.cn/weatherservice/fetch_json/weather_station/2");
        map.put("inland72", "http://typhoon.nmc.cn/weatherservice/fetch_json/weather_station/3");
        return map;
    }

    private static Map<String, String> defaultJsonArrayDataMap() {
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        map.put("coast24", "http://typhoon.nmc.cn/weatherservice/diamond8/view/0_24.json");
        map.put("coast48", "http://typhoon.nmc.cn/weatherservice/diamond8/view/0_48.json");
        map.put("coast72", "http://typhoon.nmc.cn/weatherservice/diamond8/view/0_72.json");
        map.put("inshore24", "http://typhoon.nmc.cn/weatherservice/diamond8/view/1_24.json");
        map.put("inshore48", "http://typhoon.nmc.cn/weatherservice/diamond8/view/1_48.json");
        map.put("inshore72", "http://typhoon.nmc.cn/weatherservice/diamond8/view/1_72.json");
        map.put("pelagic24", "http://typhoon.nmc.cn/weatherservice/diamond8/view/2_24.json");
        map.put("pelagic48", "http://typhoon.nmc.cn/weatherservice/diamond8/view/2_48.json");
        map.put("pelagic72", "http://typhoon.nmc.cn/weatherservice/diamond8/view/2_72.json");
        return map;
    }

    @Generated
    public boolean isEnabled() {
        return this.enabled;
    }

    @Generated
    public String getCron() {
        return this.cron;
    }

    @Generated
    public String getSaveDir() {
        return this.saveDir;
    }

    @Generated
    public int getDiluting() {
        return this.diluting;
    }

    @Generated
    public int getConnectTimeoutMs() {
        return this.connectTimeoutMs;
    }

    @Generated
    public int getReadTimeoutMs() {
        return this.readTimeoutMs;
    }

    @Generated
    public Map<String, String> getJsonDataMap() {
        return this.jsonDataMap;
    }

    @Generated
    public Map<String, String> getArrayDataMap() {
        return this.arrayDataMap;
    }

    @Generated
    public Map<String, String> getJsonArrayDataMap() {
        return this.jsonArrayDataMap;
    }

    @Generated
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Generated
    public void setCron(String cron) {
        this.cron = cron;
    }

    @Generated
    public void setSaveDir(String saveDir) {
        this.saveDir = saveDir;
    }

    @Generated
    public void setDiluting(int diluting) {
        this.diluting = diluting;
    }

    @Generated
    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    @Generated
    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    @Generated
    public void setJsonDataMap(Map<String, String> jsonDataMap) {
        this.jsonDataMap = jsonDataMap;
    }

    @Generated
    public void setArrayDataMap(Map<String, String> arrayDataMap) {
        this.arrayDataMap = arrayDataMap;
    }

    @Generated
    public void setJsonArrayDataMap(Map<String, String> jsonArrayDataMap) {
        this.jsonArrayDataMap = jsonArrayDataMap;
    }
}

