/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.config.WeatherFetchProperties
 *  com.bsball.service.WeatherDataFetchService
 *  com.fasterxml.jackson.core.type.TypeReference
 *  com.fasterxml.jackson.databind.JsonNode
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  java.net.http.HttpClient
 *  java.net.http.HttpRequest
 *  java.net.http.HttpResponse
 *  java.net.http.HttpResponse$BodyHandlers
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
 *  org.springframework.scheduling.annotation.Scheduled
 *  org.springframework.stereotype.Service
 */
package com.bsball.service;

import com.bsball.config.WeatherFetchProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix="app.weather-fetch", name={"enabled"}, havingValue="true")
public class WeatherDataFetchService {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(WeatherDataFetchService.class);
    private static final TypeReference<List<List<Object>>> LIST_LIST_OBJ = new /* Unavailable Anonymous Inner Class!! */;
    private static final Map<String, Integer> WARNING_SIGNAL_MAP = Map.of((Object)"title", (Object)0, (Object)"description", (Object)9, (Object)"icon", (Object)6, (Object)"datetimeString", (Object)5, (Object)"latitude", (Object)7, (Object)"longitude", (Object)8);
    private static final Map<String, Integer> WEATHER_FORECAST_MAP = Map.of((Object)"city", (Object)0, (Object)"latitude", (Object)1, (Object)"longitude", (Object)2, (Object)"maxTemperature", (Object)5, (Object)"minTemperature", (Object)7, (Object)"weather", (Object)4, (Object)"icon", (Object)10, (Object)"date", (Object)12, (Object)"datetime", (Object)13);
    @Value(value="${app.workspace:.}")
    private String workspace;
    private final WeatherFetchProperties props;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Scheduled(cron="${app.weather-fetch.cron:0 40 5 * * *}")
    public void scheduledFetch() {
        if (!this.running.compareAndSet(false, true)) {
            log.info("\u6c14\u8c61\u6293\u53d6\u4efb\u52a1\u4ecd\u5728\u8fd0\u884c\uff0c\u8df3\u8fc7\u672c\u8f6e\u8c03\u5ea6");
            return;
        }
        long startedAt = System.currentTimeMillis();
        int ok = 0;
        int failed = 0;
        try {
            for (Map.Entry e : this.props.getJsonDataMap().entrySet()) {
                if (this.fetchAndSaveJsonContourType((String)e.getKey(), (String)e.getValue())) {
                    ++ok;
                    continue;
                }
                ++failed;
            }
            for (Map.Entry e : this.props.getArrayDataMap().entrySet()) {
                if (this.fetchAndSaveArrayType((String)e.getKey(), (String)e.getValue())) {
                    ++ok;
                    continue;
                }
                ++failed;
            }
            for (Map.Entry e : this.props.getJsonArrayDataMap().entrySet()) {
                if (this.fetchAndSaveJsonArrayType((String)e.getKey(), (String)e.getValue())) {
                    ++ok;
                    continue;
                }
                ++failed;
            }
            log.info("\u6c14\u8c61\u6293\u53d6\u5b8c\u6210\uff1a\u6210\u529f {}\uff0c\u5931\u8d25 {}\uff0c\u8017\u65f6 {}ms", new Object[]{ok, failed, System.currentTimeMillis() - startedAt});
        }
        finally {
            this.running.set(false);
        }
    }

    private boolean fetchAndSaveJsonContourType(String key, String url) {
        try {
            String body = this.fetchJsonpBody(url);
            JsonNode root = this.objectMapper.readTree(body);
            JsonNode contours = root.path("contours");
            ArrayList<Map> features = new ArrayList<Map>();
            if (contours.isArray()) {
                for (JsonNode contour : contours) {
                    features.add(this.buildContourFeature(key, contour, Math.max(1, this.props.getDiluting())));
                }
            }
            this.writeGeoJsonFile(key, this.featureCollection(features));
            return true;
        }
        catch (Exception e) {
            log.warn("\u6293\u53d6 {} \u5931\u8d25: {}", (Object)key, (Object)e.getMessage());
            return false;
        }
    }

