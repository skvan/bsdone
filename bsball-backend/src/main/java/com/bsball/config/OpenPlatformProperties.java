/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.config.OpenPlatformProperties
 *  com.bsball.config.OpenPlatformProperties$Common
 *  com.bsball.config.OpenPlatformProperties$Lbs
 *  lombok.Generated
 *  org.springframework.boot.context.properties.ConfigurationProperties
 */
package com.bsball.config;

import com.bsball.config.OpenPlatformProperties;
import lombok.Generated;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix="app.open-platform")
public class OpenPlatformProperties {
    private Common common = new Common();
    private Lbs lbs = new Lbs();

    public static int effectiveMinIntervalMs(int maxQps, int extraSpacingMs) {
        int q = Math.max(1, maxQps);
        int base = (int)Math.ceil(1000.0 / (double)q);
        return base + Math.max(0, extraSpacingMs);
    }

    public boolean anyProviderConfigured() {
        return this.lbs.getProviders().values().stream().anyMatch(p -> p.isEnabled() && p.getKey() != null && !p.getKey().isBlank());
    }

    public int maxBatchIps() {
        return Math.max(1, this.common.getMaxBatchIps());
    }

    public int regionStaleDays() {
        return this.common.getRegionStaleDays();
    }

    public int maxAttempts() {
        return Math.max(1, this.lbs.getMaxAttempts());
    }

    @Generated
    public OpenPlatformProperties() {
    }

    @Generated
    public Common getCommon() {
        return this.common;
    }

    @Generated
    public Lbs getLbs() {
        return this.lbs;
    }

    @Generated
    public void setCommon(Common common) {
        this.common = common;
    }

