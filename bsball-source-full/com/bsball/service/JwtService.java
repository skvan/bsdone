/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.config.JwtProperties
 *  com.bsball.config.TenantProperties
 *  com.bsball.service.JwtService
 *  com.bsball.service.JwtService$TokenAuth
 *  io.jsonwebtoken.Claims
 *  io.jsonwebtoken.ExpiredJwtException
 *  io.jsonwebtoken.JwtBuilder
 *  io.jsonwebtoken.JwtException
 *  io.jsonwebtoken.Jwts
 *  io.jsonwebtoken.MalformedJwtException
 *  io.jsonwebtoken.UnsupportedJwtException
 *  io.jsonwebtoken.lang.NestedCollection
 *  io.jsonwebtoken.security.Keys
 *  io.jsonwebtoken.security.SignatureException
 *  org.springframework.stereotype.Component
 */
package com.bsball.service;

import com.bsball.config.JwtProperties;
import com.bsball.config.TenantProperties;
import com.bsball.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.lang.NestedCollection;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/*
 * Exception performing whole class analysis ignored.
 */
@Component
public class JwtService {
    private static final String CLAIM_TID = "tid";
    private final JwtProperties props;
    private final TenantProperties tenantProps;
    private final SecretKey key;

    public JwtService(JwtProperties props, TenantProperties tenantProps) {
        this.props = props;
        this.tenantProps = tenantProps;
        String secret = props.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("app.jwt.secret \u672a\u914d\u7f6e\uff1a\u8bf7\u8bbe\u7f6e\u73af\u5883\u53d8\u91cf JWT_SECRET \u6216\u5728 application.yml \u4e2d\u914d\u7f6e app.jwt.secret\uff08HS256 \u9700\u81f3\u5c11 32 \u5b57\u8282\uff09");
        }
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        if (raw.length < 32) {
            throw new IllegalStateException("app.jwt.secret \u8fc7\u77ed\uff1aHS256 \u81f3\u5c11\u9700\u8981 32 \u5b57\u8282\uff08UTF-8 \u4e0b\u5efa\u8bae\u81f3\u5c11 32 \u4e2a ASCII \u5b57\u7b26\uff09");
        }
        if (props.getExpiration() == null || props.getExpiration().isNegative() || props.getExpiration().isZero()) {
            throw new IllegalStateException("app.jwt.expiration \u987b\u4e3a\u6b63\u7684\u6709\u6548\u65f6\u957f\uff0c\u5982 7d\u300112h\u300190m");
        }
        this.key = Keys.hmacShaKeyFor((byte[])raw);
    }

    public String createToken(Long userId) {
        return this.createToken(userId, Long.valueOf(this.tenantProps.getDefaultId()));
    }

    public String createToken(Long userId, Long tenantId) {
        if (tenantId == null) {
            tenantId = this.tenantProps.getDefaultId();
        }
        Date now = new Date();
        JwtBuilder b = Jwts.builder().subject(String.valueOf(userId)).claim("tid", (Object)tenantId).issuedAt(now).expiration(new Date(now.getTime() + this.props.getExpiration().toMillis())).signWith((Key)this.key);
        if (this.props.getIssuer() != null && !this.props.getIssuer().isBlank()) {
            b = b.issuer(this.props.getIssuer().trim());
        }
        if (this.props.getAudience() != null && !this.props.getAudience().isBlank()) {
            b = (JwtBuilder)((NestedCollection)b.audience().add((Object)this.props.getAudience().trim())).and();
        }
        return b.compact();
    }

    public TokenAuth authenticateBearerToken(String token) {
        if (token == null || token.isBlank()) {
            return new TokenAuth(null, null, null, null);
        }
        try {
            String s;
            Claims claims = (Claims)Jwts.parser().verifyWith(this.key).build().parseSignedClaims((CharSequence)token.trim()).getPayload();
            Long tid = null;
            Object v = claims.get((Object)"tid");
            if (v instanceof Number) {
                Number n = (Number)v;
                tid = n.longValue();
            } else if (v instanceof String && !(s = (String)v).isBlank()) {
                tid = Long.parseLong(s.trim());
            }
            return new TokenAuth(Long.valueOf(Long.parseLong(claims.getSubject())), tid, null, null);
        }
        catch (ExpiredJwtException e) {
            return new TokenAuth(null, null, "\u767b\u5f55\u5df2\u8fc7\u671f\uff0c\u8bf7\u91cd\u65b0\u767b\u5f55", "\u4ee4\u724c\u5df2\u8fc7\u671f");
        }
        catch (JwtException e) {
            return new TokenAuth(null, null, "\u767b\u5f55\u4fe1\u606f\u65e0\u6548\uff0c\u8bf7\u91cd\u65b0\u767b\u5f55", JwtService.jwtInvalidDetail((JwtException)e));
        }
        catch (IllegalArgumentException e) {
            return new TokenAuth(null, null, "\u767b\u5f55\u4fe1\u606f\u65e0\u6548\uff0c\u8bf7\u91cd\u65b0\u767b\u5f55", "\u4ee4\u724c\u683c\u5f0f\u5f02\u5e38");
        }
    }

    private static String jwtInvalidDetail(JwtException e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SignatureException) {
                return "\u65e0\u6548\u7b7e\u540d";
            }
            if (t instanceof MalformedJwtException) {
                return "\u4ee4\u724c\u683c\u5f0f\u9519\u8bef";
            }
            if (!(t instanceof UnsupportedJwtException)) continue;
            return "\u4e0d\u652f\u6301\u7684\u4ee4\u724c";
        }
        return "\u4ee4\u724c\u6821\u9a8c\u5931\u8d25";
    }

    public Long parseUserId(String token) {
        return this.authenticateBearerToken(token).userId();
    }
}