    private boolean fetchAndSaveArrayType(String key, String url) {
        try {
            String body = this.fetchJsonpBody(url);
            List rows = (List)this.objectMapper.readValue(body, LIST_LIST_OBJ);
            Map mapping = this.resolveArrayMapping(key);
            if (mapping == null) {
                throw new IllegalArgumentException("unknown data type: " + key);
            }
            ArrayList<Map> features = new ArrayList<Map>();
            for (List row : rows) {
                features.add(this.buildArrayPointFeature(key, row, mapping));
            }
            this.writeGeoJsonFile(key, this.featureCollection(features));
            return true;
        }
        catch (Exception e) {
            log.warn("\u6293\u53d6 {} \u5931\u8d25: {}", (Object)key, (Object)e.getMessage());
            return false;
        }
    }

    private boolean fetchAndSaveJsonArrayType(String key, String url) {
        try {
            String body = this.fetchJsonpBody(url);
            JsonNode root = this.objectMapper.readTree(body);
            JsonNode list = root.path("diamond8List");
            ArrayList<Map> features = new ArrayList<Map>();
            if (list.isArray()) {
                for (JsonNode node : list) {
                    features.add(this.buildSeaForecastFeature(node));
                }
            }
            this.writeGeoJsonFile(key, this.featureCollection(features));
            return true;
        }
        catch (Exception e) {
            log.warn("\u6293\u53d6 {} \u5931\u8d25: {}", (Object)key, (Object)e.getMessage());
            return false;
        }
    }

    private Map<String, Integer> resolveArrayMapping(String key) {
        if ("warning".equals(key)) {
            return WARNING_SIGNAL_MAP;
        }
        if ("inland24".equals(key) || "inland48".equals(key) || "inland72".equals(key)) {
            return WEATHER_FORECAST_MAP;
        }
        return null;
    }

    private Map<String, Object> buildContourFeature(String weatherType, JsonNode contour, int diluting) {
        String symbolText = this.describe(weatherType, contour.path("symbol").asInt(-1));
        LinkedHashMap<String, String> properties = new LinkedHashMap<String, String>();
        properties.put("symbol", symbolText);
        properties.put("fill", this.rgbStringToHex(contour.path("color").asText("")));
        properties.put("description", symbolText);
        ArrayList polygon = new ArrayList();
        ArrayList<List> ring = new ArrayList<List>();
        JsonNode latAndLong = contour.path("latAndLong");
        if (latAndLong.isArray()) {
            JsonNode first;
            int lastIndex = -1;
            for (int i = 0; i < latAndLong.size(); ++i) {
                JsonNode p = latAndLong.get(i);
                if (!p.isArray() || p.size() < 2 || lastIndex != -1 && i - lastIndex < diluting) continue;
                double lat = p.get(0).asDouble();
                double lon = p.get(1).asDouble();
                ring.add(List.of((Object)lon, (Object)lat));
                lastIndex = i;
            }
            if (latAndLong.size() > 0 && (first = latAndLong.get(0)).isArray() && first.size() >= 2) {
                ring.add(List.of((Object)first.get(1).asDouble(), (Object)first.get(0).asDouble()));
            }
        }
        polygon.add(ring);
        Map geometry = Map.of((Object)"type", (Object)"Polygon", (Object)"coordinates", polygon);
        return this.feature(properties, geometry);
    }

