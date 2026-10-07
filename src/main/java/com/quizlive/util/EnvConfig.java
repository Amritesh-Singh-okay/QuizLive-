package com.quizlive.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Environment and Configuration Manager for QuizLive.
 *
 * Automatically resolves configuration settings across development and deployment
 * environments according to the following precedence hierarchy:
 * 1. Java System Properties (-Dkey=value)
 * 2. Operating System / Cloud Environment Variables (System.getenv)
 * 3. Local .env file (if present in working directory or application root)
 * 4. db.properties fallback from classpath
 * 5. Built-in resilient defaults
 *
 * Also provides intelligent parsing for cloud database URIs (e.g. mysql://user:pass@host:port/db)
 * commonly injected by platforms like Railway, Render, Heroku, and Docker.
 */
public final class EnvConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvConfig.class);

    private static final Map<String, String> DOTENV_MAP;
    private static final Properties CLASSPATH_PROPERTIES;
    private static String parsedDbUserFromUrl = null;
    private static String parsedDbPassFromUrl = null;

    static {
        CLASSPATH_PROPERTIES = loadClasspathProperties();
        DOTENV_MAP = loadDotEnvFile();
    }

    private EnvConfig() {
        // Prevent instantiation
    }

    /**
     * Loads base classpath properties from db.properties if available.
     */
    private static Properties loadClasspathProperties() {
        Properties props = new Properties();
        try (InputStream in = EnvConfig.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            LOGGER.debug("No classpath db.properties loaded: {}", e.getMessage());
        }
        return props;
    }

    /**
     * Attempts to find and parse a .env file in the current working directory or user.dir.
     * If no .env file is found, returns an empty map (standard for production container/cloud environments).
     */
    private static Map<String, String> loadDotEnvFile() {
        File[] candidateFiles = new File[]{
                new File(".env"),
                new File(System.getProperty("user.dir", "."), ".env")
        };

        File envFile = null;
        for (File candidate : candidateFiles) {
            if (candidate.exists() && candidate.isFile() && candidate.canRead()) {
                envFile = candidate;
                break;
            }
        }

        if (envFile == null) {
            LOGGER.info("No .env file found on disk; relying on system environment variables and classpath defaults.");
            return Collections.emptyMap();
        }

        LOGGER.info("Loading local environment configuration from: {}", envFile.getAbsolutePath());
        Map<String, String> map = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(envFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Support 'export KEY=VALUE' syntax
                if (line.startsWith("export ") || line.startsWith("EXPORT ")) {
                    line = line.substring(7).trim();
                }

                int eqIdx = line.indexOf('=');
                if (eqIdx <= 0) {
                    continue;
                }

                String key = line.substring(0, eqIdx).trim();
                String value = line.substring(eqIdx + 1).trim();

                // Strip surrounding matching single or double quotes
                if ((value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) ||
                        (value.startsWith("'") && value.endsWith("'") && value.length() >= 2)) {
                    value = value.substring(1, value.length() - 1);
                }

                map.put(key, value);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to read .env file {}: {}", envFile.getAbsolutePath(), e.getMessage());
        }

        return Collections.unmodifiableMap(map);
    }

    /**
     * Resolves a configuration value by key, checking System Property, System Environment,
     * .env file, and classpath db.properties in order.
     */
    public static String get(String key, String defaultValue) {
        if (key == null) {
            return defaultValue;
        }

        // 1. System property (-Dkey=value)
        String val = System.getProperty(key);
        if (val != null && !val.trim().isEmpty()) {
            return val.trim();
        }

        // 2. OS / Cloud Environment Variable
        val = System.getenv(key);
        if (val != null && !val.trim().isEmpty()) {
            return val.trim();
        }

        // Try normalized uppercase key for environment (e.g. db.url -> DB_URL)
        String envKey = key.replace('.', '_').toUpperCase();
        val = System.getenv(envKey);
        if (val != null && !val.trim().isEmpty()) {
            return val.trim();
        }

        // 3. Local .env map
        val = DOTENV_MAP.get(key);
        if (val != null && !val.trim().isEmpty()) {
            return val.trim();
        }
        val = DOTENV_MAP.get(envKey);
        if (val != null && !val.trim().isEmpty()) {
            return val.trim();
        }

        // 4. Classpath properties
        val = CLASSPATH_PROPERTIES.getProperty(key);
        if (val != null && !val.trim().isEmpty()) {
            return val.trim();
        }

        return defaultValue;
    }

    public static String get(String key) {
        return get(key, null);
    }

    public static int getInt(String key, int defaultValue) {
        String val = get(key, null);
        if (val != null) {
            try {
                return Integer.parseInt(val.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    public static long getLong(String key, long defaultValue) {
        String val = get(key, null);
        if (val != null) {
            try {
                return Long.parseLong(val.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String val = get(key, null);
        if (val != null) {
            return "true".equalsIgnoreCase(val.trim()) || "1".equals(val.trim()) || "yes".equalsIgnoreCase(val.trim());
        }
        return defaultValue;
    }

    // =========================================================================
    // Database Specific Resolution
    // =========================================================================

    public static String getDbDriver() {
        return get("db.driver", "com.mysql.cj.jdbc.Driver");
    }

    /**
     * Resolves the JDBC URL. Handles:
     * - DB_URL / DATABASE_URL / MYSQL_URL
     * - RFC standard URIs (mysql://user:pass@host:port/database)
     * - Disaggregated parameters (DB_HOST, DB_PORT, DB_NAME, DB_SSL)
     * - Classpath db.url default
     */
    public static String getDbUrl() {
        // 1. Direct explicit DB_URL or db.url
        String directUrl = get("DB_URL", null);
        if (directUrl == null) {
            directUrl = get("JDBC_DATABASE_URL", null);
        }
        if (directUrl == null) {
            directUrl = get("DATABASE_URL", null);
        }
        if (directUrl == null) {
            directUrl = get("MYSQL_URL", null);
        }

        if (directUrl != null && !directUrl.trim().isEmpty()) {
            directUrl = directUrl.trim();
            if (directUrl.startsWith("jdbc:")) {
                return directUrl;
            }
            if (directUrl.startsWith("mysql://") || directUrl.startsWith("mysqls://")) {
                return convertUriToJdbc(directUrl);
            }
        }

        // 2. Check if individual connection components are specified in env
        String host = get("DB_HOST", get("MYSQLHOST", null));
        String portStr = get("DB_PORT", get("MYSQLPORT", "3306"));
        String dbName = get("DB_NAME", get("MYSQLDATABASE", null));

        if (host != null || dbName != null) {
            String resolvedHost = (host != null && !host.trim().isEmpty()) ? host.trim() : "localhost";
            String resolvedPort = (portStr != null && !portStr.trim().isEmpty()) ? portStr.trim() : "3306";
            String resolvedDb = (dbName != null && !dbName.trim().isEmpty()) ? dbName.trim() : "quizlive";
            boolean ssl = getBoolean("DB_SSL", false);

            return "jdbc:mysql://" + resolvedHost + ":" + resolvedPort + "/" + resolvedDb +
                    "?useSSL=" + ssl + "&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true";
        }

        // 3. Fallback to classpath db.properties or local default
        return get("db.url", "jdbc:mysql://localhost:3306/quizlive?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true");
    }

    /**
     * Converts a standard mysql://[user[:password]@]host[:port]/database[?query] URI to a JDBC URL.
     * Also extracts and caches username/password if present in user-info.
     */
    private static String convertUriToJdbc(String uriString) {
        try {
            boolean isSsl = uriString.startsWith("mysqls://");
            String clean = uriString.replaceFirst("^mysqls?://", "http://");
            URI uri = URI.create(clean);

            String host = uri.getHost() != null ? uri.getHost() : "localhost";
            int port = uri.getPort() != -1 ? uri.getPort() : 3306;
            String path = uri.getPath() != null && uri.getPath().length() > 1 ? uri.getPath().substring(1) : "quizlive";

            if (uri.getUserInfo() != null) {
                String[] userPass = uri.getUserInfo().split(":", 2);
                parsedDbUserFromUrl = userPass[0];
                if (userPass.length > 1) {
                    parsedDbPassFromUrl = userPass[1];
                }
            }

            StringBuilder jdbc = new StringBuilder("jdbc:mysql://");
            jdbc.append(host).append(":").append(port).append("/").append(path);
            jdbc.append("?useSSL=").append(isSsl);
            jdbc.append("&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true");

            if (uri.getQuery() != null && !uri.getQuery().trim().isEmpty()) {
                jdbc.append("&").append(uri.getQuery());
            }

            return jdbc.toString();
        } catch (Exception e) {
            LOGGER.warn("Failed to parse database URI '{}', returning as-is: {}", uriString, e.getMessage());
            return uriString;
        }
    }

    public static String getDbUser() {
        if (parsedDbUserFromUrl != null && !parsedDbUserFromUrl.trim().isEmpty()) {
            return parsedDbUserFromUrl;
        }
        String user = get("DB_USER", get("MYSQLUSER", get("MYSQL_USER", get("DB_USERNAME", null))));
        if (user != null && !user.trim().isEmpty()) {
            return user.trim();
        }
        return get("db.user", "root");
    }

    public static String getDbPassword() {
        if (parsedDbPassFromUrl != null) {
            return parsedDbPassFromUrl;
        }
        String pass = get("DB_PASSWORD", get("MYSQLPASSWORD", get("MYSQL_PASSWORD", get("DB_PASS", null))));
        if (pass != null) {
            return pass.trim();
        }
        return get("db.password", "");
    }

    // =========================================================================
    // HikariCP Pool Settings
    // =========================================================================

    public static int getHikariMaximumPoolSize() {
        return getInt("HIKARI_MAX_POOL_SIZE", getInt("hikari.maximumPoolSize", 10));
    }

    public static int getHikariMinimumIdle() {
        return getInt("HIKARI_MIN_IDLE", getInt("hikari.minimumIdle", 2));
    }

    public static long getHikariIdleTimeout() {
        return getLong("HIKARI_IDLE_TIMEOUT", getLong("hikari.idleTimeout", 30000L));
    }

    public static long getHikariConnectionTimeout() {
        return getLong("HIKARI_CONNECTION_TIMEOUT", getLong("hikari.connectionTimeout", 20000L));
    }

    public static long getHikariMaxLifetime() {
        return getLong("HIKARI_MAX_LIFETIME", getLong("hikari.maxLifetime", 1800000L));
    }

    // =========================================================================
    // Server & Application Settings
    // =========================================================================

    public static int getPort(int defaultPort) {
        return getInt("PORT", getInt("SERVER_PORT", getInt("server.port", defaultPort)));
    }

    public static String getContextPath(String defaultContext) {
        String ctx = get("CONTEXT_PATH", get("SERVER_CONTEXT_PATH", get("server.contextPath", defaultContext)));
        if (ctx == null) {
            return defaultContext;
        }
        ctx = ctx.trim();
        if (ctx.equals("/") || ctx.isEmpty()) {
            return "";
        }
        if (!ctx.startsWith("/")) {
            ctx = "/" + ctx;
        }
        return ctx;
    }

    public static boolean isDbAutoInitEnabled() {
        return getBoolean("DB_AUTO_INIT", true);
    }
}
