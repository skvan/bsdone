/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.config.OpenPlatformProperties
 *  com.bsball.config.OpenPlatformProperties$IpLocationApi
 *  com.bsball.config.OpenPlatformProperties$Provider
 *  com.bsball.model.enums.LbsProvider
 *  com.bsball.service.iplocation.IpGcj02
 *  com.bsball.service.iplocation.IpLocationDetail
 *  com.bsball.service.iplocation.IpLocationProvider
 *  com.bsball.service.iplocation.IpRegionTextFormatter
 *  com.bsball.service.iplocation.TencentIpLocationProvider
 *  com.bsball.service.iplocation.TokenBucketRateLimiter
 *  com.fasterxml.jackson.databind.JsonNode
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  lombok.Generated
 *  org.locationtech.jts.geom.Point
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Component
 *  org.springframework.web.client.RestClient
 *  org.springframework.web.util.UriComponentsBuilder
 */
package com.bsball.service.iplocation;

import com.bsball.config.OpenPlatformProperties;
import com.bsball.model.enums.LbsProvider;
import com.bsball.service.iplocation.IpGcj02;
import com.bsball.service.iplocation.IpLocationDetail;
import com.bsball.service.iplocation.IpLocationProvider;
import com.bsball.service.iplocation.IpRegionTextFormatter;
import com.bsball.service.iplocation.TokenBucketRateLimiter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import lombok.Generated;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/*
 * Exception performing whole class analysis ignored.
 */
@Component
public class TencentIpLocationProvider
implements IpLocationProvider {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(TencentIpLocationProvider.class);
    private static final RestClient REST = RestClient.create();
    private static final long QOS_COOLDOWN_MS = 300000L;
    private final OpenPlatformProperties props;
    private final ObjectMapper objectMapper;
    private volatile long qosBlockedUntilMs;
    private volatile TokenBucketRateLimiter rateLimiter;

    public boolean isConfigured() {
        OpenPlatformProperties.Provider p = (OpenPlatformProperties.Provider)this.props.getLbs().getProviders().get("tencent");
        return p != null && p.isEnabled() && p.getKey() != null && !p.getKey().isBlank();
    }

    public LbsProvider lbsProvider() {
        return LbsProvider.TENCENT;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public Optional<IpLocationDetail> locate(String ip) {
        if (!this.isConfigured()) {
            return Optional.empty();
        }
        long now = System.currentTimeMillis();
        if (now < this.qosBlockedUntilMs) {
            return Optional.empty();
        }
        this.getRateLimiter().acquire();
        OpenPlatformProperties.Provider p = (OpenPlatformProperties.Provider)this.props.getLbs().getProviders().get("tencent");
        if (p == null) {
            return Optional.empty();
        }
        OpenPlatformProperties.IpLocationApi api = p.getIpLocationApi();
        String endpoint = api.getEndpoint() != null && !api.getEndpoint().isBlank() ? api.getEndpoint() : "https://apis.map.qq.com/ws/location/v1/ip";
        UriComponentsBuilder b = UriComponentsBuilder.fromUriString((String)endpoint).queryParam(api.getKeyParam(), new Object[]{p.getKey().trim()}).queryParam(api.getIpParam(), new Object[]{ip});
        for (Map.Entry e : api.getFixedParams().entrySet()) {
            b.queryParam((String)e.getKey(), new Object[]{e.getValue()});
        }
        URI uri = b.encode(StandardCharsets.UTF_8).build().toUri();
        try {
            String body = (String)REST.get().uri(uri).retrieve().body(String.class);
            if (body == null || body.isBlank()) {
                Optional<IpLocationDetail> optional = Optional.empty();
                return optional;
            }
            JsonNode root = this.objectMapper.readTree(body);
            if (root.path("status").asInt(-1) != 0) {
                if (TencentIpLocationProvider.isQosLike((String)root.path("message").asText(""))) {
                    this.qosBlockedUntilMs = System.currentTimeMillis() + 300000L;
                    log.warn("\u817e\u8baf IP \u5b9a\u4f4d\u89e6\u53d1\u9650\u6d41/\u914d\u989d\uff0c\u8fdb\u5165\u51b7\u5374 {} \u79d2", (Object)300L);
                }
                Optional<IpLocationDetail> optional = Optional.empty();
                return optional;
            }
            JsonNode ad = root.path("result").path("ad_info");
            String prov = ad.path("province").asText("");
            String city = ad.path("city").asText("");
            String adcode = ad.path("adcode").asText("");
            String formatted = IpRegionTextFormatter.format((String)prov, (String)city);
            if (formatted.isEmpty()) {
                Optional<IpLocationDetail> optional = Optional.empty();
                return optional;
            }
            Point gcjPt = TencentIpLocationProvider.parseTencentLocationGcj((JsonNode)root);
            Optional<IpLocationDetail> optional = Optional.of(new IpLocationDetail(formatted, TencentIpLocationProvider.blankToNull((String)prov), TencentIpLocationProvider.blankToNull((String)city), TencentIpLocationProvider.blankToNull((String)adcode), null, gcjPt, LbsProvider.TENCENT));
            return optional;
        }
        catch (Exception e) {
            log.debug("\u817e\u8baf IP \u5b9a\u4f4d\u5f02\u5e38: {}", (Object)e.getMessage());
            Optional<IpLocationDetail> optional = Optional.empty();
            return optional;
        }
        finally {
            this.getRateLimiter().markHttpComplete();
        }
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    private static boolean isQosLike(String msg) {
        if (msg == null || msg.isBlank()) {
            return false;
        }
        String m = msg.toLowerCase();
        return m.contains("qps") || m.contains("qos") || m.contains("quota") || m.contains("limit") || m.contains("\u9891\u6b21") || m.contains("\u914d\u989d") || m.contains("\u9650\u6d41");
    }

    private static Point parseTencentLocationGcj(JsonNode root) {
        JsonNode loc = root.path("result").path("location");
        if (loc.isMissingNode() || loc.isNull()) {
            return null;
        }
        double lat = loc.path("lat").asDouble(Double.NaN);
        double lng = loc.path("lng").asDouble(Double.NaN);
        if (Double.isNaN(lat) || Double.isNaN(lng)) {
            return null;
        }
        return IpGcj02.point((double)lng, (double)lat);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private TokenBucketRateLimiter getRateLimiter() {
        if (this.rateLimiter == null) {
            TencentIpLocationProvider tencentIpLocationProvider = this;
            synchronized (tencentIpLocationProvider) {
                if (this.rateLimiter == null) {
                    OpenPlatformProperties.Provider p = (OpenPlatformProperties.Provider)this.props.getLbs().getProviders().get("tencent");
                    OpenPlatformProperties.IpLocationApi api = p != null ? p.getIpLocationApi() : new OpenPlatformProperties.IpLocationApi();
                    int qps = Math.max(1, api.getMaxQps());
                    this.rateLimiter = new TokenBucketRateLimiter((double)qps, api.getExtraSpacingMs());
                }
            }
        }
        return this.rateLimiter;
    }

    @Generated
    public TencentIpLocationProvider(OpenPlatformProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
    }
}

