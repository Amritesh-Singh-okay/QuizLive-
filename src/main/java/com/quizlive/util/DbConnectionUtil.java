package com.quizlive.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Utility class to manage database connections using HikariCP connection pooling.
 * This class provides a high-performance, thread-safe connection pool for JDBC DAOs.
 */
public final class DbConnectionUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbConnectionUtil.class);
    private static volatile HikariDataSource dataSource;

    private DbConnectionUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Initializes and returns the singleton HikariDataSource.
     * Uses double-checked locking for thread-safety.
     *
     * @return the initialized DataSource
     */
    public static DataSource getDataSource() {
        if (dataSource == null) {
            synchronized (DbConnectionUtil.class) {
                if (dataSource == null) {
                    initDataSource();
                }
            }
        }
        return dataSource;
    }

    /**
     * Borrows an active Connection from the HikariCP pool.
     *
     * @return an open SQL Connection
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    /**
     * Initializes the connection pool from db.properties.
     */
    private static void initDataSource() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(EnvConfig.getDbDriver());
        config.setJdbcUrl(EnvConfig.getDbUrl());
        config.setUsername(EnvConfig.getDbUser());
        config.setPassword(EnvConfig.getDbPassword());

        // Pool tuning properties
        config.setMaximumPoolSize(EnvConfig.getHikariMaximumPoolSize());
        config.setMinimumIdle(EnvConfig.getHikariMinimumIdle());
        config.setIdleTimeout(EnvConfig.getHikariIdleTimeout());
        config.setConnectionTimeout(EnvConfig.getHikariConnectionTimeout());
        config.setMaxLifetime(EnvConfig.getHikariMaxLifetime());

        // Pool identification
        config.setPoolName("QuizLive-HikariPool");

        // Performance optimizations recommended for MySQL
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        try {
            LOGGER.info("Initializing HikariCP pool for database: {} (user: {})", EnvConfig.getDbUrl(), EnvConfig.getDbUser());
            dataSource = new HikariDataSource(config);
            LOGGER.info("HikariCP connection pool initialized successfully.");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize HikariCP connection pool: {}", e.getMessage());
            throw new RuntimeException("Database pool initialization failed", e);
        }
    }

    /**
     * Safely closes the connection pool upon application shutdown.
     */
    public static void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            LOGGER.info("HikariCP connection pool closed.");
        }
    }
}