    private Map<String, Object> buildArrayPointFeature(String key, List<Object> row, Map<String, Integer> mapping) {
        LinkedHashMap<String, Object> properties = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, Integer> e : mapping.entrySet()) {
            String field = e.getKey();
            if ("latitude".equals(field) || "longitude".equals(field)) continue;
            Object v = this.safeIndex(row, e.getValue());
            if ("weather".equals(field) && ("inland24".equals(key) || "inland48".equals(key) || "inland72".equals(key)) && v instanceof String) {
                String s = (String)v;
                int idx = s.indexOf(45);
                v = idx >= 0 ? s.substring(idx + 1) : s;
            }
            properties.put(field, v);
        }
        double lat = this.parseDouble(this.safeIndex(row, mapping.get("latitude")));
        double lon = this.parseDouble(this.safeIndex(row, mapping.get("longitude")));
        Map geometry = Map.of((Object)"type", (Object)"Point", (Object)"coordinates", (Object)List.of((Object)lon, (Object)lat));
        return this.feature(properties, geometry);
    }

    private Map<String, Object> buildSeaForecastFeature(JsonNode node) {
        LinkedHashMap<String, String> properties = new LinkedHashMap<String, String>();
        properties.put("stationName", node.path("stationName").asText(""));
        properties.put("weatherPhenomena1", node.path("weatherPhenomena1").asText(""));
        properties.put("weatherPhenomena2", node.path("weatherPhenomena2").asText(""));
        properties.put("windDirection1", node.path("windDirection1").asText(""));
        properties.put("windDirection2", node.path("windDirection2").asText(""));
        properties.put("windSpeed1", node.path("windSpeed1").asText(""));
        properties.put("windSpeed2", node.path("windSpeed2").asText(""));
        properties.put("visibility1", node.path("visibility1").asText(""));
        properties.put("visibility2", node.path("visibility2").asText(""));
        double lat = this.parseDouble((Object)node.path("latitude").asText(""));
        double lon = this.parseDouble((Object)node.path("longitude").asText(""));
        Map geometry = Map.of((Object)"type", (Object)"Point", (Object)"coordinates", (Object)List.of((Object)lon, (Object)lat));
        return this.feature(properties, geometry);
    }

    private Map<String, Object> featureCollection(List<Map<String, Object>> features) {
        LinkedHashMap<String, Object> geo = new LinkedHashMap<String, Object>();
        geo.put("type", "FeatureCollection");
        geo.put("features", features);
        return geo;
    }

    private Map<String, Object> feature(Map<String, Object> properties, Map<String, Object> geometry) {
        LinkedHashMap<String, Object> f = new LinkedHashMap<String, Object>();
        f.put("type", "Feature");
        f.put("properties", properties);
        f.put("geometry", geometry);
        return f;
    }

    private void writeGeoJsonFile(String key, Map<String, Object> geoJson) throws IOException {
        Path base = this.resolveSaveDir();
        Files.createDirectories(base, new FileAttribute[0]);
        Path file = base.resolve(key + ".json");
        Files.writeString((Path)file, (CharSequence)this.objectMapper.writeValueAsString(geoJson), (Charset)StandardCharsets.UTF_8, (OpenOption[])new OpenOption[0]);
    }

    private Path resolveSaveDir() {
        Path save = Paths.get(this.props.getSaveDir(), new String[0]);
        if (save.isAbsolute()) {
            return save.normalize();
        }
        return Paths.get(this.workspace, new String[0]).toAbsolutePath().normalize().resolve(save).normalize();
    }

    private String fetchJsonpBody(String url) throws IOException, InterruptedException {
        HttpRequest req;
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(Math.max(1000, this.props.getConnectTimeoutMs()))).build();
        HttpResponse resp = client.send(req = HttpRequest.newBuilder((URI)URI.create(url)).timeout(Duration.ofMillis(Math.max(1000, this.props.getReadTimeoutMs()))).header("Accept", "*/*").GET().build(), HttpResponse.BodyHandlers.ofString((Charset)StandardCharsets.UTF_8));
        if (resp.statusCode() != 200) {
            throw new IOException("unexpected status code: " + resp.statusCode());
        }
        String body = (String)resp.body();
        int start = body.indexOf(40);
        int end = body.lastIndexOf(41);
        if (start < 0 || end <= start) {
            throw new IOException("invalid JSONP payload");
        }
        return body.substring(start + 1, end).trim();
    }

    private Object safeIndex(List<Object> row, Integer idx) {
        if (idx == null || idx < 0 || idx >= row.size()) {
            return null;
        }
        return row.get(idx);
    }

    private double parseDouble(Object v) {
        if (v == null) {
            return 0.0;
        }
        if (v instanceof Number) {
            Number n = (Number)v;
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(v).trim());
        }
        catch (Exception e) {
            return 0.0;
        }
    }

    private String rgbStringToHex(String rgb) {
        if (rgb == null) {
            return "#000000";
        }
        String s = rgb.trim();
        if (s.isEmpty()) {
            return "#000000";
        }
        if (s.startsWith("#")) {
            return s;
        }
        int l = s.indexOf(40);
        int r = s.lastIndexOf(41);
        if (l < 0 || r <= l) {
            return s;
        }
        String[] parts = s.substring(l + 1, r).split(",");
        if (parts.length < 3) {
            return s;
        }
        try {
            int rr = Integer.parseInt(parts[0].trim());
            int gg = Integer.parseInt(parts[1].trim());
            int bb = Integer.parseInt(parts[2].trim());
            rr = Math.max(0, Math.min(255, rr));
            gg = Math.max(0, Math.min(255, gg));
            bb = Math.max(0, Math.min(255, bb));
            return String.format("#%02x%02x%02x", rr, gg, bb);
        }
        catch (Exception ignore) {
            return s;
        }
    }

    private String describe(String weather, int symbol) {
        return switch (weather) {
            case "rainfall24", "rainfall48", "rainfall72" -> this.describeRainfall(symbol);
            case "fog" -> {
                switch (symbol) {
                    case 18: {
                        yield "\u8f7b\u96fe";
                    }
                    case 57: {
                        yield "\u5927\u96fe";
                    }
                    case 32: {
                        yield "\u6d53\u96fe";
                    }
                    case 49: {
                        yield "\u5f3a\u6d53\u96fe";
                    }
                }
                yield "\u672a\u77e5";
            }
            case "haze" -> {
                switch (symbol) {
                    case 53: {
                        yield "\u8f7b\u5ea6\u973e";
                    }
                    case 54: {
                        yield "\u4e2d\u5ea6\u973e";
                    }
                    case 55: {
                        yield "\u91cd\u5ea6\u973e";
                    }
                    case 56: {
                        yield "\u4e25\u91cd\u973e";
                    }
                }
                yield "\u672a\u77e5";
            }
            case "dust" -> {
                switch (symbol) {
                    case 30: {
                        yield "\u626c\u6c99\u6216\u6d6e\u5c18";
                    }
                    case 31: {
                        yield "\u6c99\u5c18\u66b4";
                    }
                }
                yield "\u672a\u77e5";
            }
            case "windAndTemperature" -> {
                switch (symbol) {
                    case 36: {
                        yield "\u964d\u6e2910\u2103";
                    }
                    case 37: {
                        yield "\u964d\u6e298\u2103";
                    }
                    case 38: {
                        yield "\u964d\u6e296\u2103";
                    }
                    case 39: {
                        yield "\u964d\u6e294\u2103";
                    }
                    case 40: {
                        yield "6\u7ea7\u4ee5\u4e0a\u5927\u98ce";
                    }
                }
                yield "\u672a\u77e5";
            }
            default -> "\u672a\u77e5";
        };
    }

    private String describeRainfall(int symbol) {
        if (symbol >= 0 && symbol < 10) {
            return "\u5c0f\u96e8";
        }
        if (symbol < 25) {
            return "\u4e2d\u96e8";
        }
        if (symbol < 50) {
            return "\u5927\u96e8";
        }
        if (symbol < 100) {
            return "\u66b4\u96e8";
        }
        if (symbol < 250) {
            return "\u5927\u66b4\u96e8";
        }
        return "\u7279\u5927\u66b4\u96e8";
    }

    @Generated
    public WeatherDataFetchService(WeatherFetchProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
    }
}

