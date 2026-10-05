package com.quizlive;

import com.quizlive.util.DatabaseInitializer;
import com.quizlive.util.DbConnectionUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DbConnectionTest {

    @Test
    @DisplayName("Verify DataSource loads configuration and creates pool")
    void testDataSourceLoads() {
        assertNotNull(DbConnectionUtil.getDataSource(), "DataSource should not be null");
    }

    @Test
    @DisplayName("Verify DB connection and seed data verification")
    void testConnectionAndDatabaseInitialization() {
        // Run database initialization
        DatabaseInitializer.initialize();

        try (Connection conn = DbConnectionUtil.getConnection();
             Statement stmt = conn.createStatement()) {

            assertNotNull(conn, "Connection should be valid and open");
            assertTrue(conn.isValid(2), "Connection should be valid");

            // Verify users table has initial seed rows
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM users")) {
                assertTrue(rs.next());
                int count = rs.getInt("total");
                assertTrue(count >= 5, "Database should contain at least 5 seed users");
                System.out.println(">>> SUCCESS: Found " + count + " users in MySQL 'quizlive' database! <<<");
            }

            // Verify quizzes table has initial seed rows
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM quizzes")) {
                assertTrue(rs.next());
                int count = rs.getInt("total");
                assertTrue(count >= 3, "Database should contain at least 3 seed quizzes");
                System.out.println(">>> SUCCESS: Found " + count + " quizzes in MySQL 'quizlive' database! <<<");
            }
        } catch (Exception e) {
            throw new RuntimeException("Database verification failed", e);
        }
    }
}
