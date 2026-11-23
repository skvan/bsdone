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
