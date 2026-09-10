/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.config.OpenPlatformProperties
 *  com.bsball.config.OpenPlatformProperties$IpLocationApi
 *  com.bsball.config.OpenPlatformProperties$Provider
 *  com.bsball.model.enums.LbsProvider
 *  com.bsball.service.iplocation.InterfaceBoxIpLocationProvider
 *  com.bsball.service.iplocation.IpGcj02
 *  com.bsball.service.iplocation.IpLocationDetail
 *  com.bsball.service.iplocation.IpLocationProvider
 *  com.bsball.service.iplocation.IpRegionTextFormatter
 *  com.bsball.service.iplocation.TokenBucketRateLimiter
 *  com.fasterxml.jackson.databind.JsonNode
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  lombok.Generated
 *  org.locationtech.jts.geom.Point
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.http.HttpStatusCode
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
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/*
 * Exception performing whole class analysis ignored.
 */
@Component
public class InterfaceBoxIpLocationProvider
implements IpLocationProvider {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(InterfaceBoxIpLocationProvider.class);
    private static final RestClient REST = RestClient.create();
    private static final long QOS_COOLDOWN_MS = 300000L;
    private final OpenPlatformProperties props;
    private final ObjectMapper objectMapper;
    private volatile long qosBlockedUntilMs;
    private volatile TokenBucketRateLimiter rateLimiter;

    public boolean isConfigured() {
        OpenPlatformProperties.Provider p = (OpenPlatformProperties.Provider)this.props.getLbs().getProviders().get("interface-box");
        return p != null && p.isEnabled() && p.getId() != null && !p.getId().isBlank() && p.getKey() != null && !p.getKey().isBlank();
    }

    public LbsProvider lbsProvider() {
        return LbsProvider.INTERFACE_BOX;
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
        OpenPlatformProperties.Provider p = (OpenPlatformProperties.Provider)this.props.getLbs().getProviders().get("interface-box");
        if (p == null) {
            return Optional.empty();
        }
        OpenPlatformProperties.IpLocationApi api = p.getIpLocationApi();
        String endpoint = api.getEndpoint();
        if (endpoint == null || endpoint.isBlank()) {
            return Optional.empty();
        }
        UriComponentsBuilder b = UriComponentsBuilder.fromUriString((String)endpoint).queryParam(api.getIpParam(), new Object[]{ip}).queryParam(api.getKeyParam(), new Object[]{p.getKey().trim()});
        if (p.getId() != null && !p.getId().isBlank()) {
            b.queryParam(api.getIdParam(), new Object[]{p.getId().trim()});
        }
        for (Map.Entry e : api.getFixedParams().entrySet()) {
            b.queryParam((String)e.getKey(), new Object[]{e.getValue()});
        }
        URI uri = b.encode(StandardCharsets.UTF_8).build().toUri();
        try {
            String formatted;
            String body = (String)REST.get().uri(uri).exchange((req, resp) -> {
                HttpStatusCode status = resp.getStatusCode();
                byte[] bytes = resp.getBody().readAllBytes();
                String text = new String(bytes, StandardCharsets.UTF_8);
                if (!status.is2xxSuccessful()) {
                    log.warn("\u63a5\u53e3\u76d2\u5b50 IP \u5b9a\u4f4d HTTP \u975e\u6210\u529f: status={}, endpointPath={}, bodySnippet={}", new Object[]{status.value(), InterfaceBoxIpLocationProvider.safeEndpointPath((URI)uri), InterfaceBoxIpLocationProvider.snippetForLog((String)text)});
                    return null;
                }
                if (InterfaceBoxIpLocationProvider.looksLikeHtml((String)text)) {
                    log.warn("\u63a5\u53e3\u76d2\u5b50 IP \u5b9a\u4f4d\u8fd4\u56de\u7591\u4f3c HTML\uff08\u901a\u5e38\u662f endpoint \u914d\u6210\u4e86\u6587\u6863\u9875/\u9519\u8bef\u8def\u5f84\uff09: endpointPath={}, bodySnippet={}", (Object)InterfaceBoxIpLocationProvider.safeEndpointPath((URI)uri), (Object)InterfaceBoxIpLocationProvider.snippetForLog((String)text));
                    return null;
                }
                return text;
            });
            if (body == null || body.isBlank()) {
                Optional<IpLocationDetail> optional = Optional.empty();
                return optional;
            }
            JsonNode root = this.objectMapper.readTree(body);
            int code = root.path("code").asInt(-1);
            if (code != 200) {
                if (InterfaceBoxIpLocationProvider.isQosLike((String)root.path("msg").asText(""))) {
                    this.qosBlockedUntilMs = System.currentTimeMillis() + 300000L;
                    log.warn("\u63a5\u53e3\u76d2\u5b50 IP \u5b9a\u4f4d\u89e6\u53d1\u9650\u6d41/\u914d\u989d\uff0c\u8fdb\u5165\u51b7\u5374 {} \u79d2", (Object)300L);
                }
                Optional<IpLocationDetail> optional = Optional.empty();
                return optional;
            }
            String prov = root.path("sheng").asText("");
            String city = root.path("shi").asText("");
            String adcode = InterfaceBoxIpLocationProvider.firstNonBlank((String[])new String[]{root.path("qucode").asText(""), root.path("shicode").asText(""), root.path("shengcode").asText("")});
            String msg = root.path("msg").asText("");
            String string = formatted = msg != null && !msg.isBlank() ? msg : IpRegionTextFormatter.format((String)prov, (String)city);
            if (formatted == null || formatted.isBlank()) {
                Optional<IpLocationDetail> optional = Optional.empty();
                return optional;
            }
            Point gcjPt = this.parseLocation(root);
            Optional<IpLocationDetail> optional = Optional.of(new IpLocationDetail(formatted.trim(), InterfaceBoxIpLocationProvider.blankToNull((String)prov), InterfaceBoxIpLocationProvider.blankToNull((String)city), InterfaceBoxIpLocationProvider.blankToNull((String)adcode), null, gcjPt, LbsProvider.INTERFACE_BOX));
            return optional;
        }
        catch (Exception e) {
            log.debug("\u63a5\u53e3\u76d2\u5b50 IP \u5b9a\u4f4d\u5f02\u5e38: endpointPath={}, err={}", (Object)InterfaceBoxIpLocationProvider.safeEndpointPath((URI)uri), (Object)e.toString());
            Optional<IpLocationDetail> optional = Optional.empty();
            return optional;
        }
        finally {
            this.getRateLimiter().markHttpComplete();
        }
    }

    private Point parseLocation(JsonNode root) {
        String lon = root.path("lon").asText("");
        String lat = root.path("lat").asText("");
        if (lon.isBlank() || lat.isBlank()) {
            return null;
        }
        try {
            return IpGcj02.point((double)Double.parseDouble(lon.trim()), (double)Double.parseDouble(lat.trim()));
        }
        catch (Exception ex) {
            return null;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private TokenBucketRateLimiter getRateLimiter() {
        if (this.rateLimiter == null) {
            InterfaceBoxIpLocationProvider interfaceBoxIpLocationProvider = this;
            synchronized (interfaceBoxIpLocationProvider) {
                if (this.rateLimiter == null) {
                    OpenPlatformProperties.Provider p = (OpenPlatformProperties.Provider)this.props.getLbs().getProviders().get("interface-box");
                    OpenPlatformProperties.IpLocationApi api = p != null ? p.getIpLocationApi() : new OpenPlatformProperties.IpLocationApi();
                    int qps = Math.max(1, api.getMaxQps());
                    this.rateLimiter = new TokenBucketRateLimiter((double)qps, api.getExtraSpacingMs());
                }
            }
        }
        return this.rateLimiter;
    }

    private static boolean isQosLike(String msg) {
        if (msg == null || msg.isBlank()) {
            return false;
        }
        String m = msg.toLowerCase();
        return m.contains("qps") || m.contains("qos") || m.contains("quota") || m.contains("limit") || m.contains("\u9891\u6b21") || m.contains("\u914d\u989d") || m.contains("\u9650\u6d41");
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    private static String firstNonBlank(String ... arr) {
        for (String s : arr) {
            if (s == null || s.isBlank()) continue;
            return s.trim();
        }
        return "";
    }

    private static String safeEndpointPath(URI uri) {
        if (uri == null) {
            return "";
        }
        String path = uri.getPath();
        return path != null ? path : "";
    }

    private static boolean looksLikeHtml(String body) {
        if (body == null) {
            return false;
        }
        String s = body.stripLeading();
        if (s.isEmpty()) {
            return false;
        }
        char c = s.charAt(0);
        return c == '<';
    }

    private static String snippetForLog(String body) {
        if (body == null) {
            return "";
        }
        String s = body.replace('\r', ' ').replace('\n', ' ').trim();
        if (s.length() <= 240) {
            return s;
        }
        return s.substring(0, 240) + "...";
    }

    @Generated
    public InterfaceBoxIpLocationProvider(OpenPlatformProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
    }
}

