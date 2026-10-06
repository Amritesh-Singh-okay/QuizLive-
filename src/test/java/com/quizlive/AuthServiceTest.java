package com.quizlive;

import com.quizlive.dao.UserDao;
import com.quizlive.dao.impl.UserDaoImpl;
import com.quizlive.exception.UnauthorizedException;
import com.quizlive.model.AppUser;
import com.quizlive.model.enums.Role;
import com.quizlive.service.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {

    private AuthService authService;
    private UserDao userDao;
    private int createdUserId = 0;

    @BeforeEach
    void setUp() {
        this.userDao = new UserDaoImpl();
        this.authService = new AuthService(userDao);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (createdUserId > 0) {
            userDao.delete(createdUserId);
            createdUserId = 0;
        }
    }

    @Test
    @DisplayName("Verify successful login with valid credentials")
    void testLoginSuccess() throws SQLException, UnauthorizedException {
        AppUser user = authService.login("admin@quizlive.com", "password123");
        assertNotNull(user);
        assertEquals("admin@quizlive.com", user.getEmail());
        assertEquals(Role.ADMIN, user.getRole());
    }

    @Test
    @DisplayName("Verify login rejection on incorrect password or unknown user")
    void testLoginFailure() {
        assertThrows(UnauthorizedException.class, () ->
                authService.login("admin@quizlive.com", "wrongPassword")
        );

        assertThrows(UnauthorizedException.class, () ->
                authService.login("nonexistent@quizlive.com", "password123")
        );
    }

    @Test
    @DisplayName("Verify user registration and subsequent login")
    void testRegisterAndLogin() throws SQLException, UnauthorizedException {
        String testEmail = "testauth_" + System.currentTimeMillis() + "@quizlive.com";
        String testPassword = "securePassword99";

        AppUser registered = authService.register("Auth Test User", testEmail, testPassword, Role.PARTICIPANT);
        assertNotNull(registered);
        assertTrue(registered.getId() > 0);
        createdUserId = registered.getId();

        AppUser loggedIn = authService.login(testEmail, testPassword);
        assertNotNull(loggedIn);
        assertEquals(registered.getId(), loggedIn.getId());
        assertEquals(Role.PARTICIPANT, loggedIn.getRole());
    }

    @Test
    @DisplayName("Verify registration rejects duplicate emails and weak passwords")
    void testRegistrationValidation() {
        assertThrows(IllegalArgumentException.class, () ->
                authService.register("Duplicate", "admin@quizlive.com", "password123", Role.PARTICIPANT)
        );

        assertThrows(IllegalArgumentException.class, () ->
                authService.register("Short Pass", "short@quizlive.com", "123", Role.PARTICIPANT)
        );
    }

    @Test
    @DisplayName("Verify registration rejects ADMIN role")
    void testRegisterAdminRoleRejected() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                authService.register("Admin Aspirant", "aspiring_admin@quizlive.com", "secure123", Role.ADMIN)
        );
        assertEquals("Registration with ADMIN role is not permitted", ex.getMessage());
    }

    @Test
    @DisplayName("Verify creator registration succeeds")
    void testRegisterCreatorRoleSuccess() throws SQLException {
        String testEmail = "creator_" + System.currentTimeMillis() + "@quizlive.com";
        AppUser creator = authService.register("Quiz Author", testEmail, "author123", Role.CREATOR);
        assertNotNull(creator);
        assertEquals(Role.CREATOR, creator.getRole());
        userDao.delete(creator.getId());
    }
}
