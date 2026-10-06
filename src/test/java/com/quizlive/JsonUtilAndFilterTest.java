package com.quizlive;

import com.quizlive.model.AppUser;
import com.quizlive.model.enums.Role;
import com.quizlive.util.JsonUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonUtilAndFilterTest {

    @Test
    @DisplayName("Verify JsonUtil serialization and deserialization")
    void testJsonUtil() {
        AppUser user = AppUser.create(1, "Test Alice", "alice@example.com", "hash", "salt", Role.PARTICIPANT, null);

        String json = JsonUtil.toJson(user);
        assertNotNull(json);
        assertTrue(json.contains("Test Alice"));
        assertTrue(json.contains("alice@example.com"));

        Map<?, ?> map = JsonUtil.fromJson(json, Map.class);
        assertEquals("Test Alice", map.get("name"));
        assertEquals("alice@example.com", map.get("email"));
    }

    @Test
    @DisplayName("Verify role permissions checking logic")
    void testRolePermissions() {
        AppUser admin = AppUser.create(1, "Admin", "admin@quizlive.com", "h", "s", Role.ADMIN, null);
        AppUser creator = AppUser.create(2, "Creator", "creator@quizlive.com", "h", "s", Role.CREATOR, null);
        AppUser student = AppUser.create(3, "Student", "student@quizlive.com", "h", "s", Role.PARTICIPANT, null);

        assertTrue(admin.canApproveQuiz());
        assertFalse(admin.canCreateQuiz());

        assertTrue(creator.canCreateQuiz());
        assertFalse(creator.canApproveQuiz());

        assertTrue(student.canTakeQuiz());
        assertFalse(student.canCreateQuiz());
    }
}
