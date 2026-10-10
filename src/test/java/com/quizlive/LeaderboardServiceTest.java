package com.quizlive;

import com.quizlive.dao.AttemptDao;
import com.quizlive.dao.QuizDao;
import com.quizlive.model.Attempt;
import com.quizlive.model.AttemptAnswer;
import com.quizlive.model.LeaderboardEntry;
import com.quizlive.model.Quiz;
import com.quizlive.model.enums.AttemptStatus;
import com.quizlive.service.LeaderboardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaderboardServiceTest {

    @Test
    @DisplayName("Verify real leaderboard from database on seeded Quiz 1")
    void testRealLeaderboard() throws SQLException {
        LeaderboardService service = new LeaderboardService();
        List<LeaderboardEntry> leaderboard = service.getLeaderboard(1);

        assertNotNull(leaderboard);
        assertTrue(leaderboard.size() >= 2);

        LeaderboardEntry rank1 = leaderboard.get(0);
        assertEquals(1, rank1.getRank());
        assertEquals("Alice Johnson", rank1.getUserName());
        assertEquals(4, rank1.getScore());
        assertEquals(100.0, rank1.getPercentage());

        LeaderboardEntry rank2 = leaderboard.get(1);
        assertEquals(2, rank2.getRank());
        assertEquals("Bob Smith", rank2.getUserName());
        assertEquals(3, rank2.getScore());
        assertEquals(75.0, rank2.getPercentage());
    }

    @Test
    @DisplayName("Verify tie-breaker sorting via Comparator: same score ranks faster completion first")
    void testTieBreakerSorting() throws SQLException {
        long now = System.currentTimeMillis();

        Attempt fastStudent = new Attempt(1, 99, 10,
                new Timestamp(now - 45000), new Timestamp(now), 10, 10, 0, AttemptStatus.SUBMITTED);
        fastStudent.setUserName("Fast Student");

        Attempt slowStudent = new Attempt(2, 99, 11,
                new Timestamp(now - 120000), new Timestamp(now), 10, 10, 0, AttemptStatus.SUBMITTED);
        slowStudent.setUserName("Slow Student");

        List<Attempt> stubAttempts = new ArrayList<>();
        stubAttempts.add(slowStudent);
        stubAttempts.add(fastStudent);

        AttemptDao stubAttemptDao = new AttemptDao() {
            @Override
            public Attempt startAttempt(int quizId, int userId) { return null; }
            @Override
            public Attempt findById(int attemptId) { return null; }
            @Override
            public Attempt findByQuizAndUser(int quizId, int userId) { return null; }
            @Override
            public boolean submitAttempt(Attempt attempt, List<AttemptAnswer> answers) { return false; }
            @Override
            public boolean incrementTabSwitches(int attemptId) { return false; }
            @Override
            public List<Attempt> getLeaderboard(int quizId) { return stubAttempts; }
            @Override
            public List<Attempt> listByUser(int userId) { return stubAttempts; }
            @Override
            public List<Attempt> listByQuiz(int quizId) { return stubAttempts; }
        };

        QuizDao stubQuizDao = new QuizDao() {
            @Override
            public Quiz createWithQuestions(Quiz quiz) { return null; }
            @Override
            public Quiz findById(int id) { return null; }
            @Override
            public Quiz findByIdWithQuestions(int id) {
                Quiz q = new Quiz();
                q.setId(id);
                return q;
            }
            @Override
            public List<Quiz> listAll() { return List.of(); }
            @Override
            public List<Quiz> listApproved() { return List.of(); }
            @Override
            public List<Quiz> listByCreator(int creatorId) { return List.of(); }
            @Override
            public List<Quiz> listByStatus(com.quizlive.model.enums.QuizStatus status) { return List.of(); }
            @Override
            public boolean updateStatus(int quizId, com.quizlive.model.enums.QuizStatus status) { return false; }
            @Override
            public boolean updateHeldStatus(int quizId, boolean isHeld) { return false; }
            @Override
            public boolean updateScheduledStart(int quizId, java.sql.Timestamp scheduledStartAt) { return false; }
            @Override
            public boolean delete(int id) { return false; }
        };

        LeaderboardService service = new LeaderboardService(stubAttemptDao, stubQuizDao);
        List<LeaderboardEntry> sorted = service.getLeaderboard(99);

        assertEquals(2, sorted.size());
        assertEquals("Fast Student", sorted.get(0).getUserName(), "Faster student should win tie-break");
        assertEquals(1, sorted.get(0).getRank());

        assertEquals("Slow Student", sorted.get(1).getUserName());
        assertEquals(2, sorted.get(1).getRank());
    }
}
