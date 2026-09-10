/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.exception.UnauthorizedException
 *  com.bsball.service.AuthCaptchaService
 *  com.bsball.service.AuthCaptchaService$CaptchaEntry
 *  com.bsball.service.AuthCaptchaService$CaptchaTenantConfig
 *  com.bsball.service.AuthCaptchaService$ClickChallenge
 *  com.bsball.service.AuthCaptchaService$ClickFigure
 *  com.bsball.service.AuthCaptchaService$ClickTargetMeta
 *  com.bsball.service.AuthCaptchaService$DragPuzzle
 *  com.bsball.service.AuthCaptchaService$GifSequenceWriter
 *  com.bsball.service.AuthCaptchaService$PassEntry
 *  com.bsball.service.SysConfigService
 *  lombok.Generated
 *  org.apache.batik.transcoder.TranscoderInput
 *  org.apache.batik.transcoder.TranscoderOutput
 *  org.apache.batik.transcoder.image.ImageTranscoder
 *  org.apache.batik.transcoder.image.PNGTranscoder
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.core.env.Environment
 *  org.springframework.stereotype.Service
 */
package com.bsball.service;

import com.bsball.exception.UnauthorizedException;
import com.bsball.service.AuthCaptchaService;
import com.bsball.service.SysConfigService;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.Generated;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/*
 * Exception performing whole class analysis ignored.
 */
@Service
public class AuthCaptchaService {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(AuthCaptchaService.class);
    public static final String KEY_ENABLED = "authCaptchaEnabled";
    public static final String KEY_TYPE = "authCaptchaType";
    public static final String KEY_RANDOM_TYPES = "authCaptchaRandomTypes";
    public static final String TYPE_INPUT = "input";
    public static final String TYPE_CLICK = "click";
    public static final String TYPE_RANDOM = "random";
    public static final String TYPE_DRAG = "drag";
    public static final int INPUT_CAPTCHA_CODE_LEN = 5;
    private static final long EXPIRE_MILLIS = 120000L;
    private static final String CODE_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String[] CLICK_COLOR_KEYS = new String[]{"red", "blue", "green"};
    private static final String[] CLICK_COLOR_HEX = new String[]{"#e03a38", "#2f6fff", "#1f9f57"};
    private static final String[] CLICK_SHAPE_KEYS = new String[]{"circle", "square", "triangle"};
    private static final String[] CLICK_SIZE_KEYS = new String[]{"small", "large"};
    private static final int CLICK_CANVAS_WIDTH = 280;
    private static final int CLICK_CANVAS_HEIGHT = 96;
    private static final int CLICK_FIGURE_COUNT = 7;
    private static final int CLICK_SEQUENCE_LEN = 3;
    private static final String CLICK_SHAPE_FILL_OPACITY = "0.9";
    private static final int CAPTCHA_DEBUG_PNG_SCALE = 2;
    private static final int DRAG_TRACK_W = 280;
    private static final int DRAG_TRACK_H = 120;
    private static final int DRAG_PIECE_W = 36;
    private static final int DRAG_PIECE_H = 36;
    private static final int DRAG_HOLE_MARGIN_Y = 6;
    private static final int DRAG_DECOY_MIN_DELTA_Y = 14;
    private static final double DRAG_TOLERANCE = 2.0;
    private static final long DRAG_BEHAVIOR_MIN_ELAPSED_MS = 120L;
    private static final int DRAG_BEHAVIOR_MIN_TRACK_POINTS = 3;
    private static final int DRAG_BEHAVIOR_MIN_MOVE_COUNT = 2;
    private static final int IMAGE_CAPTCHA_ROT_MIN = -22;
    private static final int IMAGE_CAPTCHA_ROT_MAX = 22;
    private static final int IMAGE_CAPTCHA_SKEW_MIN = -9;
    private static final int IMAGE_CAPTCHA_SKEW_MAX = 9;
    private static final double IMAGE_CAPTCHA_CHAR_WIDTH_FACTOR = 0.66;
    private static final double IMAGE_CAPTCHA_MIN_ADVANCE_RATIO = 0.72;
    private static final int INPUT_CAPTCHA_GIF_FRAMES = 12;
    private static final int INPUT_CAPTCHA_GIF_DELAY_CS = 14;
    private static final int INPUT_CAPTCHA_GIF_HIDE_COUNT = 2;
    private static final int INPUT_CAPTCHA_GIF_HIDE_HOLD_FRAMES = 3;
    private final SysConfigService sysConfigService;
    private final Environment environment;
    @Value(value="${app.auth.captcha.enabled:true}")
    private boolean defaultEnabled;
    @Value(value="${app.auth.captcha.type:random}")
    private String defaultType;
    @Value(value="${app.auth.captcha.random-types:input,click,drag}")
    private String defaultRandomTypes;
    @Value(value="${app.auth.captcha.debug-save-on-fail:false}")
    private boolean debugSaveOnFail;
    @Value(value="${app.auth.captcha.debug-save-on-success:false}")
    private boolean debugSaveOnSuccess;
    @Value(value="${app.auth.captcha.debug-save-dir:logs/captcha-debug}")
    private String debugSaveDir;
    @Value(value="${app.workspace:.}")
    private String workspaceDir;
    private final ConcurrentHashMap<String, CaptchaEntry> store = new ConcurrentHashMap();
    private final ConcurrentHashMap<String, PassEntry> passStore = new ConcurrentHashMap();
    private static final int PNG_MAGIC_LEN = 8;
    private static final Pattern SVG_ATTR_INT = Pattern.compile("\\b(width|height)=['\"](\\d+)['\"]");
    private static final int IMAGE_CAPTCHA_DEBUG_USER_LINE_EXTRA = 16;
    private static final int IMAGE_CAPTCHA_SVG_W = 160;
    private static final int IMAGE_CAPTCHA_SVG_H = 54;

    private CaptchaTenantConfig readCaptchaTenantConfig(long tenantId) {
        Map cfg = this.sysConfigService.getConfig(tenantId);
        boolean enabled = AuthCaptchaService.asBoolean(cfg.get("authCaptchaEnabled"), (boolean)this.defaultEnabled);
        String configType = AuthCaptchaService.normalizeType((Object)cfg.getOrDefault("authCaptchaType", this.defaultType));
        List randomTypes = AuthCaptchaService.normalizeRandomTypes(cfg.get("authCaptchaRandomTypes"), (List)AuthCaptchaService.splitRandomTypesCsv((String)this.defaultRandomTypes));
        return new CaptchaTenantConfig(enabled, configType, randomTypes);
    }

    public Map<String, Object> getOptions(long tenantId) {
        CaptchaTenantConfig c = this.readCaptchaTenantConfig(tenantId);
        LinkedHashMap<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("enabled", c.enabled());
        out.put("type", c.configType());
        out.put("randomTypes", c.randomTypes());
        return out;
    }

