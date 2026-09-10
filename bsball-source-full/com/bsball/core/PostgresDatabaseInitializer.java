/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.core.BootstrapActiveProfile
 *  com.bsball.core.PostgresDatabaseInitializer
 *  org.yaml.snakeyaml.Yaml
 */
package com.bsball.core;

import com.bsball.core.BootstrapActiveProfile;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.yaml.snakeyaml.Yaml;

/*
 * Exception performing whole class analysis ignored.
 */
public final class PostgresDatabaseInitializer {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}:]+)(?::([^}]*))?\\}");
    private static final Pattern JDBC_PG = Pattern.compile("jdbc:postgresql://([^:/]+)(?::(\\d+))?/([^?&#]+)");

    private PostgresDatabaseInitializer() {
    }

    public static void runBeforeSpring(String[] mainArgs) {
        String password;
        String user;
        String port;
        String host;
        String dbName;
        block68: {
            Map datasource;
            String urlRaw;
            Object ds;
            Object spring;
            String profile = BootstrapActiveProfile.resolve((String[])mainArgs);
            Map yaml = PostgresDatabaseInitializer.loadYaml((String)("application-" + profile + ".yml"));
            if (yaml == null || !PostgresDatabaseInitializer.hasPostgresDatasource((Map)yaml)) {
                return;
            }
            String yamlDbName = null;
            String yamlHost = "localhost";
            String yamlPort = "5432";
            String yamlUser = "postgres";
            String yamlPassword = "postgres";
            if (yaml != null && (spring = yaml.get("spring")) instanceof Map && (ds = ((Map)spring).get("datasource")) instanceof Map && (urlRaw = PostgresDatabaseInitializer.str((datasource = (Map)ds).get("url"))) != null && urlRaw.contains("postgresql")) {
                String p;
                String u;
                String url = PostgresDatabaseInitializer.resolvePlaceholders((String)urlRaw);
                Matcher m = JDBC_PG.matcher(url);
                if (m.find()) {
                    yamlHost = m.group(1);
                    yamlPort = m.group(2) != null ? m.group(2) : "5432";
                    yamlDbName = m.group(3).trim();
                    if (yamlDbName.isEmpty()) {
                        yamlDbName = "bsball";
                    }
                }
                if ((u = PostgresDatabaseInitializer.resolvePlaceholders((String)PostgresDatabaseInitializer.str(datasource.get("username")))) != null && !u.isBlank()) {
                    yamlUser = u;
                }
                if ((p = PostgresDatabaseInitializer.resolvePlaceholders((String)PostgresDatabaseInitializer.str(datasource.get("password")))) != null) {
                    yamlPassword = p;
                }
            }
            if (yamlDbName == null || yamlDbName.isBlank()) {
                yamlDbName = "bsball";
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
            if (host == null || host.isBlank()) {
                host = "localhost";
            }
            if ((port = System.getenv("DB_PORT")) == null || port.isBlank()) {
                port = System.getProperty("DB_PORT");
            }
            if (port == null || port.isBlank()) {
                port = yamlPort;
            }
            if (port == null || port.isBlank()) {
                port = "5432";
            }
            if ((user = System.getenv("DB_USER")) == null || user.isBlank()) {
                user = System.getProperty("DB_USER");
            }
            if (user == null || user.isBlank()) {
                user = yamlUser;
            }
            if (user == null || user.isBlank()) {
                user = "postgres";
            }
            if ((password = System.getenv("DB_PASSWORD")) == null || password.isBlank()) {
                password = System.getProperty("DB_PASSWORD");
            }
            if (password == null || password.isBlank()) {
                password = yamlPassword;
            }
            if (password == null) {
                password = "postgres";
            }
            String adminUrl = String.format("jdbc:postgresql://%s:%s/postgres", host, port);
            try {
                Class.forName("org.postgresql.Driver");
            }
            catch (ClassNotFoundException e) {
                return;
            }
            System.out.println("[bs-ball] \u6b63\u5728\u68c0\u67e5/\u521b\u5efa PostgreSQL \u6570\u636e\u5e93 " + dbName + " ...");
            try (Connection conn = DriverManager.getConnection(adminUrl, user, password);
                 PreparedStatement check = conn.prepareStatement("SELECT 1 FROM pg_database WHERE datname = ?");){
                check.setString(1, dbName);
                try (ResultSet rs = check.executeQuery();){
                    if (!rs.next()) {
                        try (Statement create = conn.createStatement();){
                            create.executeUpdate("CREATE DATABASE \"" + dbName.replace("\"", "\"\"") + "\"");
                            System.out.println("[bs-ball] \u5df2\u81ea\u52a8\u521b\u5efa PostgreSQL \u6570\u636e\u5e93: " + dbName);
                            break block68;
                        }
                    }
                    System.out.println("[bs-ball] \u6570\u636e\u5e93 " + dbName + " \u5df2\u5b58\u5728\uff0c\u8df3\u8fc7\u521b\u5efa");
                }
            }
            catch (Exception e) {
                System.err.println("[bs-ball] \u81ea\u52a8\u521b\u5efa\u6570\u636e\u5e93\u5931\u8d25: " + e.getMessage());
                System.err.println("[bs-ball] \u8bf7\u786e\u4fdd PostgreSQL \u5df2\u542f\u52a8\uff0c\u4e14 DB_USER/DB_PASSWORD \u6216 jar \u5185 application-*.yml \u4e2d\u7684\u8d26\u53f7\u80fd\u8fde\u63a5 postgres \u5e93\uff1b\u6216\u624b\u52a8\u6267\u884c: CREATE DATABASE " + dbName + ";");
                throw new IllegalStateException("PostgreSQL \u6570\u636e\u5e93 " + dbName + " \u4e0d\u5b58\u5728\u4e14\u81ea\u52a8\u521b\u5efa\u5931\u8d25\uff0c\u8bf7\u5148\u521b\u5efa\u6570\u636e\u5e93\u6216\u68c0\u67e5\u8fde\u63a5\u914d\u7f6e", e);
            }
        }
        PostgresDatabaseInitializer.ensurePostgisExtension((String)host, (String)port, (String)dbName, (String)user, (String)password);
        String defaultSchema = System.getenv("DEFAULT_SCHEMA");
        if (defaultSchema == null || defaultSchema.isBlank()) {
            defaultSchema = System.getProperty("DEFAULT_SCHEMA", "public");
        }
        if (defaultSchema == null || defaultSchema.isBlank() || "public".equalsIgnoreCase(defaultSchema.trim())) {
            return;
        }
        String schema = defaultSchema.trim();
        String targetUrl = String.format("jdbc:postgresql://%s:%s/%s", host, port, dbName);
        try (Connection conn = DriverManager.getConnection(targetUrl, user, password);
             Statement st = conn.createStatement();){
            String quoted = "\"" + schema.replace("\"", "\"\"") + "\"";
            st.executeUpdate("CREATE SCHEMA IF NOT EXISTS " + quoted);
            System.out.println("[bs-ball] \u5df2\u786e\u4fdd schema " + schema + " \u5b58\u5728");
        }
        catch (Exception e) {
            System.err.println("[bs-ball] \u521b\u5efa schema \u5931\u8d25: " + e.getMessage());
        }
    }

    private static void ensurePostgisExtension(String host, String port, String dbName, String user, String password) {
        String url = String.format("jdbc:postgresql://%s:%s/%s", host, port, dbName);
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement st = conn.createStatement();){
            st.executeUpdate("CREATE EXTENSION IF NOT EXISTS postgis");
            System.out.println("[bs-ball] \u5df2\u786e\u4fdd\u6570\u636e\u5e93 " + dbName + " \u542f\u7528\u6269\u5c55 postgis");
        }
        catch (Exception e) {
            throw new IllegalStateException("[bs-ball] \u65e0\u6cd5\u6267\u884c CREATE EXTENSION postgis\u3002\u8bf7\u4f7f\u7528\u5e26 PostGIS \u7684 PostgreSQL\uff08\u4f8b\u5982 Docker\uff1apostgis/postgis\uff09\uff0c\u5e76\u786e\u4fdd\u8fde\u63a5\u7528\u6237\u6709\u521b\u5efa\u6269\u5c55\u6743\u9650\uff1b\u6216\u767b\u5f55\u6570\u636e\u5e93\u624b\u52a8\u6267\u884c: CREATE EXTENSION postgis; \u539f\u6587: " + e.getMessage(), e);
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
            cl = PostgresDatabaseInitializer.class.getClassLoader();
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
                if (!PostgresDatabaseInitializer.hasPostgresDatasource((Map)doc)) continue;
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

    private static boolean hasPostgresDatasource(Map<String, Object> yaml) {
        Object spring = yaml.get("spring");
        if (!(spring instanceof Map)) {
            return false;
        }
        Object ds = ((Map)spring).get("datasource");
        if (!(ds instanceof Map)) {
            return false;
        }
        String url = PostgresDatabaseInitializer.str(((Map)ds).get("url"));
        return url != null && url.contains("postgresql");
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

