/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.config.InitSeedProperties
 *  lombok.Generated
 *  org.springframework.boot.context.properties.ConfigurationProperties
 */
package com.bsball.config;

import lombok.Generated;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="app.init")
public class InitSeedProperties {
    private long seedTenantId = 0L;
    private boolean importTestData = false;
    private boolean seedBusinessMenusAndApis = true;
    private boolean seedContentMenusAndApis = true;
    private boolean seedAuditMenusAndApis = true;
    private boolean seedDefaultStadiums = false;
    private boolean recordLoginLog = true;
    private boolean importChinaRegion = true;

    @Generated
    public long getSeedTenantId() {
        return this.seedTenantId;
    }

    @Generated
    public boolean isImportTestData() {
        return this.importTestData;
    }

    @Generated
    public boolean isSeedBusinessMenusAndApis() {
        return this.seedBusinessMenusAndApis;
    }

    @Generated
    public boolean isSeedContentMenusAndApis() {
        return this.seedContentMenusAndApis;
    }

    @Generated
    public boolean isSeedAuditMenusAndApis() {
        return this.seedAuditMenusAndApis;
    }

    @Generated
    public boolean isSeedDefaultStadiums() {
        return this.seedDefaultStadiums;
    }

    @Generated
    public boolean isRecordLoginLog() {
        return this.recordLoginLog;
    }

    @Generated
    public boolean isImportChinaRegion() {
        return this.importChinaRegion;
    }

    @Generated
    public void setSeedTenantId(long seedTenantId) {
        this.seedTenantId = seedTenantId;
    }

    @Generated
    public void setImportTestData(boolean importTestData) {
        this.importTestData = importTestData;
    }

    @Generated
    public void setSeedBusinessMenusAndApis(boolean seedBusinessMenusAndApis) {
        this.seedBusinessMenusAndApis = seedBusinessMenusAndApis;
    }

    @Generated
    public void setSeedContentMenusAndApis(boolean seedContentMenusAndApis) {
        this.seedContentMenusAndApis = seedContentMenusAndApis;
    }

    @Generated
    public void setSeedAuditMenusAndApis(boolean seedAuditMenusAndApis) {
        this.seedAuditMenusAndApis = seedAuditMenusAndApis;
    }

    @Generated
    public void setSeedDefaultStadiums(boolean seedDefaultStadiums) {
        this.seedDefaultStadiums = seedDefaultStadiums;
    }

    @Generated
    public void setRecordLoginLog(boolean recordLoginLog) {
        this.recordLoginLog = recordLoginLog;
    }

    @Generated
    public void setImportChinaRegion(boolean importChinaRegion) {
        this.importChinaRegion = importChinaRegion;
    }
}

