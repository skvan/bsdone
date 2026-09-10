/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.core.BootstrapActiveProfile
 *  org.yaml.snakeyaml.Yaml
 */
package com.bsball.core;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.yaml.snakeyaml.Yaml;

/*
 * Exception performing whole class analysis ignored.
 */
public final class BootstrapActiveProfile {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}:]+)(?::([^}]*))?\\}");

    private BootstrapActiveProfile() {
    }

    public static String resolve(String[] mainArgs) {
        String profiles = System.getenv("SPRING_PROFILES_ACTIVE");
        if (profiles == null || profiles.isBlank()) {
            profiles = System.getProperty("spring.profiles.active", "");
        }
        if (profiles.isBlank() && mainArgs != null) {
            for (String arg : mainArgs) {
                if (arg == null || !arg.startsWith("--spring.profiles.active=")) continue;
                profiles = arg.substring("--spring.profiles.active=".length()).trim();
                break;
            }
        }
        if (profiles != null && !profiles.isBlank()) {
            return profiles.split(",")[0].trim();
        }
        String fromMainYaml = BootstrapActiveProfile.readDefaultActiveProfileFromApplicationYml();
        if (fromMainYaml != null && !fromMainYaml.isBlank()) {
            return fromMainYaml.split(",")[0].trim();
        }
        return "dev";
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private static String readDefaultActiveProfileFromApplicationYml() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = BootstrapActiveProfile.class.getClassLoader();
        }
        try (InputStream in = cl.getResourceAsStream("application.yml");){
            Object active;
            String s;
            Object prof;
            Map doc;
            Object spring;
            Object raw;
            if (in == null) {
                String string = null;
                return string;
            }
            Iterable docs = new Yaml().loadAll(in);
            if (docs == null) {
                String string = null;
                return string;
            }
            Iterator iterator = docs.iterator();
            do {
                if (!iterator.hasNext()) return null;
            } while (!((raw = iterator.next()) instanceof Map) || !((spring = (doc = (Map)raw).get("spring")) instanceof Map) || !((prof = ((Map)spring).get("profiles")) instanceof Map) || (s = BootstrapActiveProfile.str(active = ((Map)prof).get("active"))) == null);
            String string = BootstrapActiveProfile.resolvePlaceholders((String)s);
            return string;
        }
        catch (Exception ignored) {
            return null;
        }
    }

    private static String str(Object o) {
        if (o == null) {
            return null;
        }
        String s = o.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private static String resolvePlaceholders(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        Matcher matcher = PLACEHOLDER.matcher(value);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String def = matcher.group(2) != null ? matcher.group(2) : "";
            String v = System.getenv(key);
            if (v == null || v.isBlank()) {
                v = System.getProperty(key);
            }
            String replacement = v != null && !v.isBlank() ? v : def;
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}

