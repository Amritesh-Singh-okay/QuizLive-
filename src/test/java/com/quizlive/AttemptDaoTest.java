package com.quizlive;

import com.quizlive.dao.AttemptAnswerDao;
import com.quizlive.dao.AttemptDao;
import com.quizlive.dao.impl.AttemptAnswerDaoImpl;
import com.quizlive.dao.impl.AttemptDaoImpl;
import com.quizlive.model.Attempt;
import com.quizlive.model.AttemptAnswer;
import com.quizlive.model.enums.AttemptStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttemptDaoTest {

    private AttemptDao attemptDao;
    private AttemptAnswerDao attemptAnswerDao;

    @BeforeEach
    void setUp() {
        this.attemptAnswerDao = new AttemptAnswerDaoImpl();
        this.attemptDao = new AttemptDaoImpl(attemptAnswerDao);
    }

    @Test
    @DisplayName("Verify getLeaderboard returns seeded participants ordered by score DESC")
    void testGetLeaderboard() throws SQLException {
        List<Attempt> leaderboard = attemptDao.getLeaderboard(1);
        assertNotNull(leaderboard);
        assertTrue(leaderboard.size() >= 2);

        Attempt first = leaderboard.get(0);
        Attempt second = leaderboard.get(1);

        assertEquals("Alice Johnson", first.getUserName());
        assertEquals(4, first.getScore());

        assertEquals("Bob Smith", second.getUserName());
        assertEquals(3, second.getScore());
        assertEquals(1, second.getTabSwitches());
    }

    @Test
    @DisplayName("Verify attempt lifecycle: start, tab-switch increment, submit with answers")
    void testAttemptLifecycle() throws SQLException {
        int quizId = 2;
        int userId = 5; // Charlie Brown

        Attempt existing = attemptDao.findByQuizAndUser(quizId, userId);
        if (existing != null) {
            assertEquals(userId, existing.getUserId());
            return;
        }

        Attempt attempt = attemptDao.startAttempt(quizId, userId);
        assertNotNull(attempt);
        assertTrue(attempt.getId() > 0);
        assertEquals(AttemptStatus.IN_PROGRESS, attempt.getStatus());

        boolean tabIncremented = attemptDao.incrementTabSwitches(attempt.getId());
        assertTrue(tabIncremented);

        attempt.setScore(2);
        attempt.setTabSwitches(1);
        attempt.setStatus(AttemptStatus.SUBMITTED);

        List<AttemptAnswer> answers = new ArrayList<>();
        answers.add(new AttemptAnswer(0, attempt.getId(), 5, 'B', true));
        answers.add(new AttemptAnswer(0, attempt.getId(), 6, 'A', false));

        boolean submitted = attemptDao.submitAttempt(attempt, answers);
        assertTrue(submitted);

        Attempt fetched = attemptDao.findById(attempt.getId());
        assertEquals(AttemptStatus.SUBMITTED, fetched.getStatus());
        assertEquals(2, fetched.getScore());
        assertEquals(1, fetched.getTabSwitches());

        List<AttemptAnswer> savedAnswers = attemptAnswerDao.findByAttemptId(attempt.getId());
        assertEquals(2, savedAnswers.size());
    }
}
