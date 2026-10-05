package com.quizlive;

import com.quizlive.dao.UserDao;
import com.quizlive.dao.impl.UserDaoImpl;
import com.quizlive.model.Admin;
import com.quizlive.model.AppUser;
import com.quizlive.model.Participant;
import com.quizlive.model.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserDaoTest {

    private UserDao userDao;

    @BeforeEach
    void setUp() {
        userDao = new UserDaoImpl();
    }

    @Test
    @DisplayName("Verify findByEmail retrieves existing seed user polymorphically")
    void testFindByEmail() throws SQLException {
        AppUser admin = userDao.findByEmail("admin@quizlive.com");
        assertNotNull(admin);
        assertEquals("System Administrator", admin.getName());
        assertEquals(Role.ADMIN, admin.getRole());
        assertInstanceOf(Admin.class, admin);
    }

    @Test
    @DisplayName("Verify findById retrieves correct user")
    void testFindById() throws SQLException {
        AppUser user = userDao.findById(1);
        assertNotNull(user);
        assertEquals(1, user.getId());
        assertEquals("admin@quizlive.com", user.getEmail());
    }

    @Test
    @DisplayName("Verify user CRUD operations: create, update, delete")
    void testUserCrudLifecycle() throws SQLException {
        String testEmail = "testuser_" + System.currentTimeMillis() + "@quizlive.com";
        AppUser newUser = AppUser.create(0, "Test Student", testEmail, "testHash", "testSalt", Role.PARTICIPANT, null);

        AppUser created = userDao.create(newUser);
        assertTrue(created.getId() > 0);
        assertInstanceOf(Participant.class, created);

        created.setName("Updated Student Name");
        boolean updated = userDao.update(created);
        assertTrue(updated);

        AppUser fetched = userDao.findById(created.getId());
        assertEquals("Updated Student Name", fetched.getName());

        boolean deleted = userDao.delete(created.getId());
        assertTrue(deleted);

        AppUser afterDelete = userDao.findById(created.getId());
        assertNull(afterDelete);
    }

    @Test
    @DisplayName("Verify listAll and listByRole")
    void testListUsers() throws SQLException {
        List<AppUser> allUsers = userDao.listAll();
        assertTrue(allUsers.size() >= 5);

        List<AppUser> participants = userDao.listByRole(Role.PARTICIPANT);
        assertFalse(participants.isEmpty());
        for (AppUser p : participants) {
            assertEquals(Role.PARTICIPANT, p.getRole());
            assertInstanceOf(Participant.class, p);
        }
    }
}
