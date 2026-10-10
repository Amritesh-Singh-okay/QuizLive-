package com.quizlive.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Automatically executes SQL scripts on application startup to ensure
 * all required tables and default seed data are properly initialized.
 */
public final class DatabaseInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseInitializer.class);

    private DatabaseInitializer() {
    }

    /**
     * Executes the schema and seed scripts against the configured database.
     */
    public static void initialize() {
        if (!EnvConfig.isDbAutoInitEnabled()) {
            LOGGER.info("Database auto-initialization is disabled via configuration (DB_AUTO_INIT=false).");
            return;
        }
        LOGGER.info("Starting database schema and seed data check...");
        executeSqlScript("schema.sql");
        applySchemaMigrations();
        executeSqlScript("seed.sql");
        LOGGER.info("Database initialization completed successfully.");
    }

    /**
     * Applies backward-compatible schema updates for live hosting and waiting rooms.
     */
    private static void applySchemaMigrations() {
        try (Connection conn = DbConnectionUtil.getConnection();
             Statement stmt = conn.createStatement()) {
            try {
                stmt.execute("ALTER TABLE quizzes ADD COLUMN is_held BOOLEAN DEFAULT FALSE");
            } catch (Exception ignored) {
            }
            try {
                stmt.execute("ALTER TABLE quizzes ADD COLUMN scheduled_start_at TIMESTAMP NULL");
            } catch (Exception ignored) {
            }
            try {
                stmt.execute("ALTER TABLE users ADD COLUMN creator_rank VARCHAR(50) DEFAULT 'STANDARD'");
            } catch (Exception ignored) {
            }
            try {
                stmt.execute("ALTER TABLE quizzes ADD COLUMN access_code VARCHAR(32) NULL UNIQUE");
            } catch (Exception ignored) {
            }
            try {
                stmt.execute("ALTER TABLE quizzes ADD COLUMN is_public BOOLEAN DEFAULT TRUE");
            } catch (Exception ignored) {
            }
            try {
                stmt.execute("UPDATE quizzes SET access_code = CONCAT('QZ-', id, '00') WHERE access_code IS NULL");
            } catch (Exception ignored) {
            }
            try {
                stmt.execute("UPDATE quizzes SET is_public = TRUE WHERE is_public IS NULL");
            } catch (Exception ignored) {
            }
            try {
                stmt.execute("UPDATE users SET creator_rank = 'STANDARD' WHERE creator_rank IS NULL");
            } catch (Exception ignored) {
            }
        } catch (Exception e) {
            LOGGER.debug("Schema migration notice: {}", e.getMessage());
        }
    }

    /**
     * Reads a resource SQL file from the classpath and executes statements sequentially.
     *
     * @param scriptPath path to the SQL script in classpath
     */
    public static void executeSqlScript(String scriptPath) {
        try (InputStream in = DatabaseInitializer.class.getClassLoader().getResourceAsStream(scriptPath)) {
            if (in == null) {
                LOGGER.warn("SQL script not found: {}", scriptPath);
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                 Connection conn = DbConnectionUtil.getConnection();
                 Statement stmt = conn.createStatement()) {

                StringBuilder currentStatement = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    line = line.trim();

                    // Skip empty lines and single-line SQL comments
                    if (line.isEmpty() || line.startsWith("--") || line.startsWith("#")) {
                        continue;
                    }

                    currentStatement.append(line).append(" ");

                    if (line.endsWith(";")) {
                        String sql = currentStatement.toString().trim();
                        // Strip trailing semicolon for execution
                        sql = sql.substring(0, sql.length() - 1).trim();

                        String upperSql = sql.toUpperCase();
                        if (!sql.isEmpty() && !upperSql.startsWith("USE ") && !upperSql.startsWith("CREATE DATABASE ")) {
                            try {
                                stmt.execute(sql);
                            } catch (Exception e) {
                                LOGGER.debug("SQL statement note: {} (Message: {})", sql, e.getMessage());
                            }
                        }
                        currentStatement.setLength(0);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error executing script {}: {}", scriptPath, e.getMessage());
        }
    }
}
