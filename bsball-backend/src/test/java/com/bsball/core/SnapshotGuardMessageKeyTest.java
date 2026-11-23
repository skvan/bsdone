package com.bsball.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.bsball.common.Result;
import com.bsball.config.UiLocaleResolver;
import com.bsball.config.WebConfig;
import com.bsball.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.LocaleResolver;

/**
 * 快照守门那几条 409 的文案本地化：请求语言怎么定、解不出时退到哪、以及「改了这条会不会连带
 * 影响库里另外几百处 {@code new BusinessException}」。
 *
 * <p>分工（两file各管一半，别在这儿找 service 的影子）：
 * 「异常带的键与参数对不对、Java 兜底原文与 messages.properties 是否逐字一致」归
 * {@code GameServiceLiveSnapshotGuardTest}——那里能让四种拒收真的发生。本文件管的是传输那一跳：
 * 同一个键＋参数，经 DispatcherServlet → {@link UiLocaleResolver} → {@link GlobalExceptionHandler}
 * 之后，回到响应体 {@code msg} 上的到底是哪一种语言。
 *
 * <p>用 standaloneSetup 不起 Spring 上下文：本仓库所有测试都不起上下文（跑全量时上下文启动要连
 * PostgreSQL），而这里要验的只是 MVC 这一段。MessageSource 按 Boot 的
 * {@code MessageSourceAutoConfiguration} 那套装，LocaleResolver 直接取 {@link WebConfig} 那个
 * bean 方法——都用生产同一份装配，免得测试自己配得好好的、线上是另一个样子。
 */
@DisplayName("快照守门 409 文案：按请求语言解词，解不出退回原文")
class SnapshotGuardMessageKeyTest {

    private static final String WIPED_KEY = "snapshot.guard.events.wiped";
    /** messages_en.properties 里的招牌词：出现即说明响应真的是英文。 */
    private static final String EN_MARK = "Restore Session from Cloud";
    private static final Locale DEFAULT_LOCALE = Locale.getDefault();

    private static ResourceBundleMessageSource bundle;
    private MockMvc mvc;

