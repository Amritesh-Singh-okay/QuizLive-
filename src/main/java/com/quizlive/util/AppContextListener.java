package com.quizlive.util;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Global application lifecycle listener.
 * Handles automatic database migration/seeding on startup and clean pool teardown on shutdown,
 * guaranteeing correct operation whether QuizLive is launched via embedded AppLauncher or
 * deployed as a WAR inside standalone Tomcat / Cloud container environments.
 */
@WebListener
public class AppContextListener implements ServletContextListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppContextListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOGGER.info("QuizLive application context initialized. Verifying database...");
        try {
            DatabaseInitializer.initialize();
        } catch (Exception e) {
            LOGGER.warn("Database initialization check skipped or reported: {}", e.getMessage());
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOGGER.info("QuizLive application context destroying. Releasing resources...");
        try {
            DbConnectionUtil.closePool();
        } catch (Exception e) {
            LOGGER.warn("Error closing connection pool: {}", e.getMessage());
        }
    }
}
