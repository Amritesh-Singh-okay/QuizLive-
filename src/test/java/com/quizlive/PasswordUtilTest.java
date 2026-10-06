package com.quizlive;

import com.quizlive.util.PasswordUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordUtilTest {

    private static final String SEED_SALT = "f5974143ae362d2cbcb6489d0c088da1";
    private static final String SEED_HASH = "958b3891df8f6f164ed2c3384bbefc07e9c7498b7cef40359bc3fc4a6e746895";

    @Test
    @DisplayName("Verify salt generation randomness and length")
    void testSaltGeneration() {
        String salt1 = PasswordUtil.generateSalt();
        String salt2 = PasswordUtil.generateSalt();

        assertNotNull(salt1);
        assertNotNull(salt2);
        assertEquals(32, salt1.length());
        assertEquals(32, salt2.length());
        assertNotEquals(salt1, salt2);
    }

    @Test
    @DisplayName("Verify password hash matches known seed hash")
    void testPasswordHashingMatchesSeed() {
        String computedHash = PasswordUtil.hashPassword("password123", SEED_SALT);
        assertEquals(SEED_HASH, computedHash);
    }

    @Test
    @DisplayName("Verify password verification succeeds on correct password and fails on wrong")
    void testPasswordVerification() {
        assertTrue(PasswordUtil.verifyPassword("password123", SEED_HASH, SEED_SALT));
        assertFalse(PasswordUtil.verifyPassword("wrongPassword", SEED_HASH, SEED_SALT));
        assertFalse(PasswordUtil.verifyPassword("", SEED_HASH, SEED_SALT));
        assertFalse(PasswordUtil.verifyPassword(null, SEED_HASH, SEED_SALT));
    }
}
