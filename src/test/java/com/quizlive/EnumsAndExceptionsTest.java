package com.quizlive;

import com.quizlive.exception.InvalidAttemptException;
import com.quizlive.exception.QuizClosedException;
import com.quizlive.exception.UnauthorizedException;
import com.quizlive.model.enums.AttemptStatus;
import com.quizlive.model.enums.QuizStatus;
import com.quizlive.model.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnumsAndExceptionsTest {

    @Test
    @DisplayName("Verify Role enum parsing and values")
    void testRoleEnum() {
        assertEquals(Role.ADMIN, Role.fromString("admin"));
        assertEquals(Role.CREATOR, Role.fromString("CREATOR"));
        assertEquals(Role.PARTICIPANT, Role.fromString("participant"));
        assertNull(Role.fromString("INVALID_ROLE"));
        assertNull(Role.fromString(null));
        assertEquals("Admin", Role.ADMIN.getDisplayName());
    }

    @Test
    @DisplayName("Verify QuizStatus and AttemptStatus enum behavior")
    void testStatusEnums() {
        assertEquals(QuizStatus.APPROVED, QuizStatus.fromString("approved"));
        assertEquals(QuizStatus.PENDING, QuizStatus.fromString("PENDING"));
        assertNull(QuizStatus.fromString("unknown"));

        assertEquals(AttemptStatus.AUTO_SUBMITTED, AttemptStatus.fromString("auto_submitted"));
        assertEquals(AttemptStatus.SUBMITTED, AttemptStatus.fromString("submitted"));
        assertNull(AttemptStatus.fromString("unknown"));
    }

    @Test
    @DisplayName("Verify custom checked exceptions throw and preserve messages")
    void testCustomExceptions() {
        InvalidAttemptException ex1 = assertThrows(InvalidAttemptException.class, () -> {
            throw new InvalidAttemptException("Duplicate attempt detected");
        });
        assertEquals("Duplicate attempt detected", ex1.getMessage());

        QuizClosedException ex2 = assertThrows(QuizClosedException.class, () -> {
            throw new QuizClosedException("Time limit exceeded");
        });
        assertEquals("Time limit exceeded", ex2.getMessage());

        UnauthorizedException ex3 = assertThrows(UnauthorizedException.class, () -> {
            throw new UnauthorizedException("Admin access required");
        });
        assertEquals("Admin access required", ex3.getMessage());
    }
}
