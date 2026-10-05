package com.quizlive;

import com.quizlive.dao.MessageDao;
import com.quizlive.dao.SettingsDao;
import com.quizlive.dao.impl.MessageDaoImpl;
import com.quizlive.dao.impl.SettingsDaoImpl;
import com.quizlive.model.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageAndSettingsDaoTest {

    private MessageDao messageDao;
    private SettingsDao settingsDao;

    @BeforeEach
    void setUp() {
        this.messageDao = new MessageDaoImpl();
        this.settingsDao = new SettingsDaoImpl();
    }

    @Test
    @DisplayName("Verify MessageDao thread retrieval and sending")
    void testMessageDao() throws SQLException {
        List<Message> thread = messageDao.getThread(3, 2);
        assertNotNull(thread);
        assertTrue(thread.size() >= 2);

        Message first = thread.get(0);
        assertEquals(3, first.getFromUserId());
        assertEquals(2, first.getToUserId());

        Message newMsg = new Message(0, 3, 2, 1, "Testing new reply message", null);
        Message sent = messageDao.send(newMsg);
        assertTrue(sent.getId() > 0);

        List<Message> updatedThread = messageDao.getThread(3, 2);
        assertTrue(updatedThread.size() > thread.size());
    }

    @Test
    @DisplayName("Verify SettingsDao get, upsert, and getAllSettings")
    void testSettingsDao() throws SQLException {
        String platform = settingsDao.getSetting("platform_name", "Default");
        assertEquals("QuizLive", platform);

        String missing = settingsDao.getSetting("non_existent_key", "Fallback");
        assertEquals("Fallback", missing);

        boolean saved = settingsDao.setSetting("custom_test_setting", "12345");
        assertTrue(saved);
        assertEquals("12345", settingsDao.getSetting("custom_test_setting", null));

        Map<String, String> all = settingsDao.getAllSettings();
        assertFalse(all.isEmpty());
        assertTrue(all.containsKey("platform_name"));
        assertTrue(all.containsKey("custom_test_setting"));
    }
}
