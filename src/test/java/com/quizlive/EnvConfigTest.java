package com.quizlive;

import com.quizlive.util.EnvConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnvConfigTest {

    @Test
    @DisplayName("Verify EnvConfig retrieves port and context path correctly")
    void testPortAndContextPath() {
        int port = EnvConfig.getPort(8080);
        assertTrue(port > 0, "Resolved port must be a positive integer");

        String contextPath = EnvConfig.getContextPath("/quizlive");
        assertNotNull(contextPath);
        assertTrue(contextPath.startsWith("/") || contextPath.isEmpty(),
                "Context path should start with / or be empty for root");
    }

    @Test
    @DisplayName("Verify EnvConfig retrieves database connection parameters")
    void testDatabaseParameters() {
        String dbDriver = EnvConfig.getDbDriver();
        assertEquals("com.mysql.cj.jdbc.Driver", dbDriver);

        String dbUrl = EnvConfig.getDbUrl();
        assertNotNull(dbUrl);
        assertTrue(dbUrl.startsWith("jdbc:mysql://"), "Database URL must be a valid MySQL JDBC URL");

        String dbUser = EnvConfig.getDbUser();
        assertNotNull(dbUser);
        assertFalse(dbUser.trim().isEmpty());

        // Hikari pool settings
        assertTrue(EnvConfig.getHikariMaximumPoolSize() >= 2);
        assertTrue(EnvConfig.getHikariMinimumIdle() >= 1);
        assertTrue(EnvConfig.getHikariIdleTimeout() > 0);
        assertTrue(EnvConfig.getHikariConnectionTimeout() > 0);
        assertTrue(EnvConfig.getHikariMaxLifetime() > 0);
    }

    @Test
    @DisplayName("Verify System Property overrides defaults")
    void testSystemPropertyPrecedence() {
        String testKey = "test.custom.key";
        try {
            System.setProperty(testKey, "system-override-value");
            assertEquals("system-override-value", EnvConfig.get(testKey, "default"));
        } finally {
            System.clearProperty(testKey);
        }
    }

    @Test
    @DisplayName("Verify type conversion helpers")
    void testTypeConversions() {
        try {
            System.setProperty("test.int.val", "42");
            System.setProperty("test.long.val", "999999999");
            System.setProperty("test.bool.val", "true");

            assertEquals(42, EnvConfig.getInt("test.int.val", 0));
            assertEquals(999999999L, EnvConfig.getLong("test.long.val", 0L));
            assertTrue(EnvConfig.getBoolean("test.bool.val", false));

            // Test fallbacks
            assertEquals(10, EnvConfig.getInt("non.existent.int", 10));
            assertEquals(500L, EnvConfig.getLong("non.existent.long", 500L));
            assertFalse(EnvConfig.getBoolean("non.existent.bool", false));
        } finally {
            System.clearProperty("test.int.val");
            System.clearProperty("test.long.val");
            System.clearProperty("test.bool.val");
        }
    }

    @Test
    @DisplayName("Verify DATABASE_URL URI parsing when set as system property")
    void testDatabaseUriParsing() {
        String uriKey = "DATABASE_URL";
        try {
            System.setProperty(uriKey, "mysql://testuser:testpass@mysql.railway.internal:3306/productiondb");
            String resolvedUrl = EnvConfig.getDbUrl();
            assertTrue(resolvedUrl.startsWith("jdbc:mysql://mysql.railway.internal:3306/productiondb"));
            assertEquals("testuser", EnvConfig.getDbUser());
            assertEquals("testpass", EnvConfig.getDbPassword());
        } finally {
            System.clearProperty(uriKey);
        }
    }
}
