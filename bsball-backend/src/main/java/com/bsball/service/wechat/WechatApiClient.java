package com.bsball.service.wechat;

import com.bsball.config.WechatProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * 微信开放接口客户端：access_token / jsapi_ticket 两个 GET（RestClient 单例，对齐 BaiduIpLocationProvider）。
 * 注意：日志不得打印本类请求 URI 与响应内容（含凭证）。
 */
@Component
public class WechatApiClient {
    private static final String TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";
    private static final String TICKET_URL = "https://api.weixin.qq.com/cgi-bin/ticket/getticket";
    private static final RestClient REST = RestClient.builder()
            .requestFactory(new JdkClientHttpRequestFactory(
                    HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()))
            .build();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final WechatProperties props;

    public WechatApiClient(WechatProperties props) {
        this.props = props;
    }

    /** 获取 access_token（errcode!=0 时 ok=false） */
    public WechatApiResult fetchAccessToken() {
        String uri = UriComponentsBuilder.fromUriString(TOKEN_URL)
                .queryParam("grant_type", "client_credential")
                .queryParam("appid", props.getAppId())
                .queryParam("secret", props.getAppSecret())
                .build().toUriString();
        String json = REST.get().uri(uri).retrieve().body(String.class);
        return parseResult(json, "access_token");
    }

    /** 获取 jsapi_ticket */
    public WechatApiResult fetchJsapiTicket(String accessToken) {
        String uri = UriComponentsBuilder.fromUriString(TICKET_URL)
                .queryParam("access_token", accessToken)
                .queryParam("type", "jsapi")
                .build().toUriString();
        String json = REST.get().uri(uri).retrieve().body(String.class);
        return parseResult(json, "ticket");
    }

    /** 解析微信响应（包内可见，便于单测，不触网） */
    static WechatApiResult parseResult(String json, String valueField) {
        try {
            JsonNode node = MAPPER.readTree(json);
            int errcode = node.path("errcode").asInt(0);
            String errmsg = node.path("errmsg").asText("");
            String value = node.path(valueField).asText("");
            int expiresIn = node.path("expires_in").asInt(0);
            return new WechatApiResult(errcode == 0 && !value.isEmpty(), errcode, errmsg, value, expiresIn);
        } catch (Exception e) {
            return new WechatApiResult(false, -1, "parse error", "", 0);
        }
    }

    /** 微信接口结果：ok=errcode==0 且有值；errcode 供调用方自愈判断（40001/42001 失效） */
    public record WechatApiResult(boolean ok, int errcode, String errmsg, String value, int expiresIn) {
    }
}
