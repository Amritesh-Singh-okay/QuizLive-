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
        LOGGER.info("Starting database schema and seed data check...");
        executeSqlScript("schema.sql");
        executeSqlScript("seed.sql");
        LOGGER.info("Database initialization completed successfully.");
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

                        if (!sql.isEmpty() && !sql.toUpperCase().startsWith("USE ")) {
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
