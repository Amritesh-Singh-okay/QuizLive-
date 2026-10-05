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
        Properties props = new Properties();

        try (InputStream in = DbConnectionUtil.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new IllegalStateException("db.properties file not found on classpath!");
            }
            props.load(in);
        } catch (IOException e) {
            LOGGER.error("Failed to load db.properties file", e);
            throw new RuntimeException("Could not read db.properties", e);
        }

        HikariConfig config = new HikariConfig();
        config.setDriverClassName(props.getProperty("db.driver", "com.mysql.cj.jdbc.Driver"));
        config.setJdbcUrl(props.getProperty("db.url"));
        config.setUsername(props.getProperty("db.user", "root"));
        config.setPassword(props.getProperty("db.password", ""));

        // Pool tuning properties
        config.setMaximumPoolSize(Integer.parseInt(props.getProperty("hikari.maximumPoolSize", "10")));
        config.setMinimumIdle(Integer.parseInt(props.getProperty("hikari.minimumIdle", "2")));
        config.setIdleTimeout(Long.parseLong(props.getProperty("hikari.idleTimeout", "30000")));
        config.setConnectionTimeout(Long.parseLong(props.getProperty("hikari.connectionTimeout", "20000")));
        config.setMaxLifetime(Long.parseLong(props.getProperty("hikari.maxLifetime", "1800000")));

        // Pool identification
        config.setPoolName("QuizLive-HikariPool");

        // Performance optimizations recommended for MySQL
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        try {
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
