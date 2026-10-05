package com.quizlive;

import com.quizlive.model.Admin;
import com.quizlive.model.AppUser;
import com.quizlive.model.Participant;
import com.quizlive.model.QuizCreator;
import com.quizlive.model.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserHierarchyTest {

    private final Timestamp now = new Timestamp(System.currentTimeMillis());

    @Test
    @DisplayName("Verify Polymorphic AppUser Factory instantiates correct subclasses")
    void testFactoryInstantiation() {
        AppUser admin = AppUser.create(1, "Admin", "admin@quizlive.com", "hash", "salt", Role.ADMIN, now);
        AppUser creator = AppUser.create(2, "Creator", "creator@quizlive.com", "hash", "salt", Role.CREATOR, now);
        AppUser participant = AppUser.create(3, "Student", "student@quizlive.com", "hash", "salt", Role.PARTICIPANT, now);

        assertInstanceOf(Admin.class, admin);
        assertInstanceOf(QuizCreator.class, creator);
        assertInstanceOf(Participant.class, participant);
    }

    @Test
    @DisplayName("Verify Polymorphic method dispatch for role capabilities")
    void testPolymorphicCapabilities() {
        AppUser admin = AppUser.create(1, "Admin", "admin@quizlive.com", "hash", "salt", Role.ADMIN, now);
        AppUser creator = AppUser.create(2, "Creator", "creator@quizlive.com", "hash", "salt", Role.CREATOR, now);
        AppUser participant = AppUser.create(3, "Student", "student@quizlive.com", "hash", "salt", Role.PARTICIPANT, now);

        // Admin capabilities
        assertTrue(admin.canApproveQuiz());
        assertFalse(admin.canCreateQuiz());
        assertFalse(admin.canTakeQuiz());
        assertEquals("/admin/dashboard.jsp", admin.getDashboardUrl());

        // Creator capabilities
        assertFalse(creator.canApproveQuiz());
        assertTrue(creator.canCreateQuiz());
        assertFalse(creator.canTakeQuiz());
        assertEquals("/creator/dashboard.jsp", creator.getDashboardUrl());

        // Participant capabilities
        assertFalse(participant.canApproveQuiz());
        assertFalse(participant.canCreateQuiz());
        assertTrue(participant.canTakeQuiz());
        assertEquals("/participant/dashboard.jsp", participant.getDashboardUrl());
    }
}
