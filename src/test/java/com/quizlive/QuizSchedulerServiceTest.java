package com.quizlive;

import com.quizlive.concurrency.ActiveAttemptRegistry;
import com.quizlive.concurrency.QuizSchedulerService;
import com.quizlive.dao.AttemptDao;
import com.quizlive.dao.QuestionDao;
import com.quizlive.dao.UserDao;
import com.quizlive.dao.impl.AttemptDaoImpl;
import com.quizlive.dao.impl.QuestionDaoImpl;
import com.quizlive.dao.impl.UserDaoImpl;
import com.quizlive.model.AppUser;
import com.quizlive.model.Attempt;
import com.quizlive.model.enums.AttemptStatus;
import com.quizlive.model.enums.Role;
import com.quizlive.service.LeaderboardService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuizSchedulerServiceTest {

    private QuizSchedulerService schedulerService;
    private ScheduledExecutorService executorService;
    private AttemptDao attemptDao;
    private UserDao userDao;
    private int tempUserId = 0;

    @BeforeEach
    void setUp() {
        this.executorService = Executors.newScheduledThreadPool(2);
        this.attemptDao = new AttemptDaoImpl();
        this.userDao = new UserDaoImpl();
        QuestionDao questionDao = new QuestionDaoImpl();
        LeaderboardService leaderboardService = new LeaderboardService();

        this.schedulerService = new QuizSchedulerService(
                executorService,
                ActiveAttemptRegistry.getInstance(),
                attemptDao,
                questionDao,
                leaderboardService
        );
    }

    @AfterEach
    void tearDown() throws SQLException {
        schedulerService.shutdown();
        if (tempUserId > 0) {
            userDao.delete(tempUserId);
            tempUserId = 0;
        }
    }

    @Test
    @DisplayName("Verify scheduling and manual cancellation")
    void testScheduleAndCancel() {
        int attemptId = 999;
        schedulerService.scheduleAutoSubmit(attemptId, 1, 3, 300);

        assertTrue(schedulerService.isScheduled(attemptId));

        boolean cancelled = schedulerService.cancelScheduledAutoSubmit(attemptId);
        assertTrue(cancelled);
        assertFalse(schedulerService.isScheduled(attemptId));
    }

    @Test
    @DisplayName("Verify auto-submit execution finalizes attempt to AUTO_SUBMITTED")
    void testAutoSubmitExecution() throws SQLException {
        String email = "autosubmit_test_" + System.currentTimeMillis() + "@quizlive.com";
        AppUser user = userDao.create(AppUser.create(0, "AutoSubmit Student", email, "hash", "salt", Role.PARTICIPANT, null));
        tempUserId = user.getId();

        Attempt attempt = attemptDao.startAttempt(1, tempUserId);
        assertEquals(AttemptStatus.IN_PROGRESS, attempt.getStatus());

        schedulerService.handleAutoSubmit(attempt.getId());

        Attempt finalized = attemptDao.findById(attempt.getId());
        assertNotNull(finalized);
        assertEquals(AttemptStatus.AUTO_SUBMITTED, finalized.getStatus());
        assertNotNull(finalized.getSubmittedAt());
    }
}
