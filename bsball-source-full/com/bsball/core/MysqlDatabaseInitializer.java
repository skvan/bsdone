/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.core.BootstrapActiveProfile
 *  com.bsball.core.MysqlDatabaseInitializer
 *  org.yaml.snakeyaml.Yaml
 */
package com.bsball.core;

import com.bsball.core.BootstrapActiveProfile;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.yaml.snakeyaml.Yaml;

/*
 * Exception performing whole class analysis ignored.
 */
public final class MysqlDatabaseInitializer {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}:]+)(?::([^}]*))?\\}");
    private static final Pattern JDBC_MYSQL = Pattern.compile("jdbc:mysql://([^:/]+)(?::(\\d+))?/([^?&#]*)");

    private MysqlDatabaseInitializer() {
    }

    public static void runBeforeSpring(String[] mainArgs) {
        String password;
        String user;
        String port;
        String host;
        String dbName;
        Map datasource;
        String urlRaw;
        Object ds;
        String profile = BootstrapActiveProfile.resolve((String[])mainArgs);
        Map yaml = MysqlDatabaseInitializer.loadYaml((String)("application-" + profile + ".yml"));
        if (yaml == null || !MysqlDatabaseInitializer.hasMysqlDatasource((Map)yaml)) {
            return;
        }
        String yamlDbName = "bsball";
        String yamlHost = "localhost";
        String yamlPort = "3306";
        String yamlUser = "root";
        String yamlPassword = "";
        Object spring = yaml.get("spring");
        if (spring instanceof Map && (ds = ((Map)spring).get("datasource")) instanceof Map && (urlRaw = MysqlDatabaseInitializer.str((datasource = (Map)ds).get("url"))) != null && urlRaw.contains("mysql")) {
            String p;
            String u;
            String url = MysqlDatabaseInitializer.resolvePlaceholders((String)urlRaw);
            Matcher m = JDBC_MYSQL.matcher(url);
            if (m.find()) {
                yamlHost = m.group(1);
                yamlPort = m.group(2) != null ? m.group(2) : "3306";
                String path = m.group(3).trim();
                if (!path.isEmpty()) {
                    yamlDbName = path;
                }
            }
            if ((u = MysqlDatabaseInitializer.resolvePlaceholders((String)MysqlDatabaseInitializer.str(datasource.get("username")))) != null && !u.isBlank()) {
                yamlUser = u;
            }
            if ((p = MysqlDatabaseInitializer.resolvePlaceholders((String)MysqlDatabaseInitializer.str(datasource.get("password")))) != null) {
                yamlPassword = p;
            }
        }
        if ((dbName = System.getenv("DB_NAME")) == null || dbName.isBlank()) {
            dbName = System.getProperty("DB_NAME");
        }
        if (dbName == null || dbName.isBlank()) {
            dbName = yamlDbName;
        }
        if (dbName == null || dbName.isBlank()) {
            dbName = "bsball";
        }
        if (!(dbName = dbName.trim()).matches("^[a-zA-Z0-9_]+$")) {
            dbName = "bsball";
        }
        if ((host = System.getenv("DB_HOST")) == null || host.isBlank()) {
            host = System.getProperty("DB_HOST");
        }
        if (host == null || host.isBlank()) {
            host = yamlHost;
        }
        if ((port = System.getenv("DB_PORT")) == null || port.isBlank()) {
            port = System.getProperty("DB_PORT");
        }
        if (port == null || port.isBlank()) {
            port = yamlPort;
        }
        if ((user = System.getenv("DB_USER")) == null || user.isBlank()) {
            user = System.getProperty("DB_USER");
        }
        if (user == null || user.isBlank()) {
            user = yamlUser;
        }
        if ((password = System.getenv("DB_PASSWORD")) == null || password.isBlank()) {
            password = System.getProperty("DB_PASSWORD");
        }
        if (password == null || password.isBlank()) {
            password = yamlPassword;
        }
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        }
        catch (ClassNotFoundException e) {
            return;
        }
        String noDbUrl = String.format("jdbc:mysql://%s:%s/?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true", host, port);
        System.out.println("[bs-ball] \u6b63\u5728\u68c0\u67e5/\u521b\u5efa MySQL \u6570\u636e\u5e93 " + dbName + " ...");
        try (Connection conn = DriverManager.getConnection(noDbUrl, user, password);
             Statement st = conn.createStatement();){
            String safeDb = "`" + dbName.replace("`", "``") + "`";
            st.executeUpdate("CREATE DATABASE IF NOT EXISTS " + safeDb + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            System.out.println("[bs-ball] \u5df2\u786e\u4fdd MySQL \u6570\u636e\u5e93\u5b58\u5728: " + dbName);
        }
        catch (Exception e) {
            System.err.println("[bs-ball] MySQL \u81ea\u52a8\u5efa\u5e93\u5931\u8d25: " + e.getMessage());
            System.err.println("[bs-ball] \u8bf7\u786e\u4fdd MySQL \u5df2\u542f\u52a8\uff0c\u4e14 DB_USER/DB_PASSWORD \u6216 application-*.yml \u4e2d\u7684\u8d26\u53f7\u80fd\u8fde\u63a5\uff1b\u6216\u624b\u52a8\u6267\u884c: CREATE DATABASE " + dbName + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
            throw new IllegalStateException("MySQL \u6570\u636e\u5e93 " + dbName + " \u4e0d\u5b58\u5728\u4e14\u81ea\u52a8\u521b\u5efa\u5931\u8d25\uff0c\u8bf7\u5148\u521b\u5efa\u6570\u636e\u5e93\u6216\u68c0\u67e5\u8fde\u63a5\u914d\u7f6e", e);
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private static Map<String, Object> loadYaml(String resource) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = MysqlDatabaseInitializer.class.getClassLoader();
        }
        try (InputStream in = cl.getResourceAsStream(resource);){
            if (in == null) {
                Map<String, Object> map = null;
                return map;
            }
            Iterable docs = new Yaml().loadAll(in);
            if (docs == null) {
                Map<String, Object> map = null;
                return map;
            }
            Iterator it = docs.iterator();
            Map first = null;
            while (it.hasNext()) {
                Object raw = it.next();
                if (!(raw instanceof Map)) continue;
                Map doc = (Map)raw;
                if (first == null) {
                    first = doc;
                }
                if (!MysqlDatabaseInitializer.hasMysqlDatasource((Map)doc)) continue;
                Map map = doc;
                return map;
            }
            Map map = first;
            return map;
        }
        catch (Exception e) {
            return null;
        }
    }

    private static boolean hasMysqlDatasource(Map<String, Object> yaml) {
        Object spring = yaml.get("spring");
        if (!(spring instanceof Map)) {
            return false;
        }
        Object ds = ((Map)spring).get("datasource");
        if (!(ds instanceof Map)) {
            return false;
        }
        String url = MysqlDatabaseInitializer.str(((Map)ds).get("url"));
        return url != null && url.contains("mysql");
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

