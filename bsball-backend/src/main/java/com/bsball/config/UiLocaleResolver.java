package com.bsball.config;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/**
 * 请求语言判定：先看 {@code X-UI-Locale}，没有再退回父类的 {@code Accept-Language}。
 *
 * <p>为什么不能只看 Accept-Language：后台界面的语言是存在浏览器 localStorage
 * （{@code bs-ball-locale}）里、由使用者自己切的，与系统语言可以完全不一致；而 fetch/XHR 的
 * {@code Accept-Language} 是 forbidden header，前端脚本改不动它，浏览器只会发系统语言。
 * 只靠标准头就会出现「界面是英文、报错文案是中文」（反之亦然）。所以前端在 buildHeaders 里
 * 统一带上自己当前的 locale。
 *
 * <p>ko/ja 映射成英文不是偷懒，是跟前端取词保持一致：{@code useLiveGameText} 里非 {@code zh*}
 * 的语言一律取 en 词典（ja/ko 的 liveGame 块是空的）。后端若给 ja 请求回退到中文兜底，
 * 就成了「日文界面里夹一句简体中文」。
 *
 * <p><b>这层映射只覆盖 {@code X-UI-Locale}，不覆盖 {@code Accept-Language}</b>（实测所得，不是
 * 设计意图）：退到父类时 ja/ko/de 这些 locale 原样交给 MessageSource，而词典只有简中（默认那份）、
 * 繁中、英三份，无匹配就落到默认那份的简体中文。实际打不到浏览器使用者——{@code i18n/index.js} 的
 * {@code detectLocale()} 恒返回非空（localStorage 没值时按系统语言探测），所以 {@code request.js}
 * 总会带上这个头；还会命中的只剩 curl 一类没有「界面语言」概念的调用方，那种场合回中文兜底可以接受。
 * 真要把两条路径统一成英文，该改的是下面 {@link #resolveLocale} 里对父类结果的归并，不是补词典；
 * 现状由 {@code SnapshotGuardMessageKeyTest} 那条边界断言钉着，口径一改测试先红。
 *
 * <p>认不出来的值不报错、直接交给父类：这是新增的可选头，老客户端没带也要照常工作。
 */
public class UiLocaleResolver
extends AcceptHeaderLocaleResolver {
    public static final String HEADER = "X-UI-Locale";

    public Locale resolveLocale(HttpServletRequest request) {
        Locale fromHeader = parseUiLocale(request.getHeader(HEADER));
        return fromHeader != null ? fromHeader : super.resolveLocale(request);
    }

    static Locale parseUiLocale(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String tag = raw.trim().toLowerCase().replace('_', '-');
        if (tag.equals("en") || tag.startsWith("en-") || tag.equals("ko") || tag.startsWith("ko-")
                || tag.equals("ja") || tag.startsWith("ja-")) {
            return Locale.ENGLISH;
        }
        if (tag.startsWith("zh")) {
            return tag.contains("tw") || tag.contains("hk") || tag.contains("hant")
                    ? Locale.TRADITIONAL_CHINESE : Locale.SIMPLIFIED_CHINESE;
        }
        return null;
    }
}