    public Map<String, Object> createImageCaptcha(long tenantId) {
        Object imageData;
        CaptchaEntry entry;
        CaptchaTenantConfig c = this.readCaptchaTenantConfig(tenantId);
        if (!c.enabled()) {
            LinkedHashMap<String, Object> out = new LinkedHashMap<String, Object>();
            out.put("enabled", false);
            out.put("type", c.configType());
            return out;
        }
        String configType = c.configType();
        String effectiveType = AuthCaptchaService.resolveEffectiveCaptchaType((String)configType, (List)c.randomTypes());
        this.clearExpired();
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        long expireAt = System.currentTimeMillis() + 120000L;
        String svg = null;
        ClickChallenge clickChallenge = null;
        String pieceSvgForDrag = null;
        if ("click".equals(effectiveType)) {
            clickChallenge = AuthCaptchaService.randomClickChallenge();
            svg = AuthCaptchaService.buildClickSvg((List)clickChallenge.figures());
            entry = new CaptchaEntry("click", null, clickChallenge.sequence(), null, null, expireAt, svg, clickChallenge.promptLabel());
            imageData = "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
        } else if ("drag".equals(effectiveType)) {
            DragPuzzle dp = AuthCaptchaService.randomDragPuzzle();
            svg = dp.bgSvg();
            pieceSvgForDrag = dp.pieceSvg();
            entry = new CaptchaEntry("drag", null, null, Integer.valueOf(dp.targetX()), Integer.valueOf(dp.holeY()), expireAt, svg, "\u62d6\u62fd\u62fc\u56fe");
            imageData = "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
        } else {
            String code = AuthCaptchaService.randomCode((int)5);
            entry = new CaptchaEntry("input", code, null, null, null, expireAt, null, "\u5b57\u7b26\u9a8c\u8bc1\u7801");
            imageData = AuthCaptchaService.buildImageCaptchaGifDataUri((String)code);
        }
        this.store.put(AuthCaptchaService.storeKey((long)tenantId, (String)captchaId), entry);
        LinkedHashMap<String, Object> opts = new LinkedHashMap<String, Object>();
        opts.put("enabled", true);
        opts.put("type", configType);
        opts.put("randomTypes", c.randomTypes());
        LinkedHashMap<String, Object> out = new LinkedHashMap<String, Object>(opts);
        out.put("type", effectiveType);
        out.put("captchaId", captchaId);
        out.put("imageData", imageData);
        out.put("expireAt", Instant.ofEpochMilli(expireAt).toString());
        if (clickChallenge != null) {
            ArrayList targets = new ArrayList(3);
            for (ClickTargetMeta m : clickChallenge.targetMetas()) {
                LinkedHashMap<String, String> one = new LinkedHashMap<String, String>();
                one.put("color", m.colorKey());
                one.put("size", m.sizeKey());
                one.put("shape", m.shapeKey());
                if (m.qualifierKey() != null && !m.qualifierKey().isBlank()) {
                    one.put("qualifier", m.qualifierKey());
                }
                targets.add(one);
            }
            out.put("clickTargets", targets);
        }
        if (pieceSvgForDrag != null) {
            out.put("dragPieceImage", "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(pieceSvgForDrag.getBytes(StandardCharsets.UTF_8)));
            out.put("dragTrackWidth", 280);
            out.put("dragTrackHeight", 120);
            out.put("dragPieceWidth", 36);
            out.put("dragPieceHeight", 36);
            out.put("dragPieceY", entry.dragHoleY());
        }
        return out;
    }

    public Map<String, Object> createPortalFeedbackCaptcha(long tenantId) {
        CaptchaTenantConfig c = this.readCaptchaTenantConfig(tenantId);
        LinkedHashMap<String, Object> meta = new LinkedHashMap<String, Object>();
        meta.put("enabled", c.enabled());
        meta.put("type", "drag");
        if (!c.enabled()) {
            return meta;
        }
        this.clearExpired();
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        long expireAt = System.currentTimeMillis() + 120000L;
        DragPuzzle dp = AuthCaptchaService.randomDragPuzzle();
        String svg = dp.bgSvg();
        String pieceSvgForDrag = dp.pieceSvg();
        CaptchaEntry entry = new CaptchaEntry("drag", null, null, Integer.valueOf(dp.targetX()), Integer.valueOf(dp.holeY()), expireAt, svg, "\u95e8\u6237\u53cd\u9988\u62d6\u62fd\u9a8c\u8bc1\u7801");
        this.store.put(AuthCaptchaService.storeKey((long)tenantId, (String)captchaId), entry);
        String imageData = "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
        LinkedHashMap<String, Object> out = new LinkedHashMap<String, Object>(meta);
        out.put("captchaId", captchaId);
        out.put("imageData", imageData);
        out.put("expireAt", Instant.ofEpochMilli(expireAt).toString());
        out.put("dragPieceImage", "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(pieceSvgForDrag.getBytes(StandardCharsets.UTF_8)));
        out.put("dragTrackWidth", 280);
        out.put("dragTrackHeight", 120);
        out.put("dragPieceWidth", 36);
        out.put("dragPieceHeight", 36);
        out.put("dragPieceY", entry.dragHoleY());
        return out;
    }

    public void validateAndConsumePortalFeedbackCaptcha(long tenantId, Map<String, Object> body) {
        String verifyToken;
        String captchaId;
        CaptchaTenantConfig c = this.readCaptchaTenantConfig(tenantId);
        if (!c.enabled()) {
            return;
        }
        String string = captchaId = body != null && body.get("captchaId") != null ? body.get("captchaId").toString().trim() : "";
        if (captchaId.isEmpty()) {
            throw new UnauthorizedException("\u8bf7\u5b8c\u6210\u62d6\u62fd\u9a8c\u8bc1\u7801");
        }
        String string2 = verifyToken = body != null && body.get("captchaVerifyToken") != null ? body.get("captchaVerifyToken").toString().trim() : "";
        if (verifyToken.isEmpty()) {
            throw new UnauthorizedException("\u8bf7\u5148\u5b8c\u6210\u62d6\u62fd\u9a8c\u8bc1\u7801");
        }
        PassEntry pe = (PassEntry)this.passStore.get(AuthCaptchaService.passKey((long)tenantId, (String)verifyToken));
        if (pe == null || pe.expiresAt < System.currentTimeMillis() || !captchaId.equals(pe.captchaId)) {
            this.debugSaveOnCaptchaFail(null, "\u95e8\u6237\u53cd\u9988\u62d6\u62fd\u9a8c\u8bc1\u7801\u672a\u901a\u8fc7\u6216\u5df2\u8fc7\u671f");
            throw new UnauthorizedException("\u62d6\u62fd\u9a8c\u8bc1\u7801\u672a\u901a\u8fc7\u6216\u5df2\u8fc7\u671f");
        }
        this.passStore.remove(AuthCaptchaService.passKey((long)tenantId, (String)verifyToken));
        this.debugSaveOnCaptchaSuccess(null, "\u95e8\u6237\u53cd\u9988\u62d6\u62fd\u9a8c\u8bc1\u7801\u901a\u8fc7", null);
    }

    public void validateLoginCaptchaIfNeeded(long tenantId, Map<String, Object> body) {
        String captchaId;
        CaptchaTenantConfig c = this.readCaptchaTenantConfig(tenantId);
        if (!c.enabled()) {
            return;
        }
        String configType = c.configType();
        String string = captchaId = body != null && body.get("captchaId") != null ? body.get("captchaId").toString().trim() : "";
        if (captchaId.isEmpty()) {
            throw new UnauthorizedException("\u8bf7\u8f93\u5165\u9a8c\u8bc1\u7801");
        }
        String key = AuthCaptchaService.storeKey((long)tenantId, (String)captchaId);
        if ("click".equals(configType)) {
            this.validateLoginCaptchaClickPass(tenantId, body, captchaId);
            return;
        }
        if ("drag".equals(configType)) {
            this.validateLoginCaptchaClickPass(tenantId, body, captchaId);
            return;
        }
        if ("random".equals(configType)) {
            String verifyToken;
            String string2 = verifyToken = body != null && body.get("captchaVerifyToken") != null ? body.get("captchaVerifyToken").toString().trim() : "";
            if (!verifyToken.isEmpty()) {
                this.validateLoginCaptchaClickPass(tenantId, body, captchaId);
                return;
            }
            this.validateLoginCaptchaInput(body, key);
            return;
        }
        this.validateLoginCaptchaInput(body, key);
    }

    private void validateLoginCaptchaClickPass(long tenantId, Map<String, Object> body, String captchaId) {
        String verifyToken;
        String string = verifyToken = body != null && body.get("captchaVerifyToken") != null ? body.get("captchaVerifyToken").toString().trim() : "";
        if (verifyToken.isEmpty()) {
            this.debugSaveOnCaptchaFail(null, "\u56fe\u7247\u9a8c\u8bc1\u7801\u7f3a\u5c11\u6821\u9a8c\u4ee4\u724c");
            throw new UnauthorizedException("\u8bf7\u5148\u5b8c\u6210\u56fe\u7247\u9a8c\u8bc1\u7801\u6821\u9a8c");
        }
        PassEntry pe = (PassEntry)this.passStore.get(AuthCaptchaService.passKey((long)tenantId, (String)verifyToken));
        if (pe == null || pe.expiresAt < System.currentTimeMillis() || !captchaId.equals(pe.captchaId)) {
            this.debugSaveOnCaptchaFail(null, "\u56fe\u7247\u9a8c\u8bc1\u7801\u672a\u901a\u8fc7\u6216\u5df2\u8fc7\u671f");
            throw new UnauthorizedException("\u56fe\u7247\u9a8c\u8bc1\u7801\u6821\u9a8c\u672a\u901a\u8fc7\u6216\u5df2\u8fc7\u671f");
        }
    }

    private void validateLoginCaptchaInput(Map<String, Object> body, String key) {
        String captchaCode;
        CaptchaEntry e = (CaptchaEntry)this.store.get(key);
        if (e == null || e.expiresAt < System.currentTimeMillis()) {
            throw new UnauthorizedException("\u9a8c\u8bc1\u7801\u5df2\u8fc7\u671f\uff0c\u8bf7\u5237\u65b0\u540e\u91cd\u8bd5");
        }
        String string = captchaCode = body != null && body.get("captchaCode") != null ? body.get("captchaCode").toString().trim() : "";
        if (captchaCode.isEmpty()) {
            throw new UnauthorizedException("\u8bf7\u8f93\u5165\u9a8c\u8bc1\u7801");
        }
        if (e.code == null || !e.code.equalsIgnoreCase(captchaCode)) {
            this.debugSaveOnCaptchaFail(e, "\u56fe\u5f62\u9a8c\u8bc1\u7801\u9519\u8bef", null, captchaCode);
            throw new UnauthorizedException("\u9a8c\u8bc1\u7801\u9519\u8bef");
        }
    }

    public void consumeCaptchaAfterLoginSuccess(long tenantId, Map<String, Object> body) {
        String captchaId;
        CaptchaTenantConfig c = this.readCaptchaTenantConfig(tenantId);
        if (!c.enabled()) {
            return;
        }
        String configType = c.configType();
        if ("click".equals(configType)) {
            String verifyToken;
            String string = verifyToken = body != null && body.get("captchaVerifyToken") != null ? body.get("captchaVerifyToken").toString().trim() : "";
            if (!verifyToken.isEmpty()) {
                this.passStore.remove(AuthCaptchaService.passKey((long)tenantId, (String)verifyToken));
            }
            return;
        }
        if ("drag".equals(configType)) {
            String verifyToken;
            String string = verifyToken = body != null && body.get("captchaVerifyToken") != null ? body.get("captchaVerifyToken").toString().trim() : "";
            if (!verifyToken.isEmpty()) {
                this.passStore.remove(AuthCaptchaService.passKey((long)tenantId, (String)verifyToken));
            }
            return;
        }
        if ("random".equals(configType)) {
            String verifyToken;
            String string = verifyToken = body != null && body.get("captchaVerifyToken") != null ? body.get("captchaVerifyToken").toString().trim() : "";
            if (!verifyToken.isEmpty()) {
                this.passStore.remove(AuthCaptchaService.passKey((long)tenantId, (String)verifyToken));
                return;
            }
        }
        String string = captchaId = body != null && body.get("captchaId") != null ? body.get("captchaId").toString().trim() : "";
        if (captchaId.isEmpty()) {
            return;
        }
        CaptchaEntry e = (CaptchaEntry)this.store.remove(AuthCaptchaService.storeKey((long)tenantId, (String)captchaId));
        if (e != null) {
            String userCode = body != null && body.get("captchaCode") != null ? body.get("captchaCode").toString().trim() : "";
            this.debugSaveOnCaptchaSuccess(e, "\u767b\u5f55\u56fe\u5f62\u9a8c\u8bc1\u7801\u901a\u8fc7", null, userCode);
        }
    }

    public Map<String, Object> verifyClickCaptcha(long tenantId, Map<String, Object> body) {
        String captchaId = body != null && body.get("captchaId") != null ? body.get("captchaId").toString().trim() : "";
        List userClicks = AuthCaptchaService.parseOrderedClickPoints(body);
        if (captchaId.isEmpty() || userClicks == null) {
            throw new UnauthorizedException("\u8bf7\u6309\u987a\u5e8f\u70b9\u51fb\u9a8c\u8bc1\u7801\u56fe\u7247");
        }
        String key = AuthCaptchaService.storeKey((long)tenantId, (String)captchaId);
        CaptchaEntry e = (CaptchaEntry)this.store.remove(key);
        if (e == null || e.expiresAt < System.currentTimeMillis()) {
            throw new UnauthorizedException("\u9a8c\u8bc1\u7801\u5df2\u8fc7\u671f\uff0c\u8bf7\u5237\u65b0\u540e\u91cd\u8bd5");
        }
        if (!"click".equals(e.type)) {
            throw new UnauthorizedException("\u5f53\u524d\u4e0d\u662f\u70b9\u9009\u9a8c\u8bc1\u7801");
        }
        List expected = e.clickSequence();
        if (expected == null || expected.isEmpty()) {
            throw new UnauthorizedException("\u70b9\u9009\u9a8c\u8bc1\u7801\u6570\u636e\u5f02\u5e38");
        }
        if (userClicks.size() != expected.size()) {
            this.debugSaveOnCaptchaFail(e, "\u70b9\u9009\u6b21\u6570\u4e0e\u8981\u6c42\u4e0d\u7b26", userClicks);
            throw new UnauthorizedException("\u8bf7\u6309\u63d0\u793a\u987a\u5e8f\u5b8c\u6210\u70b9\u9009");
        }
        for (int i = 0; i < expected.size(); ++i) {
            int[] region = (int[])expected.get(i);
            int[] p = (int[])userClicks.get(i);
            double dist = Math.hypot(p[0] - region[0], p[1] - region[1]);
            if (!(dist > (double)region[2])) continue;
            this.debugSaveOnCaptchaFail(e, "\u70b9\u9009\u9a8c\u8bc1\u7801\u987a\u5e8f\u6216\u4f4d\u7f6e\u9519\u8bef", userClicks);
            throw new UnauthorizedException("\u70b9\u51fb\u987a\u5e8f\u6216\u4f4d\u7f6e\u4e0d\u6b63\u786e\uff0c\u8bf7\u91cd\u8bd5");
        }
        this.debugSaveOnCaptchaSuccess(e, "\u70b9\u9009\u9a8c\u8bc1\u7801\u70b9\u51fb\u6b63\u786e", userClicks);
        String token = UUID.randomUUID().toString().replace("-", "");
        this.passStore.put(AuthCaptchaService.passKey((long)tenantId, (String)token), new PassEntry(captchaId, System.currentTimeMillis() + 120000L));
        LinkedHashMap<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("ok", true);
        out.put("verifyToken", token);
        return out;
    }

    public Map<String, Object> verifyDragCaptcha(long tenantId, Map<String, Object> body) {
        double slideX;
        String captchaId;
        block10: {
            String string = captchaId = body != null && body.get("captchaId") != null ? body.get("captchaId").toString().trim() : "";
            if (captchaId.isEmpty()) {
                throw new UnauthorizedException("\u8bf7\u5b8c\u6210\u62d6\u62fd\u9a8c\u8bc1\u7801");
            }
            try {
                Object sx;
                Object object = sx = body != null ? body.get("slideX") : null;
                if (sx instanceof Number) {
                    Number n = (Number)sx;
                    slideX = n.doubleValue();
                    break block10;
                }
                if (sx != null) {
                    slideX = Double.parseDouble(sx.toString().trim());
                    break block10;
                }
                throw new UnauthorizedException("\u8bf7\u5b8c\u6210\u62d6\u62fd\u9a8c\u8bc1\u7801");
            }
            catch (NumberFormatException e) {
                throw new UnauthorizedException("\u62d6\u62fd\u4f4d\u7f6e\u65e0\u6548");
            }
        }
        String key = AuthCaptchaService.storeKey((long)tenantId, (String)captchaId);
        CaptchaEntry e = (CaptchaEntry)this.store.remove(key);
        if (e == null || e.expiresAt < System.currentTimeMillis()) {
            throw new UnauthorizedException("\u9a8c\u8bc1\u7801\u5df2\u8fc7\u671f\uff0c\u8bf7\u5237\u65b0\u540e\u91cd\u8bd5");
        }
        if (!"drag".equals(e.type)) {
            throw new UnauthorizedException("\u5f53\u524d\u4e0d\u662f\u62d6\u62fd\u9a8c\u8bc1\u7801");
        }
        Integer tx = e.dragTargetX();
        if (tx == null) {
            throw new UnauthorizedException("\u62d6\u62fd\u9a8c\u8bc1\u7801\u6570\u636e\u5f02\u5e38");
        }
        if (Math.abs(slideX - (double)tx.intValue()) > 2.0) {
            this.debugSaveOnCaptchaFail(e, "\u62d6\u62fd\u4f4d\u7f6e\u504f\u5dee\u8fc7\u5927");
            throw new UnauthorizedException("\u62d6\u62fd\u4f4d\u7f6e\u4e0d\u6b63\u786e\uff0c\u8bf7\u91cd\u8bd5");
        }
        String behaviorReject = AuthCaptchaService.evaluateDragBehavior(body, (double)slideX, (int)tx);
        if (behaviorReject != null) {
            this.debugSaveOnCaptchaFail(e, behaviorReject);
            throw new UnauthorizedException("\u62d6\u62fd\u884c\u4e3a\u5f02\u5e38\uff0c\u8bf7\u91cd\u8bd5");
        }
        this.debugSaveOnCaptchaSuccess(e, "\u62d6\u62fd\u9a8c\u8bc1\u7801\u901a\u8fc7", null);
        String token = UUID.randomUUID().toString().replace("-", "");
        this.passStore.put(AuthCaptchaService.passKey((long)tenantId, (String)token), new PassEntry(captchaId, System.currentTimeMillis() + 120000L));
        LinkedHashMap<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("ok", true);
        out.put("verifyToken", token);
        return out;
    }

    private static String normalizeType(Object v) {
        String s;
        String string = s = v == null ? "" : v.toString().trim().toLowerCase();
        if ("click".equals(s)) {
            return "click";
        }
        if ("drag".equals(s)) {
            return "drag";
        }
        if ("random".equals(s)) {
            return "random";
        }
        if ("input".equals(s)) {
            return "input";
        }
        return "input";
    }

    private static String resolveEffectiveCaptchaType(String configType, List<String> randomTypes) {
        if ("random".equals(configType)) {
            List pool = AuthCaptchaService.normalizeRandomTypes(randomTypes, (List)List.of((Object)"input", (Object)"click", (Object)"drag"));
            return (String)pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        }
        return configType;
    }

    private static List<String> splitRandomTypesCsv(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of((Object)"input", (Object)"click", (Object)"drag");
        }
        String[] parts = raw.split(",");
        ArrayList<String> out = new ArrayList<String>(parts.length);
        for (String p : parts) {
            String s = AuthCaptchaService.normalizeType((Object)p);
            if ("random".equals(s) || out.contains(s)) continue;
            out.add(s);
        }
        return out.isEmpty() ? List.of((Object)"input", (Object)"click", (Object)"drag") : List.copyOf(out);
    }

    private static List<String> normalizeRandomTypes(Object raw, List<String> fallback) {
        String sRaw;
        ArrayList<String> out = new ArrayList<String>();
        if (raw instanceof List) {
            List list = (List)raw;
            for (Object item : list) {
                String s = AuthCaptchaService.normalizeType(item);
                if ("random".equals(s) || out.contains(s)) continue;
                out.add(s);
            }
        } else if (raw instanceof String && !(sRaw = (String)raw).isBlank()) {
            out.addAll(AuthCaptchaService.splitRandomTypesCsv((String)sRaw));
        }
        if (out.isEmpty()) {
            out.addAll(fallback);
        }
        if (out.isEmpty()) {
            out.add("input");
            out.add("click");
            out.add("drag");
        }
        return List.copyOf(out);
    }

    private static boolean asBoolean(Object v, boolean defVal) {
        if (v instanceof Boolean) {
            Boolean b = (Boolean)v;
            return b;
        }
        if (v == null) {
            return defVal;
        }
        String s = v.toString().trim();
        if ("true".equalsIgnoreCase(s) || "1".equals(s)) {
            return true;
        }
        if ("false".equalsIgnoreCase(s) || "0".equals(s)) {
            return false;
        }
        return defVal;
    }

    private static String evaluateDragBehavior(Map<String, Object> body, double slideX, int targetX) {
        int moveCount;
        if (body == null) {
            return null;
        }
        List track = AuthCaptchaService.parseDragTrack((Object)body.get("track"));
        if (track.isEmpty()) {
            return null;
        }
        long elapsedMs = AuthCaptchaService.parseLong((Object)body.get("elapsedMs"), (long)-1L);
        if (elapsedMs < 0L) {
            double t0 = ((double[])track.get(0))[1];
            double t1 = ((double[])track.get(track.size() - 1))[1];
            elapsedMs = Math.max(0L, Math.round(t1 - t0));
        }
        if ((moveCount = (int)AuthCaptchaService.parseLong((Object)body.get("moveCount"), (long)-1L)) < 0) {
            moveCount = Math.max(0, track.size() - 1);
        }
        if (elapsedMs > -1L && elapsedMs < 120L) {
            return "\u62d6\u62fd\u884c\u4e3a\u8bc4\u5206\u62d2\u7edd\uff1a\u8017\u65f6\u8fc7\u77ed(" + elapsedMs + "ms)";
        }
        if (track.size() < 3) {
            return "\u62d6\u62fd\u884c\u4e3a\u8bc4\u5206\u62d2\u7edd\uff1a\u8f68\u8ff9\u70b9\u8fc7\u5c11(" + track.size() + ")";
        }
        if (moveCount < 2) {
            return "\u62d6\u62fd\u884c\u4e3a\u8bc4\u5206\u62d2\u7edd\uff1amove \u6b21\u6570\u8fc7\u5c11(" + moveCount + ")";
        }
        int stepCount = 0;
        for (int i = 1; i < track.size(); ++i) {
            double dx = ((double[])track.get(i))[0] - ((double[])track.get(i - 1))[0];
            if (Math.abs(dx) < 0.01) continue;
            ++stepCount;
        }
        if (stepCount <= 0) {
            return "\u62d6\u62fd\u884c\u4e3a\u8bc4\u5206\u62d2\u7edd\uff1a\u8f68\u8ff9\u65e0\u6709\u6548\u6b65\u8fdb";
        }
        double trackEndX = ((double[])track.get(track.size() - 1))[0];
        if (Math.abs(trackEndX - slideX) > 8.0) {
            return "\u62d6\u62fd\u884c\u4e3a\u8bc4\u5206\u62d2\u7edd\uff1a\u8f68\u8ff9\u672b\u70b9\u4e0e\u63d0\u4ea4\u4f4d\u79fb\u4e0d\u4e00\u81f4";
        }
        double trackStartX = ((double[])track.get(0))[0];
        if (Math.abs(trackStartX) > 18.0 && Math.abs(trackStartX - (double)targetX) > 18.0) {
            return "\u62d6\u62fd\u884c\u4e3a\u8bc4\u5206\u62d2\u7edd\uff1a\u8f68\u8ff9\u8d77\u70b9\u5f02\u5e38";
        }
        return null;
    }

    private static List<double[]> parseDragTrack(Object raw) {
        if (!(raw instanceof List)) {
            return Collections.emptyList();
        }
        List list = (List)raw;
        ArrayList<double[]> out = new ArrayList<double[]>();
        for (Object item : list) {
            if (!(item instanceof Map)) continue;
            Map m = (Map)item;
            double x = AuthCaptchaService.parseDouble(m.get("x"), (double)Double.NaN);
            double t = AuthCaptchaService.parseDouble(m.get("t"), (double)Double.NaN);
            if (!Double.isFinite(x) || !Double.isFinite(t)) continue;
            out.add(new double[]{x, t});
        }
        if (out.size() >= 2) {
            out.sort((a, b) -> Double.compare(a[1], b[1]));
        }
        return out;
    }

    private static double parseDouble(Object v, double defVal) {
        if (v instanceof Number) {
            Number n = (Number)v;
            return n.doubleValue();
        }
        if (v == null) {
            return defVal;
        }
        try {
            return Double.parseDouble(v.toString().trim());
        }
        catch (Exception ignore) {
            return defVal;
        }
    }

    private static long parseLong(Object v, long defVal) {
        if (v instanceof Number) {
            Number n = (Number)v;
            return n.longValue();
        }
        if (v == null) {
            return defVal;
        }
        try {
            return Long.parseLong(v.toString().trim());
        }
        catch (Exception ignore) {
            return defVal;
        }
    }

    private static String storeKey(long tenantId, String captchaId) {
        return tenantId + ":" + captchaId;
    }

    private static String passKey(long tenantId, String token) {
        return tenantId + ":pass:" + token;
    }

    private void clearExpired() {
        long now = System.currentTimeMillis();
        this.store.entrySet().removeIf(e -> ((CaptchaEntry)e.getValue()).expiresAt < now);
        this.passStore.entrySet().removeIf(e -> ((PassEntry)e.getValue()).expiresAt < now);
    }

    private static String randomCode(int len) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; ++i) {
            sb.append("23456789ABCDEFGHJKLMNPQRSTUVWXYZ".charAt(r.nextInt("23456789ABCDEFGHJKLMNPQRSTUVWXYZ".length())));
        }
        return sb.toString();
    }

    private static boolean dragHolesOverlap(int ax, int ay, int bx, int by, int w, int h) {
        return ax < bx + w && bx < ax + w && ay < by + h && by < ay + h;
    }

    private static boolean isDecoyPlacementOk(int realLeft, int realTop, int dx, int dy, int w, int h) {
        if (AuthCaptchaService.dragHolesOverlap((int)realLeft, (int)realTop, (int)dx, (int)dy, (int)w, (int)h)) {
            return false;
        }
        return Math.abs(dy - realTop) >= 14;
    }

    private static int[] pickDecoyHole(int realLeft, int realTop, ThreadLocalRandom r) {
        int dy;
        int dy2;
        int dx;
        int t;
        int lo = 40;
        int hi = 204;
        int yMin = 6;
        int yMax = 78;
        int w = 36;
        int h = 36;
        for (t = 0; t < 200; ++t) {
            dx = lo + r.nextInt(Math.max(1, hi - lo + 1));
            if (!AuthCaptchaService.isDecoyPlacementOk((int)realLeft, (int)realTop, (int)dx, (int)(dy2 = yMin + r.nextInt(Math.max(1, yMax - yMin + 1))), (int)w, (int)h)) continue;
            return new int[]{dx, dy2};
        }
        for (dy = yMin; dy <= yMax; ++dy) {
            for (dx = lo; dx <= hi; ++dx) {
                if (!AuthCaptchaService.isDecoyPlacementOk((int)realLeft, (int)realTop, (int)dx, (int)dy, (int)w, (int)h)) continue;
                return new int[]{dx, dy};
            }
        }
        for (t = 0; t < 120; ++t) {
            dx = lo + r.nextInt(Math.max(1, hi - lo + 1));
            if (AuthCaptchaService.dragHolesOverlap((int)realLeft, (int)realTop, (int)dx, (int)(dy2 = yMin + r.nextInt(Math.max(1, yMax - yMin + 1))), (int)w, (int)h)) continue;
            return new int[]{dx, dy2};
        }
        for (dy = yMin; dy <= yMax; ++dy) {
            for (dx = lo; dx <= hi; ++dx) {
                if (AuthCaptchaService.dragHolesOverlap((int)realLeft, (int)realTop, (int)dx, (int)dy, (int)w, (int)h)) continue;
                return new int[]{dx, dy};
            }
        }
        return new int[]{lo, yMin};
    }

    private static DragPuzzle randomDragPuzzle() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int lo = 40;
        int hi = 204;
        int targetX = lo + r.nextInt(Math.max(1, hi - lo + 1));
        int yMin = 6;
        int yMax = 78;
        int holeY = yMin + r.nextInt(Math.max(1, yMax - yMin + 1));
        int[] decoy = AuthCaptchaService.pickDecoyHole((int)targetX, (int)holeY, (ThreadLocalRandom)r);
        return new DragPuzzle(AuthCaptchaService.buildDragBackgroundSvg((int)targetX, (int)holeY, (int)decoy[0], (int)decoy[1], (ThreadLocalRandom)r), AuthCaptchaService.buildDragPieceSvg((ThreadLocalRandom)r), targetX, holeY);
    }

    private static String buildDragBackgroundSvg(int realLeft, int realTop, int decoyLeft, int decoyTop, ThreadLocalRandom r) {
        String mid = "dm" + r.nextInt(100000, 999999);
        int rr = Math.min(10, Math.max(6, 7));
        Object layers = AuthCaptchaService.buildNoisyBackgroundLayer((int)280, (int)120, (ThreadLocalRandom)r);
        for (int i = 0; i < 8; ++i) {
            int x1 = r.nextInt(280);
            int y1 = r.nextInt(120);
            int x2 = r.nextInt(280);
            int y2 = r.nextInt(120);
            layers = (String)layers + "<line x1='" + x1 + "' y1='" + y1 + "' x2='" + x2 + "' y2='" + y2 + "' stroke='rgb(55,65,90)' stroke-opacity='0.35' stroke-width='1' />";
        }
        String holeRect = "<rect x='" + realLeft + "' y='" + realTop + "' width='36' height='36' rx='" + rr + "' ry='" + rr + "' fill='black'/><rect x='" + decoyLeft + "' y='" + decoyTop + "' width='36' height='36' rx='" + rr + "' ry='" + rr + "' fill='black'/>";
        return "<svg xmlns='http://www.w3.org/2000/svg' width='280' height='120' viewBox='0 0 280 120'><defs><mask id='" + mid + "'><rect x='0' y='0' width='280' height='120' fill='white'/>" + holeRect + "</mask></defs><g mask='url(#" + mid + ")'>" + (String)layers + "</g><rect x='0' y='0' width='280' height='120' rx='8' ry='8' fill='none' stroke='rgb(180,190,210)' stroke-width='1'/></svg>";
    }

    private static String buildDragPieceSvg(ThreadLocalRandom r) {
        int r1 = r.nextInt(80, 180);
        int g1 = r.nextInt(90, 200);
        int b1 = r.nextInt(120, 220);
        int r2 = r.nextInt(60, 160);
        int g2 = r.nextInt(70, 180);
        int b2 = r.nextInt(100, 200);
        String gid = "dp" + r.nextInt(100000, 999999);
        int pr = Math.min(9, Math.max(5, 7));
        return "<svg xmlns='http://www.w3.org/2000/svg' width='36' height='36' viewBox='0 0 36 36'><defs><linearGradient id='" + gid + "' x1='0' y1='0' x2='1' y2='1'><stop offset='0%' stop-color='rgb(" + r1 + "," + g1 + "," + b1 + ")'/><stop offset='100%' stop-color='rgb(" + r2 + "," + g2 + "," + b2 + ")'/></linearGradient></defs><rect x='1' y='1' width='34' height='34' rx='" + pr + "' ry='" + pr + "' fill='url(#" + gid + ")' stroke='rgb(70,85,120)' stroke-width='1.2'/><line x1='8' y1='18' x2='28' y2='18' stroke='rgba(255,255,255,0.35)' stroke-width='2' stroke-linecap='round'/></svg>";
    }

    private static String buildNoisyBackgroundLayer(int width, int height, ThreadLocalRandom r) {
        int y1;
        int x1;
        int i;
        String gid = "bg" + r.nextInt(100000, 1000000);
        int r1 = r.nextInt(218, 248);
        int g1 = r.nextInt(220, 252);
        int b1 = r.nextInt(228, 255);
        int r2 = r.nextInt(200, 238);
        int g2 = r.nextInt(205, 242);
        int b2 = r.nextInt(210, 248);
        StringBuilder sb = new StringBuilder();
        sb.append("<defs><linearGradient id='").append(gid).append("' x1='0' y1='0' x2='1' y2='1'>");
        sb.append("<stop offset='0%' stop-color='rgb(").append(r1).append(",").append(g1).append(",").append(b1).append(")'/>");
        sb.append("<stop offset='100%' stop-color='rgb(").append(r2).append(",").append(g2).append(",").append(b2).append(")'/>");
        sb.append("</linearGradient></defs>");
        sb.append("<rect x='0' y='0' width='").append(width).append("' height='").append(height).append("' rx='6' ry='6' fill='url(#").append(gid).append(")'/>");
        for (i = 0; i < 48; ++i) {
            int cx = r.nextInt(width);
            int cy = r.nextInt(height);
            int rr = r.nextInt(1, 4);
            int op = r.nextInt(10, 42);
            sb.append("<circle cx='").append(cx).append("' cy='").append(cy).append("' r='").append(rr).append("' fill='rgb(45,55,75)' fill-opacity='0.").append(op).append("' />");
        }
        for (i = 0; i < 26; ++i) {
            x1 = r.nextInt(width);
            y1 = r.nextInt(height);
            int x2 = r.nextInt(width);
            int y2 = r.nextInt(height);
            int opacity = r.nextInt(18, 48);
            int sw = r.nextInt(1, 3);
            sb.append("<line x1='").append(x1).append("' y1='").append(y1).append("' x2='").append(x2).append("' y2='").append(y2).append("' stroke='rgb(55,65,90)' stroke-opacity='0.").append(opacity).append("' stroke-width='").append(sw).append("' />");
        }
        for (i = 0; i < 6; ++i) {
            x1 = r.nextInt(width);
            y1 = r.nextInt(height);
            int mx = r.nextInt(width);
            int my = r.nextInt(height);
            int x2 = r.nextInt(width);
            int y2 = r.nextInt(height);
            int opacity = r.nextInt(15, 38);
            sb.append("<path d='M ").append(x1).append(" ").append(y1).append(" Q ").append(mx).append(" ").append(my).append(" ").append(x2).append(" ").append(y2).append("' fill='none' stroke='rgb(70,80,110)' stroke-opacity='0.").append(opacity).append("' stroke-width='1.2' />");
        }
        return sb.toString();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static String buildImageCaptchaGifDataUri(String code) {
        String string;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int width = 160;
        int height = 54;
        int n = code.length();
        int fontSize = r.nextInt(24, 28);
        double charW = (double)fontSize * 0.66;
        double minAdvance = charW * 0.72;
        int[] charX = AuthCaptchaService.layoutImageCaptchaCharXs((int)width, (int)n, (double)charW, (double)minAdvance, (ThreadLocalRandom)r);
        int[] charY = new int[n];
        int[] rotate = new int[n];
        int[] skew = new int[n];
        int[] color = new int[n];
        for (int i = 0; i < n; ++i) {
            charY[i] = 34 + r.nextInt(-3, 4);
            rotate[i] = r.nextInt(-22, 23);
            skew[i] = r.nextInt(-9, 10);
            color[i] = r.nextInt(2042429, 5925509);
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream(8192);
        try {
            GifSequenceWriter writer = new GifSequenceWriter(bos, 2, 14, true);
            BufferedImage staticBg = AuthCaptchaService.buildImageCaptchaGifStaticBg((int)width, (int)height, (ThreadLocalRandom)r);
            boolean[][] hiddenSchedule = AuthCaptchaService.buildHiddenSchedule((int)n, (int)12, (int)2, (int)3);
            for (int f = 0; f < 12; ++f) {
                BufferedImage frame = new BufferedImage(width, height, 2);
                Graphics2D g = frame.createGraphics();
                try {
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g.drawImage((Image)staticBg, 0, 0, null);
                    AuthCaptchaService.drawImageCaptchaGifDynamicNoise((Graphics2D)g, (int)width, (int)height, (ThreadLocalRandom)r);
                    boolean[] hidden = hiddenSchedule[f];
                    for (int i = 0; i < n; ++i) {
                        if (hidden[i]) continue;
                        g.translate(charX[i], charY[i]);
                        g.rotate(Math.toRadians(rotate[i]));
                        g.shear(Math.tan(Math.toRadians(skew[i])), 0.0);
                        g.setFont(new Font("Arial", 1, fontSize));
                        int rgb = color[i];
                        g.setComposite(AlphaComposite.getInstance(3, 0.94f));
                        g.setColor(new Color(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF));
                        g.drawString(String.valueOf(code.charAt(i)), 0, 0);
                        g.setComposite(AlphaComposite.SrcOver);
                        g.setTransform(new AffineTransform());
                    }
                }
                finally {
                    g.dispose();
                }
                writer.writeFrame(frame);
            }
            writer.close();
            string = "data:image/gif;base64," + Base64.getEncoder().encodeToString(bos.toByteArray());
        }
        catch (Throwable throwable) {
            try {
                try {
                    bos.close();
                }
                catch (Throwable throwable2) {
                    throwable.addSuppressed(throwable2);
                }
                throw throwable;
            }
            catch (IOException e) {
                throw new RuntimeException("\u751f\u6210 GIF \u9a8c\u8bc1\u7801\u5931\u8d25", e);
            }
        }
        bos.close();
        return string;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static BufferedImage buildImageCaptchaGifStaticBg(int width, int height, ThreadLocalRandom r) {
        BufferedImage bg = new BufferedImage(width, height, 2);
        Graphics2D g = bg.createGraphics();
        try {
            int i;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color[][] palettes = new Color[][]{{new Color(238, 246, 255), new Color(231, 242, 248)}, {new Color(244, 240, 255), new Color(235, 229, 248)}, {new Color(236, 248, 241), new Color(229, 244, 236)}, {new Color(255, 244, 236), new Color(248, 236, 227)}};
            Color[] palette = palettes[r.nextInt(palettes.length)];
            GradientPaint gp = new GradientPaint(0.0f, 0.0f, palette[0], width, height, palette[1]);
            g.setPaint(gp);
            g.fillRect(0, 0, width, height);
            for (i = 0; i < 24; ++i) {
                int x = r.nextInt(width);
                int y = r.nextInt(height);
                int rr = r.nextInt(1, 3);
                int a = r.nextInt(14, 30);
                int tone = 120 + r.nextInt(40);
                g.setColor(new Color(tone - 30, tone - 15, tone, a));
                g.fillOval(x, y, rr, rr);
            }
            for (i = 0; i < 5; ++i) {
                int x1 = r.nextInt(width);
                int y1 = r.nextInt(height);
                int x2 = r.nextInt(width);
                int y2 = r.nextInt(height);
                int a = r.nextInt(18, 36);
                Color[] lineColors = new Color[]{new Color(88, 116, 160, a), new Color(120, 98, 168, a), new Color(86, 138, 132, a)};
                g.setColor(lineColors[r.nextInt(lineColors.length)]);
                g.drawLine(x1, y1, x2, y2);
            }
        }
        finally {
            g.dispose();
        }
        return bg;
    }

    private static boolean[][] buildHiddenSchedule(int n, int totalFrames, int hideCount, int holdFrames) {
        boolean[][] schedule = new boolean[totalFrames][Math.max(0, n)];
        if (n <= 0 || totalFrames <= 0) {
            return schedule;
        }
        int m = Math.min(Math.max(1, hideCount), n);
        int hold = Math.max(1, holdFrames);
        int offset = Math.max(1, n / 2);
        for (int f = 0; f < totalFrames; ++f) {
            int phase = f / hold % n;
            for (int k = 0; k < m; ++k) {
                int idx = (phase + k * offset) % n;
                for (int probe = 0; schedule[f][idx] && probe < n; ++probe) {
                    idx = (idx + 1) % n;
                }
                schedule[f][idx] = true;
            }
        }
        return schedule;
    }

    private static void drawImageCaptchaGifDynamicNoise(Graphics2D g, int width, int height, ThreadLocalRandom r) {
        int i;
        for (i = 0; i < 6; ++i) {
            int x = r.nextInt(width);
            int y = r.nextInt(height);
            int rr = r.nextInt(1, 3);
            int a = r.nextInt(10, 26);
            g.setColor(new Color(108, 118, 146, a));
            g.fillOval(x, y, rr, rr);
        }
        for (i = 0; i < 2; ++i) {
            int x1 = r.nextInt(width);
            int y1 = r.nextInt(height);
            int x2 = r.nextInt(width);
            int y2 = r.nextInt(height);
            int a = r.nextInt(14, 34);
            g.setColor(new Color(58, 72, 102, a));
            g.drawLine(x1, y1, x2, y2);
        }
    }

    private static int[] layoutImageCaptchaCharXs(int width, int n, double charW, double minAdvance, ThreadLocalRandom r) {
        int i;
        int[] xs = new int[n];
        int marginL = 8;
        int marginR = 8;
        xs[0] = marginL + r.nextInt(0, 5);
        for (i = 1; i < n; ++i) {
            int tailMin;
            int maxForCanvas;
            int hiExclusive;
            int hi;
            int lo = (int)Math.ceil((double)xs[i - 1] + minAdvance);
            if (lo > (hi = Math.min(hiExclusive = (int)Math.floor((double)xs[i - 1] + charW * 1.06), maxForCanvas = (int)Math.floor((double)(width - marginR) - charW - (double)(tailMin = (int)Math.ceil((double)(n - 1 - i) * minAdvance)))))) {
                lo = Math.min(lo, maxForCanvas);
                hi = maxForCanvas;
            }
            xs[i] = lo + (hi > lo ? r.nextInt(0, hi - lo + 1) : 0);
        }
        if ((double)xs[n - 1] + charW > (double)(width - marginR)) {
            xs[n - 1] = (int)Math.floor((double)(width - marginR) - charW);
            for (i = n - 2; i >= 0; --i) {
                int maxXi = (int)Math.floor((double)xs[i + 1] - minAdvance);
                if (xs[i] <= maxXi) continue;
                xs[i] = Math.max(i == 0 ? marginL : (int)Math.ceil((double)xs[i - 1] + minAdvance), maxXi);
            }
        }
        return xs;
    }

    private static ClickChallenge randomClickChallenge() {
        List orderedIndices;
        List figures;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        while (!(AuthCaptchaService.hasDuplicateColor((List)(figures = AuthCaptchaService.randomFiguresWithScatter((ThreadLocalRandom)r, (int)7))) && AuthCaptchaService.hasDuplicateShape((List)figures) && AuthCaptchaService.hasAtLeastTwoDistinctColors((List)figures) && (orderedIndices = AuthCaptchaService.pickOrderedTargetIndices((List)figures, (ThreadLocalRandom)r)) != null)) {
        }
        ArrayList<int[]> seq = new ArrayList<int[]>(3);
        ArrayList<ClickTargetMeta> metas = new ArrayList<ClickTargetMeta>(3);
        StringBuilder prompt = new StringBuilder();
        for (int i = 0; i < orderedIndices.size(); ++i) {
            int pickIdx = (Integer)orderedIndices.get(i);
            ClickFigure f = (ClickFigure)figures.get(pickIdx);
            seq.add(new int[]{f.cx, f.cy, f.hitRadius});
            ClickTargetMeta meta = AuthCaptchaService.buildClickTargetMeta((List)figures, (int)pickIdx, (ThreadLocalRandom)r);
            metas.add(meta);
            if (i > 0) {
                prompt.append("_then_");
            }
            prompt.append(f.colorKey).append("-").append(f.sizeKey).append("-").append(f.shapeKey);
            if (meta.qualifierKey() == null || meta.qualifierKey().isBlank()) continue;
            prompt.append("@").append(meta.qualifierKey());
        }
        return new ClickChallenge(figures, List.copyOf(seq), List.copyOf(metas), prompt.toString());
    }

    private static List<Integer> pickOrderedTargetIndices(List<ClickFigure> figures, ThreadLocalRandom r) {
        ArrayList<Integer> eligible = new ArrayList<Integer>();
        for (int i = 0; i < figures.size(); ++i) {
            if (!AuthCaptchaService.isPromptResolvable(figures, (int)i)) continue;
            eligible.add(i);
        }
        if (eligible.size() < 3) {
            return null;
        }
        Collections.shuffle(eligible, r);
        ArrayList<Integer> picked = new ArrayList<Integer>(3);
        for (int i = 0; i < 3; ++i) {
            picked.add((Integer)eligible.get(i));
        }
        if (r.nextBoolean()) {
            Collections.reverse(picked);
        }
        return picked;
    }

    private static ClickTargetMeta buildClickTargetMeta(List<ClickFigure> figures, int selectedIdx, ThreadLocalRandom r) {
        ClickFigure f = figures.get(selectedIdx);
        List qualifiers = AuthCaptchaService.availableQualifiers(figures, (int)selectedIdx);
        String qualifier = qualifiers.isEmpty() ? null : (String)qualifiers.get(r.nextInt(qualifiers.size()));
        return new ClickTargetMeta(f.colorKey, f.sizeKey, f.shapeKey, qualifier);
    }

    private static boolean isPromptResolvable(List<ClickFigure> figures, int selectedIdx) {
        ClickFigure f = figures.get(selectedIdx);
        String key = AuthCaptchaService.targetLabelKey((ClickFigure)f);
        long same = figures.stream().filter(x -> AuthCaptchaService.targetLabelKey((ClickFigure)x).equals(key)).count();
        if (same <= 1L) {
            return true;
        }
        return !AuthCaptchaService.availableQualifiers(figures, (int)selectedIdx).isEmpty();
    }

    private static List<String> availableQualifiers(List<ClickFigure> figures, int selectedIdx) {
        ClickFigure f = figures.get(selectedIdx);
        String key = AuthCaptchaService.targetLabelKey((ClickFigure)f);
        List same = figures.stream().filter(x -> AuthCaptchaService.targetLabelKey((ClickFigure)x).equals(key)).collect(Collectors.toList());
        if (same.size() <= 1) {
            return Collections.emptyList();
        }
        ArrayList<String> out = new ArrayList<String>(4);
        int minX = same.stream().mapToInt(x -> x.cx).min().orElse(f.cx);
        int maxX = same.stream().mapToInt(x -> x.cx).max().orElse(f.cx);
        int minY = same.stream().mapToInt(x -> x.cy).min().orElse(f.cy);
        int maxY = same.stream().mapToInt(x -> x.cy).max().orElse(f.cy);
        long minXC = same.stream().filter(x -> x.cx == minX).count();
        long maxXC = same.stream().filter(x -> x.cx == maxX).count();
        long minYC = same.stream().filter(x -> x.cy == minY).count();
        long maxYC = same.stream().filter(x -> x.cy == maxY).count();
        if (f.cx == minX && minXC == 1L) {
            out.add("leftmost");
        }
        if (f.cx == maxX && maxXC == 1L) {
            out.add("rightmost");
        }
        if (f.cy == minY && minYC == 1L) {
            out.add("topmost");
        }
        if (f.cy == maxY && maxYC == 1L) {
            out.add("bottommost");
        }
        return out;
    }

    private static String buildClickSvg(List<ClickFigure> figures) {
        int width = 280;
        int height = 96;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        StringBuilder defs = new StringBuilder("<defs>");
        ArrayList<String> fillIds = new ArrayList<String>(figures.size());
        for (int i = 0; i < figures.size(); ++i) {
            fillIds.add(AuthCaptchaService.appendClickFigureTexturedFillDef((StringBuilder)defs, (ClickFigure)figures.get(i), (int)i, (ThreadLocalRandom)r));
        }
        defs.append("</defs>");
        StringBuilder bg = new StringBuilder(AuthCaptchaService.buildNoisyBackgroundLayer((int)width, (int)height, (ThreadLocalRandom)r));
        for (int i = 0; i < 10; ++i) {
            int x1 = r.nextInt(width);
            int y1 = r.nextInt(height);
            int x2 = r.nextInt(width);
            int y2 = r.nextInt(height);
            int opacity = r.nextInt(20, 45);
            bg.append("<line x1='").append(x1).append("' y1='").append(y1).append("' x2='").append(x2).append("' y2='").append(y2).append("' stroke='rgb(40,50,70)' stroke-opacity='0.").append(opacity).append("' stroke-width='1' />");
        }
        StringBuilder figSvg = new StringBuilder();
        for (int i = 0; i < figures.size(); ++i) {
            ClickFigure f = figures.get(i);
            String fillRef = "url(#" + (String)fillIds.get(i) + ")";
            int unit = f.sizePx;
            figSvg.append("<g transform='rotate(").append(f.rotateDeg).append(" ").append(f.cx).append(" ").append(f.cy).append(")'>");
            if ("circle".equals(f.shapeKey)) {
                figSvg.append("<circle cx='").append(f.cx).append("' cy='").append(f.cy).append("' r='").append(unit).append("' fill='").append(fillRef).append("' fill-opacity='").append("0.9").append("' stroke='rgb(0,0,0)' stroke-opacity='0.12' stroke-width='0.6' />");
            } else if ("square".equals(f.shapeKey)) {
                int side = unit * 2;
                figSvg.append("<rect x='").append(f.cx - unit).append("' y='").append(f.cy - unit).append("' width='").append(side).append("' height='").append(side).append("' rx='2' fill='").append(fillRef).append("' fill-opacity='").append("0.9").append("' stroke='rgb(0,0,0)' stroke-opacity='0.12' stroke-width='0.6' />");
            } else {
                int up = unit + 1;
                int down = unit - 1;
                figSvg.append("<polygon points='").append(f.cx).append(",").append(f.cy - up).append(" ").append(f.cx - unit).append(",").append(f.cy + down).append(" ").append(f.cx + unit).append(",").append(f.cy + down).append("' fill='").append(fillRef).append("' fill-opacity='").append("0.9").append("' stroke='rgb(0,0,0)' stroke-opacity='0.12' stroke-width='0.6' />");
            }
            figSvg.append("</g>");
        }
        return "<svg xmlns='http://www.w3.org/2000/svg' width='" + width + "' height='" + height + "' viewBox='0 0 " + width + " " + height + "'>" + String.valueOf(defs) + String.valueOf(bg) + String.valueOf(figSvg) + "</svg>";
    }

    private static String appendClickFigureTexturedFillDef(StringBuilder defs, ClickFigure f, int index, ThreadLocalRandom r) {
        String id = "ctf" + index + "n" + r.nextInt(10000, 99999);
        int[] rgb = AuthCaptchaService.hexToRgb((String)f.colorHex);
        int kind = r.nextInt(5);
        switch (kind) {
            case 0: {
                AuthCaptchaService.appendClickLinearGradientDef((StringBuilder)defs, (String)id, (int[])rgb, (ThreadLocalRandom)r);
                break;
            }
            case 1: {
                AuthCaptchaService.appendClickRadialGradientDef((StringBuilder)defs, (String)id, (int[])rgb, (ThreadLocalRandom)r);
                break;
            }
            case 2: {
                AuthCaptchaService.appendClickStripePatternDef((StringBuilder)defs, (String)id, (int[])rgb, (ThreadLocalRandom)r);
                break;
            }
            case 3: {
                AuthCaptchaService.appendClickDotsPatternDef((StringBuilder)defs, (String)id, (int[])rgb, (ThreadLocalRandom)r);
                break;
            }
            default: {
                AuthCaptchaService.appendClickMultiStopGradientDef((StringBuilder)defs, (String)id, (int[])rgb, (ThreadLocalRandom)r);
            }
        }
        return id;
    }

    private static int[] hexToRgb(String hex) {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        int v = Integer.parseInt(h, 16);
        return new int[]{v >> 16 & 0xFF, v >> 8 & 0xFF, v & 0xFF};
    }

    private static String rgbHex(int red, int green, int blue) {
        return String.format("#%02x%02x%02x", AuthCaptchaService.clamp255((int)red), AuthCaptchaService.clamp255((int)green), AuthCaptchaService.clamp255((int)blue));
    }

    private static String rgbHex(int[] rgb) {
        return AuthCaptchaService.rgbHex((int)rgb[0], (int)rgb[1], (int)rgb[2]);
    }

    private static int clamp255(int x) {
        return Math.max(0, Math.min(255, x));
    }

    private static int[] varyRgb(int[] base, ThreadLocalRandom r, int maxDelta) {
        int[] out = new int[]{AuthCaptchaService.clamp255((int)(base[0] + r.nextInt(-maxDelta, maxDelta + 1))), AuthCaptchaService.clamp255((int)(base[1] + r.nextInt(-maxDelta, maxDelta + 1))), AuthCaptchaService.clamp255((int)(base[2] + r.nextInt(-maxDelta, maxDelta + 1)))};
        int dominant = 0;
        if (base[1] > base[dominant]) {
            dominant = 1;
        }
        if (base[2] > base[dominant]) {
            dominant = 2;
        }
        for (int i = 0; i < 3; ++i) {
            if (i == dominant) continue;
            int cap = Math.min(base[i] + Math.max(8, maxDelta / 2), base[dominant] - 40);
            out[i] = Math.min(out[i], AuthCaptchaService.clamp255((int)cap));
        }
        int otherA = (dominant + 1) % 3;
        int otherB = (dominant + 2) % 3;
        int minDom = Math.max(base[dominant] - Math.max(6, maxDelta / 3), Math.max(out[otherA], out[otherB]) + 35);
        out[dominant] = Math.max(out[dominant], AuthCaptchaService.clamp255((int)minDom));
        return out;
    }

    private static void appendClickLinearGradientDef(StringBuilder defs, String id, int[] rgb, ThreadLocalRandom r) {
        int deg = r.nextInt(360);
        defs.append("<linearGradient id='").append(id).append("' gradientUnits='objectBoundingBox' x1='0' y1='0' x2='1' y2='1' gradientTransform='rotate(").append(deg).append(" 0.5 0.5)'>");
        defs.append("<stop offset='0%' stop-color='").append(AuthCaptchaService.rgbHex((int[])AuthCaptchaService.varyRgb((int[])rgb, (ThreadLocalRandom)r, (int)20))).append("'/>");
        defs.append("<stop offset='28%' stop-color='").append(AuthCaptchaService.rgbHex((int[])AuthCaptchaService.varyRgb((int[])rgb, (ThreadLocalRandom)r, (int)12))).append("'/>");
        defs.append("<stop offset='52%' stop-color='").append(AuthCaptchaService.rgbHex((int[])rgb)).append("'/>");
        defs.append("<stop offset='78%' stop-color='").append(AuthCaptchaService.rgbHex((int[])AuthCaptchaService.varyRgb((int[])rgb, (ThreadLocalRandom)r, (int)14))).append("'/>");
        defs.append("<stop offset='100%' stop-color='").append(AuthCaptchaService.rgbHex((int[])AuthCaptchaService.varyRgb((int[])rgb, (ThreadLocalRandom)r, (int)20))).append("'/>");
        defs.append("</linearGradient>");
    }

    private static void appendClickRadialGradientDef(StringBuilder defs, String id, int[] rgb, ThreadLocalRandom r) {
        double fx = r.nextDouble() * 0.45 + 0.2;
        double fy = r.nextDouble() * 0.45 + 0.2;
        defs.append("<radialGradient id='").append(id).append("' gradientUnits='objectBoundingBox' cx='").append(String.format(Locale.US, "%.3f", fx)).append("' cy='").append(String.format(Locale.US, "%.3f", fy)).append("' r='0.92'>");
        defs.append("<stop offset='0%' stop-color='").append(AuthCaptchaService.rgbHex((int[])AuthCaptchaService.varyRgb((int[])rgb, (ThreadLocalRandom)r, (int)22))).append("'/>");
        defs.append("<stop offset='45%' stop-color='").append(AuthCaptchaService.rgbHex((int[])AuthCaptchaService.varyRgb((int[])rgb, (ThreadLocalRandom)r, (int)10))).append("'/>");
        defs.append("<stop offset='100%' stop-color='").append(AuthCaptchaService.rgbHex((int[])AuthCaptchaService.varyRgb((int[])rgb, (ThreadLocalRandom)r, (int)16))).append("'/>");
        defs.append("</radialGradient>");
    }

    private static void appendClickStripePatternDef(StringBuilder defs, String id, int[] rgb, ThreadLocalRandom r) {
        String gid = id + "_sg";
        int[] c0 = AuthCaptchaService.varyRgb((int[])rgb, (ThreadLocalRandom)r, (int)16);
        int[] c1 = AuthCaptchaService.varyRgb((int[])rgb, (ThreadLocalRandom)r, (int)16);
        defs.append("<linearGradient id='").append(gid).append("' x1='0' y1='0' x2='1' y2='1'>");
        defs.append("<stop offset='0%' stop-color='").append(AuthCaptchaService.rgbHex((int[])c0)).append("'/>");
        defs.append("<stop offset='100%' stop-color='").append(AuthCaptchaService.rgbHex((int[])c1)).append("'/>");
        defs.append("</linearGradient>");
        int pw = r.nextInt(5, 10);
        defs.append("<pattern id='").append(id).append("' patternUnits='userSpaceOnUse' width='").append(pw).append("' height='").append(pw).append("' patternTransform='rotate(").append(r.nextInt(360)).append(")'>");
        defs.append("<rect width='").append(pw).append("' height='").append(pw).append("' fill='url(#").append(gid).append(")'/>");
        defs.append("<line x1='0' y1='0' x2='").append(pw).append("' y2='").append(pw).append("' stroke='").append(AuthCaptchaService.rgbHex((int[])AuthCaptchaService.varyRgb((int[])rgb, (ThreadLocalRandom)r, (int)10))).append("' stroke-width='1.1' opacity='0.55'/>");
        defs.append("<line x1='0' y1='").append(pw).append("' x2='").append(pw).append("' y2='0' stroke='").append(AuthCaptchaService.rgbHex((int[])AuthCaptchaService.varyRgb((int[])rgb, (ThreadLocalRandom)r, (int)9))).append("' stroke-width='0.9' opacity='0.4'/>");
        defs.append("</pattern>");
    }

    private static void appendClickDotsPatternDef(StringBuilder defs, String id, int[] rgb, ThreadLocalRandom r) {
        int pw = r.nextInt(5, 8);
        defs.append("<pattern id='").append(id).append("' patternUnits='userSpaceOnUse' width='").append(pw).append("' height='").append(pw).append("'>");
        defs.append("<rect width='").append(pw).append("' height='").append(pw).append("' fill='").append(AuthCaptchaService.rgbHex((int[])AuthCaptchaService.varyRgb((int[])rgb, (ThreadLocalRandom)r, (int)14))).append("' opacity='0.88'/>");
        defs.append("<circle cx='").append((double)pw / 2.0).append("' cy='").append((double)pw / 2.0).append("' r='1.5' fill='").append(AuthCaptchaService.rgbHex((int[])rgb)).append("' opacity='0.95'/>");
        defs.append("<circle cx='1.2' cy='1.2' r='0.85' fill='").append(AuthCaptchaService.rgbHex((int[])AuthCaptchaService.varyRgb((int[])rgb, (ThreadLocalRandom)r, (int)10))).append("' opacity='0.75'/>");
        defs.append("<circle cx='").append((double)pw - 1.2).append("' cy='").append((double)pw - 1.2).append("' r='0.75' fill='").append(AuthCaptchaService.rgbHex((int[])AuthCaptchaService.varyRgb((int[])rgb, (ThreadLocalRandom)r, (int)10))).append("' opacity='0.65'/>");
        defs.append("</pattern>");
    }

    private static void appendClickMultiStopGradientDef(StringBuilder defs, String id, int[] rgb, ThreadLocalRandom r) {
        int deg = r.nextInt(360);
        defs.append("<linearGradient id='").append(id).append("' gradientUnits='objectBoundingBox' x1='0' y1='0' x2='1' y2='0' gradientTransform='rotate(").append(deg).append(" 0.5 0.5)'>");
        defs.append("<stop offset='0%' stop-color='").append(AuthCaptchaService.rgbHex((int[])AuthCaptchaService.varyRgb((int[])rgb, (ThreadLocalRandom)r, (int)18))).append("'/>");
        defs.append("<stop offset='20%' stop-color='").append(AuthCaptchaService.rgbHex((int[])AuthCaptchaService.varyRgb((int[])rgb, (ThreadLocalRandom)r, (int)10))).append("'/>");
        defs.append("<stop offset='40%' stop-color='").append(AuthCaptchaService.rgbHex((int[])rgb)).append("'/>");
        defs.append("<stop offset='60%' stop-color='").append(AuthCaptchaService.rgbHex((int[])AuthCaptchaService.varyRgb((int[])rgb, (ThreadLocalRandom)r, (int)12))).append("'/>");
        defs.append("<stop offset='80%' stop-color='").append(AuthCaptchaService.rgbHex((int[])AuthCaptchaService.varyRgb((int[])rgb, (ThreadLocalRandom)r, (int)9))).append("'/>");
        defs.append("<stop offset='100%' stop-color='").append(AuthCaptchaService.rgbHex((int[])AuthCaptchaService.varyRgb((int[])rgb, (ThreadLocalRandom)r, (int)18))).append("'/>");
        defs.append("</linearGradient>");
    }

    private static ClickFigure randomFigureAt(int x, int y, ThreadLocalRandom r) {
        int color = r.nextInt(CLICK_COLOR_KEYS.length);
        int shape = r.nextInt(CLICK_SHAPE_KEYS.length);
        int size = r.nextInt(CLICK_SIZE_KEYS.length);
        int sizePx = size == 0 ? 11 : 16;
        int rotateDeg = r.nextInt(360);
        return new ClickFigure(x, y, CLICK_COLOR_KEYS[color], CLICK_COLOR_HEX[color], CLICK_SHAPE_KEYS[shape], CLICK_SIZE_KEYS[size], sizePx, sizePx + 5, rotateDeg);
    }

    private static List<ClickFigure> randomFiguresWithScatter(ThreadLocalRandom r, int count) {
        ArrayList<ClickFigure> list = new ArrayList<ClickFigure>(count);
        for (int tries = 0; list.size() < count && tries < 200; ++tries) {
            int y;
            int x = r.nextInt(20, 260);
            ClickFigure fig = AuthCaptchaService.randomFigureAt((int)x, (int)(y = r.nextInt(16, 80)), (ThreadLocalRandom)r);
            if (AuthCaptchaService.isTooCloseToAny(list, (ClickFigure)fig)) continue;
            list.add(fig);
        }
        if (list.size() < count) {
            list.clear();
            list.add(AuthCaptchaService.randomFigureAt((int)28, (int)22, (ThreadLocalRandom)r));
            list.add(AuthCaptchaService.randomFigureAt((int)84, (int)26, (ThreadLocalRandom)r));
            list.add(AuthCaptchaService.randomFigureAt((int)140, (int)20, (ThreadLocalRandom)r));
            list.add(AuthCaptchaService.randomFigureAt((int)196, (int)24, (ThreadLocalRandom)r));
            list.add(AuthCaptchaService.randomFigureAt((int)252, (int)22, (ThreadLocalRandom)r));
            list.add(AuthCaptchaService.randomFigureAt((int)70, (int)68, (ThreadLocalRandom)r));
            list.add(AuthCaptchaService.randomFigureAt((int)210, (int)68, (ThreadLocalRandom)r));
        }
        return list;
    }

    private static boolean isTooCloseToAny(List<ClickFigure> existing, ClickFigure candidate) {
        for (ClickFigure f : existing) {
            int dx = f.cx - candidate.cx;
            int dy = f.cy - candidate.cy;
            double minDist = f.hitRadius + candidate.hitRadius + 3;
            if (!((double)(dx * dx + dy * dy) < minDist * minDist)) continue;
            return true;
        }
        return false;
    }

    private static String targetLabelKey(ClickFigure f) {
        return f.colorKey + "|" + f.sizeKey + "|" + f.shapeKey;
    }

    private static boolean hasDuplicateColor(List<ClickFigure> list) {
        return list.stream().map(f -> f.colorKey).distinct().count() < (long)list.size();
    }

    private static boolean hasDuplicateShape(List<ClickFigure> list) {
        return list.stream().map(f -> f.shapeKey).distinct().count() < (long)list.size();
    }

    private static boolean hasAtLeastTwoDistinctColors(List<ClickFigure> list) {
        return list.stream().map(f -> f.colorKey).distinct().count() >= 2L;
    }

    private static Integer parseInt(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            Number n = (Number)v;
            return n.intValue();
        }
        try {
            return Integer.parseInt(v.toString().trim());
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    private static List<int[]> parseOrderedClickPoints(Map<String, Object> body) {
        List list;
        if (body == null) {
            return null;
        }
        Object raw = body.get("clicks");
        if (!(raw instanceof List) || (list = (List)raw).isEmpty()) {
            return null;
        }
        ArrayList<int[]> out = new ArrayList<int[]>(list.size());
        for (Object o : list) {
            if (!(o instanceof Map)) {
                return null;
            }
            Map m = (Map)o;
            Integer x = AuthCaptchaService.parseInt(m.get("x"));
            Integer y = AuthCaptchaService.parseInt(m.get("y"));
            if (x == null || y == null) {
                return null;
            }
            out.add(new int[]{x, y});
        }
        return out;
    }

    private boolean isCaptchaDebugProfileActive() {
        for (String profile : this.environment.getActiveProfiles()) {
            if (!"dev".equalsIgnoreCase(profile)) continue;
            return true;
        }
        return false;
    }

    private boolean shouldDebugSaveOnFail() {
        return this.debugSaveOnFail && this.isCaptchaDebugProfileActive();
    }

    private boolean shouldDebugSaveOnSuccess() {
        return this.debugSaveOnSuccess && this.isCaptchaDebugProfileActive();
    }

    private void debugSaveOnCaptchaFail(CaptchaEntry entry, String reason) {
        this.debugSaveOnCaptchaFail(entry, reason, null, null);
    }

    private void debugSaveOnCaptchaFail(CaptchaEntry entry, String reason, List<int[]> userClicks) {
        this.debugSaveOnCaptchaFail(entry, reason, userClicks, null);
    }

    private void debugSaveOnCaptchaFail(CaptchaEntry entry, String reason, List<int[]> userClicks, String imageCaptchaUserInput) {
        if (entry == null || entry.svg == null || entry.svg.isBlank() || !this.shouldDebugSaveOnFail()) {
            return;
        }
        try {
            Path dir = Paths.get(this.debugSaveDir, new String[0]);
            if (!dir.isAbsolute()) {
                dir = Paths.get(this.workspaceDir, new String[0]).resolve(dir).normalize();
            }
            Files.createDirectories(dir, new FileAttribute[0]);
            String baseRaw = entry.prompt == null || entry.prompt.isBlank() ? reason : entry.prompt;
            String base = AuthCaptchaService.sanitizeFileName((String)AuthCaptchaService.stripClickCaptchaPromptPrefix((String)baseRaw));
            if (base.isBlank()) {
                base = AuthCaptchaService.sanitizeFileName((String)reason);
            }
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
            String stem = ts + "_" + base;
            String svgOut = AuthCaptchaService.augmentSvgForCaptchaDebug((CaptchaEntry)entry, userClicks, (String)imageCaptchaUserInput);
            Path written = this.persistCaptchaDebugPngOrSvg(dir, stem, entry, svgOut);
            log.info("Captcha debug saved: reason={}, file={}", (Object)reason, (Object)written.toAbsolutePath());
        }
        catch (Exception ex) {
            log.warn("Captcha debug save failed: {}", (Object)ex.getMessage());
        }
    }

    private void debugSaveOnCaptchaSuccess(CaptchaEntry entry, String reason, List<int[]> userClicks) {
        this.debugSaveOnCaptchaSuccess(entry, reason, userClicks, null);
    }

    private void debugSaveOnCaptchaSuccess(CaptchaEntry entry, String reason, List<int[]> userClicks, String imageCaptchaUserInput) {
        if (entry == null || entry.svg == null || entry.svg.isBlank() || !this.shouldDebugSaveOnSuccess()) {
            return;
        }
        try {
            Path dir = Paths.get(this.debugSaveDir, new String[0]);
            if (!dir.isAbsolute()) {
                dir = Paths.get(this.workspaceDir, new String[0]).resolve(dir).normalize();
            }
            Files.createDirectories(dir, new FileAttribute[0]);
            String baseRaw = entry.prompt == null || entry.prompt.isBlank() ? reason : entry.prompt;
            String base = AuthCaptchaService.sanitizeFileName((String)AuthCaptchaService.stripClickCaptchaPromptPrefix((String)baseRaw));
            if (base.isBlank()) {
                base = AuthCaptchaService.sanitizeFileName((String)reason);
            }
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
            String stem = "ok_" + ts + "_" + base;
            String svgOut = AuthCaptchaService.augmentSvgForCaptchaDebug((CaptchaEntry)entry, userClicks, (String)imageCaptchaUserInput);
            Path written = this.persistCaptchaDebugPngOrSvg(dir, stem, entry, svgOut);
            log.info("Captcha debug (success) saved: reason={}, file={}", (Object)reason, (Object)written.toAbsolutePath());
        }
        catch (Exception ex) {
            log.warn("Captcha debug (success) save failed: {}", (Object)ex.getMessage());
        }
    }

    private Path persistCaptchaDebugPngOrSvg(Path dir, String fileStem, CaptchaEntry entry, String svgOut) throws Exception {
        Path png = dir.resolve(fileStem + ".png");
        try {
            AuthCaptchaService.rasterizeCaptchaDebugSvgToPng((String)svgOut, (Path)png, (CaptchaEntry)entry);
            return png;
        }
        catch (Exception rasterEx) {
            log.warn("Captcha debug PNG raster failed ({}), writing SVG fallback", (Object)rasterEx.getMessage());
            try {
                Files.deleteIfExists(png);
            }
            catch (IOException delEx) {
                log.debug("Could not remove partial PNG: {}", (Object)delEx.getMessage());
            }
            Path svg = dir.resolve(fileStem + ".svg");
            Files.writeString((Path)svg, (CharSequence)svgOut, (Charset)StandardCharsets.UTF_8, (OpenOption[])new OpenOption[0]);
            return svg;
        }
    }

    private static int[] parseCaptchaDebugSvgPixelSize(String svg, CaptchaEntry entry) {
        int h;
        int w;
        int[] parsed = AuthCaptchaService.tryParseSvgRootWh((String)svg);
        if (parsed != null) {
            return parsed;
        }
        if ("click".equals(entry.type)) {
            w = 280;
            h = 96;
        } else if ("drag".equals(entry.type)) {
            w = 280;
            h = 120;
        } else {
            w = 160;
            h = 54;
        }
        return new int[]{w, h};
    }

    private static int[] tryParseSvgRootWh(String svg) {
        int open = svg.indexOf("<svg");
        if (open < 0) {
            return null;
        }
        int close = svg.indexOf(62, open);
        if (close < 0) {
            return null;
        }
        String tag = svg.substring(open, close + 1);
        Integer w = null;
        Integer h = null;
        Matcher m = SVG_ATTR_INT.matcher(tag);
        while (m.find()) {
            int v;
            String name = m.group(1);
            try {
                v = Integer.parseInt(m.group(2));
            }
            catch (NumberFormatException e) {
                continue;
            }
            if (v <= 0) continue;
            if ("width".equals(name)) {
                w = v;
                continue;
            }
            h = v;
        }
        if (w != null && h != null) {
            return new int[]{w, h};
        }
        return null;
    }

    private static void rasterizeCaptchaDebugSvgToPng(String svg, Path outFile, CaptchaEntry entry) throws Exception {
        int[] wh = AuthCaptchaService.parseCaptchaDebugSvgPixelSize((String)svg, (CaptchaEntry)entry);
        int w = wh[0];
        int h = wh[1];
        int pw = w * 2;
        int ph = h * 2;
        PNGTranscoder transcoder = new PNGTranscoder();
        transcoder.addTranscodingHint(ImageTranscoder.KEY_WIDTH, (Object)Float.valueOf(pw));
        transcoder.addTranscodingHint(ImageTranscoder.KEY_HEIGHT, (Object)Float.valueOf(ph));
        TranscoderInput input = new TranscoderInput((InputStream)new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)));
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(Math.max(16384, pw * ph / 2));
        transcoder.transcode(input, new TranscoderOutput((OutputStream)buffer));
        byte[] pngBytes = buffer.toByteArray();
        if (!AuthCaptchaService.isValidPngSignature((byte[])pngBytes)) {
            throw new IOException("PNG rasterizer produced invalid output (length=" + pngBytes.length + ")");
        }
        Files.write(outFile, pngBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static boolean isValidPngSignature(byte[] data) {
        if (data == null || data.length < 8) {
            return false;
        }
        return (data[0] & 0xFF) == 137 && data[1] == 80 && data[2] == 78 && data[3] == 71 && data[4] == 13 && data[5] == 10 && data[6] == 26 && data[7] == 10;
    }

    private static String expandImageCaptchaSvgForDebugUserLine(String svg) {
        int open = svg.indexOf("<svg");
        if (open < 0) {
            return svg;
        }
        int close = svg.indexOf(62, open);
        if (close < 0) {
            return svg;
        }
        int hNew = 70;
        String head = svg.substring(open, close + 1);
        String newHead = head.replaceFirst("height='54'", "height='" + hNew + "'").replaceFirst("height=\"54\"", "height=\"" + hNew + "\"").replace("viewBox='0 0 160 54'", "viewBox='0 0 160 " + hNew + "'").replace("viewBox=\"0 0 160 54\"", "viewBox=\"0 0 160 " + hNew + "\"");
        return svg.substring(0, open) + newHead + svg.substring(close + 1);
    }

    private static String escapeXmlTextForSvg(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length() + 8);
        block5: for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            switch (c) {
                case '&': {
                    sb.append("&amp;");
                    continue block5;
                }
                case '<': {
                    sb.append("&lt;");
                    continue block5;
                }
                case '>': {
                    sb.append("&gt;");
                    continue block5;
                }
                default: {
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    private static String augmentSvgForCaptchaDebug(CaptchaEntry entry, List<int[]> userClicks, String imageCaptchaUserInput) {
        String svg = entry.svg;
        if (svg == null || !svg.contains("</svg>")) {
            return svg;
        }
        boolean wantImageUserLine = "input".equals(entry.type) && imageCaptchaUserInput != null && !imageCaptchaUserInput.isBlank();
        String working = wantImageUserLine ? AuthCaptchaService.expandImageCaptchaSvgForDebugUserLine((String)svg) : svg;
        StringBuilder overlays = new StringBuilder();
        if ("drag".equals(entry.type) && entry.dragTargetX() != null) {
            int tx = entry.dragTargetX();
            int ty = entry.dragHoleY() != null ? entry.dragHoleY() : 6;
            overlays.append("<g id='captcha-debug-drag-target' aria-label='drag-slot'>").append("<rect x='").append(tx).append("' y='").append(ty).append("' width='").append(36).append("' height='").append(36).append("' fill='none' stroke='#16a34a' stroke-width='1.5' stroke-dasharray='4 3' opacity='0.95'/>").append("</g>");
        }
        if ("click".equals(entry.type) && entry.clickSequence() != null) {
            overlays.append("<g id='captcha-debug-target' aria-label='correct-hit-regions'>");
            for (int[] hr : entry.clickSequence()) {
                overlays.append("<circle cx='").append(hr[0]).append("' cy='").append(hr[1]).append("' r='").append(hr[2]).append("' fill='none' stroke='#16a34a' stroke-width='1.5' stroke-dasharray='4 3' opacity='0.95'/>").append("<circle cx='").append(hr[0]).append("' cy='").append(hr[1]).append("' r='2' fill='#16a34a'/>");
            }
            overlays.append("</g>");
        }
        if (userClicks != null && "click".equals(entry.type)) {
            int step = 0;
            for (int[] p : userClicks) {
                int ux = p[0];
                int uy = p[1];
                overlays.append("<g id='captcha-debug-user-click-").append(++step).append("' aria-label='user-click-").append(step).append("'>").append("<line x1='").append(ux - 10).append("' y1='").append(uy).append("' x2='").append(ux + 10).append("' y2='").append(uy).append("' stroke='#dc2626' stroke-width='2' stroke-linecap='round'/>").append("<line x1='").append(ux).append("' y1='").append(uy - 10).append("' x2='").append(ux).append("' y2='").append(uy + 10).append("' stroke='#dc2626' stroke-width='2' stroke-linecap='round'/>").append("<circle cx='").append(ux).append("' cy='").append(uy).append("' r='5' fill='none' stroke='#dc2626' stroke-width='2'/>").append("</g>");
            }
        }
        StringBuilder tail = new StringBuilder();
        if (!overlays.isEmpty()) {
            tail.append((CharSequence)overlays);
        }
        if (wantImageUserLine) {
            Object shown = imageCaptchaUserInput.trim();
            if (((String)shown).length() > 120) {
                shown = ((String)shown).substring(0, 120) + "...";
            }
            int textY = 68;
            tail.append("<text x='6' y='").append(textY).append("' font-size='11' font-family='Arial, sans-serif' fill='#374151'>\u7528\u6237\u8f93\u5165\uff1a").append(AuthCaptchaService.escapeXmlTextForSvg((String)shown)).append("</text>");
        }
        if (tail.isEmpty()) {
            return svg;
        }
        int i = working.lastIndexOf("</svg>");
        return working.substring(0, i) + String.valueOf(tail) + working.substring(i);
    }

    private static String stripClickCaptchaPromptPrefix(String s) {
        if (s == null || s.isBlank()) {
            return "";
        }
        String t = s.trim();
        if (t.startsWith("\u8bf7\u70b9\u51fb\uff1a")) {
            return t.substring("\u8bf7\u70b9\u51fb\uff1a".length()).trim();
        }
        if (t.startsWith("\u8bf7\u70b9\u51fb:")) {
            return t.substring("\u8bf7\u70b9\u51fb:".length()).trim();
        }
        return t;
    }

    private static String sanitizeFileName(String s) {
        if (s == null || s.isBlank()) {
            return "";
        }
        return s.replaceAll("[\\\\/:*?\"<>|]", "_").replaceAll("\\s+", "_");
    }

    @Generated
    public AuthCaptchaService(SysConfigService sysConfigService, Environment environment) {
        this.sysConfigService = sysConfigService;
        this.environment = environment;
    }
}