    @Generated
    public void setLbs(Lbs lbs) {
        this.lbs = lbs;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof OpenPlatformProperties)) {
            return false;
        }
        OpenPlatformProperties other = (OpenPlatformProperties)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Common this$common = this.getCommon();
        Common other$common = other.getCommon();
        if (this$common == null ? other$common != null : !this$common.equals(other$common)) {
            return false;
        }
        Lbs this$lbs = this.getLbs();
        Lbs other$lbs = other.getLbs();
        return !(this$lbs == null ? other$lbs != null : !this$lbs.equals(other$lbs));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof OpenPlatformProperties;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Common $common = this.getCommon();
        result = result * 59 + ($common == null ? 43 : $common.hashCode());
        Lbs $lbs = this.getLbs();
        result = result * 59 + ($lbs == null ? 43 : $lbs.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "OpenPlatformProperties(common=" + String.valueOf(this.getCommon()) + ", lbs=" + String.valueOf(this.getLbs()) + ")";
    }

    public static class Common {
        private int maxBatchIps = 60;
        private int regionCacheHours = 168;
        private long regionCacheMaxSize = 50000L;
        private int regionStaleDays = 90;

        @Generated
        public Common() {
        }

        @Generated
        public int getMaxBatchIps() {
            return this.maxBatchIps;
        }

        @Generated
        public int getRegionCacheHours() {
            return this.regionCacheHours;
        }

        @Generated
        public long getRegionCacheMaxSize() {
            return this.regionCacheMaxSize;
        }

        @Generated
        public int getRegionStaleDays() {
            return this.regionStaleDays;
        }

        @Generated
        public void setMaxBatchIps(int maxBatchIps) {
            this.maxBatchIps = maxBatchIps;
        }

        @Generated
        public void setRegionCacheHours(int regionCacheHours) {
            this.regionCacheHours = regionCacheHours;
        }

        @Generated
        public void setRegionCacheMaxSize(long regionCacheMaxSize) {
            this.regionCacheMaxSize = regionCacheMaxSize;
        }

        @Generated
        public void setRegionStaleDays(int regionStaleDays) {
            this.regionStaleDays = regionStaleDays;
        }

        @Generated
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof Common)) {
                return false;
            }
            OpenPlatformProperties.Common other = (Common)o;
            if (!other.canEqual(this)) {
                return false;
            }
            if (this.getMaxBatchIps() != other.getMaxBatchIps()) {
                return false;
            }
            if (this.getRegionCacheHours() != other.getRegionCacheHours()) {
                return false;
            }
            if (this.getRegionCacheMaxSize() != other.getRegionCacheMaxSize()) {
                return false;
            }
            return this.getRegionStaleDays() == other.getRegionStaleDays();
        }

        @Generated
        protected boolean canEqual(Object other) {
            return other instanceof Common;
        }

        @Generated
        public int hashCode() {
            int PRIME = 59;
            int result = 1;
            result = result * 59 + this.getMaxBatchIps();
            result = result * 59 + this.getRegionCacheHours();
            long $regionCacheMaxSize = this.getRegionCacheMaxSize();
            result = result * 59 + (int)($regionCacheMaxSize >>> 32 ^ $regionCacheMaxSize);
            result = result * 59 + this.getRegionStaleDays();
            return result;
        }

        @Generated
        public String toString() {
            return "OpenPlatformProperties.Common(maxBatchIps=" + this.getMaxBatchIps() + ", regionCacheHours=" + this.getRegionCacheHours() + ", regionCacheMaxSize=" + this.getRegionCacheMaxSize() + ", regionStaleDays=" + this.getRegionStaleDays() + ")";
        }
    }

    public static class IpLocationApi {
        private String endpoint = "";
        private int maxQps = 1;
        private int extraSpacingMs = 0;
        private String ipParam = "ip";
        private String keyParam = "key";
        private String idParam = "id";
        private Map<String, String> fixedParams = new LinkedHashMap<String, String>();

        @Generated
        public IpLocationApi() {
        }

        @Generated
        public String getEndpoint() {
            return this.endpoint;
        }

        @Generated
        public int getMaxQps() {
            return this.maxQps;
        }

        @Generated
        public int getExtraSpacingMs() {
            return this.extraSpacingMs;
        }

        @Generated
        public String getIpParam() {
            return this.ipParam;
        }

        @Generated
        public String getKeyParam() {
            return this.keyParam;
        }

        @Generated
        public String getIdParam() {
            return this.idParam;
        }

        @Generated
        public Map<String, String> getFixedParams() {
            return this.fixedParams;
        }

        @Generated
        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        @Generated
        public void setMaxQps(int maxQps) {
            this.maxQps = maxQps;
        }

        @Generated
        public void setExtraSpacingMs(int extraSpacingMs) {
            this.extraSpacingMs = extraSpacingMs;
        }

        @Generated
        public void setIpParam(String ipParam) {
            this.ipParam = ipParam;
        }

        @Generated
        public void setKeyParam(String keyParam) {
            this.keyParam = keyParam;
        }

        @Generated
        public void setIdParam(String idParam) {
            this.idParam = idParam;
        }

        @Generated
        public void setFixedParams(Map<String, String> fixedParams) {
            this.fixedParams = fixedParams;
        }

        @Generated
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof IpLocationApi)) {
                return false;
            }
            OpenPlatformProperties.IpLocationApi other = (IpLocationApi)o;
            if (!other.canEqual(this)) {
                return false;
            }
            if (this.getMaxQps() != other.getMaxQps()) {
                return false;
            }
            if (this.getExtraSpacingMs() != other.getExtraSpacingMs()) {
                return false;
            }
            String this$endpoint = this.getEndpoint();
            String other$endpoint = other.getEndpoint();
            if (this$endpoint == null ? other$endpoint != null : !this$endpoint.equals(other$endpoint)) {
                return false;
            }
            String this$ipParam = this.getIpParam();
            String other$ipParam = other.getIpParam();
            if (this$ipParam == null ? other$ipParam != null : !this$ipParam.equals(other$ipParam)) {
                return false;
            }
            String this$keyParam = this.getKeyParam();
            String other$keyParam = other.getKeyParam();
            if (this$keyParam == null ? other$keyParam != null : !this$keyParam.equals(other$keyParam)) {
                return false;
            }
            String this$idParam = this.getIdParam();
            String other$idParam = other.getIdParam();
            if (this$idParam == null ? other$idParam != null : !this$idParam.equals(other$idParam)) {
                return false;
            }
            Map<String, String> this$fixedParams = this.getFixedParams();
            Map<String, String> other$fixedParams = other.getFixedParams();
            return !(this$fixedParams == null ? other$fixedParams != null : !((Object)this$fixedParams).equals(other$fixedParams));
        }

        @Generated
        protected boolean canEqual(Object other) {
            return other instanceof IpLocationApi;
        }

        @Generated
        public int hashCode() {
            int PRIME = 59;
            int result = 1;
            result = result * 59 + this.getMaxQps();
            result = result * 59 + this.getExtraSpacingMs();
            String $endpoint = this.getEndpoint();
            result = result * 59 + ($endpoint == null ? 43 : $endpoint.hashCode());
            String $ipParam = this.getIpParam();
            result = result * 59 + ($ipParam == null ? 43 : $ipParam.hashCode());
            String $keyParam = this.getKeyParam();
            result = result * 59 + ($keyParam == null ? 43 : $keyParam.hashCode());
            String $idParam = this.getIdParam();
            result = result * 59 + ($idParam == null ? 43 : $idParam.hashCode());
            Map<String, String> $fixedParams = this.getFixedParams();
            result = result * 59 + ($fixedParams == null ? 43 : ((Object)$fixedParams).hashCode());
            return result;
        }

        @Generated
        public String toString() {
            return "OpenPlatformProperties.IpLocationApi(endpoint=" + this.getEndpoint() + ", maxQps=" + this.getMaxQps() + ", extraSpacingMs=" + this.getExtraSpacingMs() + ", ipParam=" + this.getIpParam() + ", keyParam=" + this.getKeyParam() + ", idParam=" + this.getIdParam() + ", fixedParams=" + String.valueOf(this.getFixedParams()) + ")";
        }
    }

    public static class Lbs {
        private int maxAttempts = 3;
        private String strategy = "parallel";
        private Map<String, OpenPlatformProperties.Provider> providers = new LinkedHashMap<String, OpenPlatformProperties.Provider>();

        @Generated
        public Lbs() {
        }

        @Generated
        public int getMaxAttempts() {
            return this.maxAttempts;
        }

        @Generated
        public String getStrategy() {
            return this.strategy;
        }

        @Generated
        public Map<String, OpenPlatformProperties.Provider> getProviders() {
            return this.providers;
        }

        @Generated
        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        @Generated
        public void setStrategy(String strategy) {
            this.strategy = strategy;
        }

        @Generated
        public void setProviders(Map<String, OpenPlatformProperties.Provider> providers) {
            this.providers = providers;
        }

        @Generated
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof Lbs)) {
                return false;
            }
            OpenPlatformProperties.Lbs other = (Lbs)o;
            if (!other.canEqual(this)) {
                return false;
            }
            if (this.getMaxAttempts() != other.getMaxAttempts()) {
                return false;
            }
            String this$strategy = this.getStrategy();
            String other$strategy = other.getStrategy();
            if (this$strategy == null ? other$strategy != null : !this$strategy.equals(other$strategy)) {
                return false;
            }
            Map<String, OpenPlatformProperties.Provider> this$providers = this.getProviders();
            Map<String, OpenPlatformProperties.Provider> other$providers = other.getProviders();
            return !(this$providers == null ? other$providers != null : !((Object)this$providers).equals(other$providers));
        }

        @Generated
        protected boolean canEqual(Object other) {
            return other instanceof Lbs;
        }

        @Generated
        public int hashCode() {
            int PRIME = 59;
            int result = 1;
            result = result * 59 + this.getMaxAttempts();
            String $strategy = this.getStrategy();
            result = result * 59 + ($strategy == null ? 43 : $strategy.hashCode());
            Map<String, OpenPlatformProperties.Provider> $providers = this.getProviders();
            result = result * 59 + ($providers == null ? 43 : ((Object)$providers).hashCode());
            return result;
        }

        @Generated
        public String toString() {
            return "OpenPlatformProperties.Lbs(maxAttempts=" + this.getMaxAttempts() + ", strategy=" + this.getStrategy() + ", providers=" + String.valueOf(this.getProviders()) + ")";
        }
    }

    public static class Provider {
        private boolean enabled = false;
        private String id = "";
        private String key = "";
        private OpenPlatformProperties.IpLocationApi ipLocationApi = new IpLocationApi();

        @Generated
        public Provider() {
        }

        @Generated
        public boolean isEnabled() {
            return this.enabled;
        }

        @Generated
        public String getId() {
            return this.id;
        }

        @Generated
        public String getKey() {
            return this.key;
        }

        @Generated
        public IpLocationApi getIpLocationApi() {
            return this.ipLocationApi;
        }

        @Generated
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @Generated
        public void setId(String id) {
            this.id = id;
        }

        @Generated
        public void setKey(String key) {
            this.key = key;
        }

        @Generated
        public void setIpLocationApi(IpLocationApi ipLocationApi) {
            this.ipLocationApi = ipLocationApi;
        }

        @Generated
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof Provider)) {
                return false;
            }
            OpenPlatformProperties.Provider other = (Provider)o;
            if (!other.canEqual(this)) {
                return false;
            }
            if (this.isEnabled() != other.isEnabled()) {
                return false;
            }
            String this$id = this.getId();
            String other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) {
                return false;
            }
            String this$key = this.getKey();
            String other$key = other.getKey();
            if (this$key == null ? other$key != null : !this$key.equals(other$key)) {
                return false;
            }
            OpenPlatformProperties.IpLocationApi this$ipLocationApi = this.getIpLocationApi();
            OpenPlatformProperties.IpLocationApi other$ipLocationApi = other.getIpLocationApi();
            return !(this$ipLocationApi == null ? other$ipLocationApi != null : !((Object)this$ipLocationApi).equals(other$ipLocationApi));
        }

        @Generated
        protected boolean canEqual(Object other) {
            return other instanceof Provider;
        }

        @Generated
        public int hashCode() {
            int PRIME = 59;
            int result = 1;
            result = result * 59 + (this.isEnabled() ? 79 : 97);
            String $id = this.getId();
            result = result * 59 + ($id == null ? 43 : $id.hashCode());
            String $key = this.getKey();
            result = result * 59 + ($key == null ? 43 : $key.hashCode());
            OpenPlatformProperties.IpLocationApi $ipLocationApi = this.getIpLocationApi();
            result = result * 59 + ($ipLocationApi == null ? 43 : ((Object)$ipLocationApi).hashCode());
            return result;
        }

        @Generated
        public String toString() {
            return "OpenPlatformProperties.Provider(enabled=" + this.isEnabled() + ", id=" + this.getId() + ", key=" + this.getKey() + ", ipLocationApi=" + String.valueOf(this.getIpLocationApi()) + ")";
        }
    }
}