    @BeforeAll
    static void assembleBundleLikeBootDoes() {
        bundle = new ResourceBundleMessageSource();
        bundle.setBasenames("messages");
        bundle.setDefaultEncoding(StandardCharsets.UTF_8.name());
        bundle.setFallbackToSystemLocale(false);
    }

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new GuardProbeController())
                .setControllerAdvice(new GlobalExceptionHandler(bundle))
                .setLocaleResolver(localeResolverLikeWebConfig())
                .build();
    }

    @AfterEach
    void restoreDefaultLocale() {
        Locale.setDefault(DEFAULT_LOCALE);
    }

    private static LocaleResolver localeResolverLikeWebConfig() {
        // 走生产的 bean 方法本身，不在测试里重设默认语言：默认值改了就该被这些测试感到。
        LocaleResolver resolver = new WebConfig(null).localeResolver();
        assertNotNull(resolver);
        return resolver;
    }

    private String extractMsg(MvcResult res) throws Exception {
        String body = res.getResponse().getContentAsString(StandardCharsets.UTF_8);
        Result<?> parsed = new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(body, Result.class);
        assertEquals(200, res.getResponse().getStatus(),
                "409 是业务码不是 HTTP 状态（request.js 靠 body.code 判）：" + body);
        assertEquals(409, parsed.getCode(), body);
        return parsed.getMsg();
    }

    private MvcResult call(String probe, String uiLocale, String acceptLanguage) throws Exception {
        var req = post("/_t/guard").param("probe", probe);
        if (uiLocale != null) {
            req = req.header(UiLocaleResolver.HEADER, uiLocale);
        }
        if (acceptLanguage != null) {
            req = req.header("Accept-Language", acceptLanguage);
        }
        return mvc.perform(req).andReturn();
    }

    private static boolean hasCjk(String s) {
        return s.codePoints().anyMatch(c -> c >= 0x4E00 && c <= 0x9FFF);
    }

    @Test
    @DisplayName("X-UI-Locale: en 时响应给英文，正文里没有汉字")
    void englishUiLocaleGetsEnglishMessage() throws Exception {
        String msg = extractMsg(call("wiped", "en", null));

        assertTrue(msg.contains(EN_MARK), "英文条目没解出来：" + msg);
        assertTrue(msg.contains("24"), "条数参数没插进去：" + msg);
        assertFalse(hasCjk(msg), "英文响应里夹了汉字：" + msg);
    }

    @Test
    @DisplayName("X-UI-Locale: zh-TW 给繁体、zh-CN 给简体，两者不能互换")
    void traditionalAndSimplifiedAreDistinct() throws Exception {
        String tw = extractMsg(call("wiped", "zh-TW", null));
        String cn = extractMsg(call("wiped", "zh-CN", null));

        // 只给一句繁体招牌词就够：兜底原文是简体，出现「已拒絕覆蓋」就说明真的解到了
        // messages_zh_TW，不是退回原文（不再拿句子里其余的用词做反向断言，那些会随措辞改版而假红）。
        assertTrue(tw.contains("已拒絕覆蓋"), "繁体条目没解出来：" + tw);
        assertFalse(tw.contains("拒绝"), "繁体里出现简体「拒绝」，说明落到简体兜底了：" + tw);
        assertTrue(cn.contains("已拒绝覆盖"), "简体条目没解出来：" + cn);
        assertFalse(cn.contains("拒絕"), "简体里出现繁体：" + cn);
        assertNotEquals(tw, cn, "繁简两句一字不差，至少有一份不对");
    }

    @Test
    @DisplayName("没带 X-UI-Locale 时退回 Accept-Language（老客户端照常工作）")
    void fallsBackToAcceptLanguageWhenHeaderMissing() throws Exception {
        String msg = extractMsg(call("wiped", null, "en-US,en;q=0.9"));

        assertTrue(msg.contains(EN_MARK), "Accept-Language 没起作用：" + msg);
    }

    @Test
    @DisplayName("两个头都没有时用简体中文，不是 JVM 默认语言")
    void missingBothHeadersUsesDeclaredDefaultNotJvmLocale() throws Exception {
        // 把 JVM 默认语言改成英文再发：若 WebConfig 里那句 setDefaultLocale 被删掉，
        // AcceptHeaderLocaleResolver 会拿 Locale.getDefault()，这句就红。
        // 这也顺带钉住「同一句报错不随部署机器语言变」。
        Locale.setDefault(Locale.ENGLISH);
        try {
            String msg = extractMsg(call("wiped", null, null));
            assertTrue(msg.contains("已拒绝覆盖"), "默认语言不是简体：" + msg);
        } finally {
            Locale.setDefault(DEFAULT_LOCALE);
        }
    }

    @Test
    @DisplayName("ja/ko 界面取英文：跟前端 useLiveGameText 的取词口径一致")
    void japaneseAndKoreanUiGetEnglish() throws Exception {
        for (String tag : new String[] {"ja", "ko-KR"}) {
            String msg = extractMsg(call("wiped", tag, null));
            assertTrue(msg.contains(EN_MARK), tag + " 应该拿到英文（前端那门也没有词典）：" + msg);
        }
    }

    @Test
    @DisplayName("X-UI-Locale 是认不出的值时交给 Accept-Language，不报错也不硬翻")
    void unknownUiLocaleDelegatesToAcceptLanguage() throws Exception {
        String msg = extractMsg(call("wiped", "fr", "zh-TW"));

        assertTrue(msg.contains("已拒絕覆蓋"), "认不出的值不该抢在 Accept-Language 前面：" + msg);
    }

    @Test
    @DisplayName("键在词典里不存在时退回异常自带原文，不给空串也不给键名")
    void missingBundleEntryFallsBackToOriginalText() throws Exception {
        String msg = extractMsg(call("noSuchKey", "en", null));

        assertEquals("这条键故意不存在时的兜底原文 24", msg, "缺键时要把原文说完整：" + msg);
    }

    @Test
    @DisplayName("没带键的两参 BusinessException 原文照发：全库另外几百处不受影响")
    void legacyTwoArgExceptionIsUntouched() throws Exception {
        // 这条是「改动范围」的护栏。实测全库 600 处 new BusinessException 调用点里，本轮带键的
        // 只有 GameService 那 4 处，其余 596 处（590 个两参、6 个单参）都不带键；只要
        // handleBusiness 对 messageKey==null 走原路，它们的文案就一个字都不会变。哪天有人把
        // localize() 改成无条件过 MessageSource，红的第一条就是它。
        String msg = extractMsg(call("legacy", "en", null));

        assertEquals("赛事不存在或无权使用", msg, "两参异常的原文被改写了：" + msg);
    }

    @Test
    @DisplayName("条数参数不带千分位：MessageFormat 默认分组会让 1234 变 1,234")
    void countArgumentIsNotGroupedWithSeparators() throws Exception {
        // Java 兜底那边是字符串拼接，永远得到 "1234"；bundle 若被 MessageFormat 格式化成
        // "1,234"，两份措辞就不一致了（GameServiceLiveSnapshotGuardTest 的逐字比对用的是 24，
        // 位数不够，看不出这件事）。
        String en = extractMsg(call("bigCount", "en", null));
        String cn = extractMsg(call("bigCount", "zh-CN", null));

        assertTrue(en.contains("1234") && !en.contains("1,234"), "英文带上了千分位：" + en);
        assertTrue(cn.contains("1234") && !cn.contains("1,234"), "简体带上了千分位：" + cn);
    }

    @Test
    @DisplayName("每个键×三语都真过 MessageFormat：没一条被格式化弄坏或悄悄退原文")
    void everyBundleKeyRendersInEveryLocale() throws Exception {
        // 上面的语言选择例全拿 wiped 那一把键发，所以其余三条的 en/zh-TW 模板从没进过
        // MessageFormat：一个多余的花括号、一个没成对的撇号（MessageFormat 拿它当引用符，
        // 会把下一个字符吃掉），只有使用者真的撞上那一条拒收时才看得见。这条把词典里每个键
        // 都推过一遍真管线，比对基准是「占位符直接换数字」——一份不经过 MessageFormat 的期望。
        String[] args = {"24", "18"};
        for (String key : keysOf("messages")) {
            for (String locale : new String[] {"zh-CN", "zh-TW", "en"}) {
                String file = localeFileOf(locale);
                String msg = extractMsg(call("key:" + key, locale, null));
                assertEquals(expectedText(file, key, args), msg,
                        locale + " / " + key + " 解出来的不是词典那句话");
                assertTrue(msg.contains("24"), locale + " / " + key + " 没把条数插进去：" + msg);
            }
        }
    }

    @Test
    @DisplayName("三份词典的键集合必须一模一样（改一边要改另一边）")
    void allBundlesCarryTheSameKeySet() throws Exception {
        Set<String> base = keysOf("messages");
        Set<String> tw = keysOf("messages_zh_TW");
        Set<String> en = keysOf("messages_en");

        assertFalse(base.isEmpty(), "messages.properties 一个键都没解出来（basename/编码装错了？）");
        assertEquals(base, tw, "繁简键集合不一致");
        assertEquals(base, en, "中英键集合不一致");
        assertTrue(base.contains(WIPED_KEY), "缺了守门在用的键：" + base);
    }

    @Test
    @DisplayName("application.yml 的 spring.messages 与本测试装 MessageSource 的方式一致")
    void applicationYmlMatchesHowTheBundleIsAssembled() throws Exception {
        // 本测试手装的 MessageSource 只在「和线上那份装法相同」时才有意义。这句比对不是重复啰嗦：
        // 它把这条前提变成断言，yml 一改（比如换 basename、关掉 UTF-8），这里立刻红。
        String yml = new String(new ClassPathResource("application.yml").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);

        assertTrue(yml.contains("basename: messages"), "yml 里 basename 不是 messages：" + yml);
        assertTrue(yml.contains("encoding: UTF-8"), "yml 里没声明 UTF-8 编码");
        assertTrue(yml.contains("fallback-to-system-locale: false"),
                "yml 没关掉 system locale 兜底，报错文案会随部署机器语言变");
    }

    @Test
    @DisplayName("词典里不留孤儿键：每一个都还有代码在用")
    void everyBundleKeyIsReferencedByTheGuard() throws Exception {
        // 变异验证定的职（不是凭推断）：把键在 service 与三份词典里同时改名，红的是
        // 上面那些语言选择的例（探针自己的键字面量在词典里查不到，只好退原文）。
        // 本条独有的那一格是反方向：词典里多出一个没人引用的键（新增忘接线、或删了某条
        // 拒收分支却留着文案）。那一格其他例子都不会动，只有这里会红。
        String guard = readSource("com/bsball/service/GameService.java");
        for (String key : keysOf("messages")) {
            assertTrue(guard.contains("\"" + key + "\""),
                    "词典键 " + key + " 在 GameService 里没被引用");
        }
    }

    /** 从模块根往上找源码：IDE 跑测试时的 cwd 不一定是模块根。 */
    private static String readSource(String suffix) throws Exception {
        java.nio.file.Path rel = java.nio.file.Path.of("src/main/java").resolve(suffix);
        for (java.nio.file.Path dir = java.nio.file.Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            java.nio.file.Path candidate = dir.resolve(rel);
            if (java.nio.file.Files.exists(candidate)) {
                return java.nio.file.Files.readString(candidate, StandardCharsets.UTF_8);
            }
        }
        throw new AssertionError("找不到源码 " + suffix);
    }

    /** 词典文件名按 locale 对位：跟 {@code ResourceBundleMessageSource} 的找法一致，没带区域的就是兜底那份。 */
    private static String localeFileOf(String locale) {
        if ("en".equals(locale)) {
            return "messages_en";
        }
        if ("zh-TW".equals(locale)) {
            return "messages_zh_TW";
        }
        return "messages";
    }

    /** 不经过 MessageFormat 的期望值：只把 {@code {N,number,#}} 换成第 N 个参数。 */
    private static String expectedText(String bundleFile, String key, String[] args) throws Exception {
        try (var in = new ClassPathResource(bundleFile + ".properties").getInputStream()) {
            String raw = new PropertyResourceBundle(in).getString(key);
            String out = raw;
            for (int i = 0; i < args.length; i++) {
                out = out.replace("{" + i + ",number,#}", args[i]);
            }
            assertFalse(out.contains("{") || out.contains("}"),
                    "本探针只认 {N,number,#} 这一种占位符写法，" + bundleFile + "/" + key + " 用了别的：" + raw);
            return out;
        }
    }

    private static Set<String> keysOf(String bundleName) throws Exception {
        try (var in = new ClassPathResource(bundleName + ".properties").getInputStream()) {
            ResourceBundle rb = new PropertyResourceBundle(in);
            Set<String> keys = new TreeSet<>();
            for (String k : rb.keySet()) {
                keys.add(k);
            }
            return keys;
        } catch (MissingResourceException ex) {
            throw new AssertionError("词典文件不在 classpath 上：" + bundleName, ex);
        }
    }

    /**
     * 探针控制器：只负责把守门真正会抛的那几种异常发进 MVC 管线。
     * 键与参数照 GameService 的用法给，「配对是否属实」由 service 那侧的测试钉，这里不重复。
     */
    @RestController
    static class GuardProbeController {

        @PostMapping("/_t/guard")
        public String probe(@RequestParam("probe") String probe) {
            switch (probe) {
                case "wiped":
                    throw new BusinessException(409, "兜底原文 24", WIPED_KEY, Integer.valueOf(24));
                case "bigCount":
                    throw new BusinessException(409, "兜底原文 1234", WIPED_KEY, Integer.valueOf(1234));
                case "noSuchKey":
                    throw new BusinessException(409, "这条键故意不存在时的兜底原文 24",
                            "snapshot.guard.no.such.key", Integer.valueOf(24));
                case "legacy":
                    throw new BusinessException(409, "赛事不存在或无权使用");
                default:
                    if (probe.startsWith("key:")) {
                        // 参数一律给两个：现有模板最多用到 {1}，多出来的参数 MessageFormat 会忽略
                        throw new BusinessException(409, "兜底原文 24", probe.substring(4),
                                Integer.valueOf(24), Integer.valueOf(18));
                    }
                    throw new IllegalStateException("unknown probe: " + probe);
            }
        }
    }
}
