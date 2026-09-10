/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.config.SmsProperties
 *  com.bsball.service.sms.AliyunSmsClient
 *  com.fasterxml.jackson.databind.JsonNode
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  java.net.http.HttpClient
 *  java.net.http.HttpRequest
 *  java.net.http.HttpResponse
 *  java.net.http.HttpResponse$BodyHandlers
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Component
 */
package com.bsball.service.sms;

import com.bsball.config.SmsProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/*
 * Exception performing whole class analysis ignored.
 */
@Component
public class AliyunSmsClient {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(AliyunSmsClient.class);
    private static final String ENDPOINT = "https://dysmsapi.aliyuncs.com/";
    private static final DateTimeFormatter ISO8601 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);
    private final SmsProperties props;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10L)).build();

    public boolean send(String phone, String templateCode, Map<String, String> templateParams) {
        if (this.props.getAccessKeyId() == null || this.props.getAccessKeyId().isBlank() || this.props.getAccessKeySecret() == null || this.props.getAccessKeySecret().isBlank()) {
            return false;
        }
        try {
            TreeMap<String, String> params = new TreeMap<String, String>();
            params.put("AccessKeyId", this.props.getAccessKeyId());
            params.put("Action", "SendSms");
            params.put("Format", "JSON");
            params.put("PhoneNumbers", phone);
            params.put("RegionId", "cn-hangzhou");
            params.put("SignName", this.props.getSignName());
            params.put("SignatureMethod", "HMAC-SHA1");
            params.put("SignatureNonce", UUID.randomUUID().toString());
            params.put("SignatureVersion", "1.0");
            params.put("TemplateCode", templateCode);
            params.put("TemplateParam", this.objectMapper.writeValueAsString(templateParams));
            params.put("Timestamp", ISO8601.format(Instant.now()));
            params.put("Version", "2017-05-25");
            String canonical = params.entrySet().stream().map(e -> AliyunSmsClient.percentEncode((String)((String)e.getKey())) + "=" + AliyunSmsClient.percentEncode((String)((String)e.getValue()))).collect(Collectors.joining("&"));
            String stringToSign = "GET&" + AliyunSmsClient.percentEncode((String)"/") + "&" + AliyunSmsClient.percentEncode((String)canonical);
            String signature = AliyunSmsClient.sign((String)stringToSign, (String)(this.props.getAccessKeySecret() + "&"));
            params.put("Signature", signature);
            String query = params.entrySet().stream().map(e -> (String)e.getKey() + "=" + URLEncoder.encode((String)((String)e.getValue()), (Charset)StandardCharsets.UTF_8)).collect(Collectors.joining("&"));
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create("https://dysmsapi.aliyuncs.com/?" + query)).timeout(Duration.ofSeconds(15L)).GET().build();
            HttpResponse resp = this.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode root = this.objectMapper.readTree((String)resp.body());
            String code = root.path("Code").asText("");
            if ("OK".equalsIgnoreCase(code)) {
                return true;
            }
            log.warn("Aliyun SMS failed: {} {}", (Object)code, (Object)root.path("Message").asText(""));
            return false;
        }
        catch (Exception e2) {
            log.error("Aliyun SMS error", (Throwable)e2);
            return false;
        }
    }

    private static String sign(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    private static String percentEncode(String value) {
        return URLEncoder.encode((String)value, (Charset)StandardCharsets.UTF_8).replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
    }

    @Generated
    public AliyunSmsClient(SmsProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
    }
}

