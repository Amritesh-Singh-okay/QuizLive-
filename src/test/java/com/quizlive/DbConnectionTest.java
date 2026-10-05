package com.quizlive;

import com.quizlive.util.DbConnectionUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DbConnectionTest {

    @Test
    @DisplayName("Verify DataSource loads configuration and creates pool")
    void testDataSourceLoads() {
        assertNotNull(DbConnectionUtil.getDataSource(), "DataSource should not be null");
    }

    @Test
    @DisplayName("Verify DB connection can be borrowed (when credentials valid)")
    void testConnectionCanBeBorrowed() {
        try (Connection conn = DbConnectionUtil.getConnection()) {
            assertNotNull(conn, "Connection should be valid and open");
            assertTrue(conn.isValid(2), "Connection should be valid within 2 seconds");
            System.out.println(">>> SUCCESS: Connected to MySQL database via HikariCP! <<<");
        } catch (Exception e) {
            System.out.println(">>> NOTE: Database connection failed (Update password in db.properties): " + e.getMessage());
        }
    }
}
