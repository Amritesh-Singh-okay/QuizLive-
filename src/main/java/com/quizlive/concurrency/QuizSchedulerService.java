package com.quizlive.concurrency;

import com.quizlive.dao.AttemptDao;
import com.quizlive.dao.QuestionDao;
import com.quizlive.dao.impl.AttemptDaoImpl;
import com.quizlive.dao.impl.QuestionDaoImpl;
import com.quizlive.model.Attempt;
import com.quizlive.model.enums.AttemptStatus;
import com.quizlive.service.LeaderboardService;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class QuizSchedulerService {

    private static final int THREAD_POOL_SIZE = 4;
    private static final int GRACE_PERIOD_SECONDS = 3;

    private final ScheduledExecutorService scheduler;
    private final ConcurrentHashMap<Integer, ScheduledFuture<?>> scheduledTasks;
    private final ActiveAttemptRegistry registry;
    private final AttemptDao attemptDao;
    private final QuestionDao questionDao;
    private final LeaderboardService leaderboardService;

    public QuizSchedulerService() {
        this(
                Executors.newScheduledThreadPool(THREAD_POOL_SIZE, r -> {
                    Thread thread = new Thread(r, "QuizLive-SchedulerThread");
                    thread.setDaemon(true);
                    return thread;
                }),
                ActiveAttemptRegistry.getInstance(),
                new AttemptDaoImpl(),
                new QuestionDaoImpl(),
                new LeaderboardService()
        );
    }

    public QuizSchedulerService(ScheduledExecutorService scheduler, ActiveAttemptRegistry registry,
                                AttemptDao attemptDao, QuestionDao questionDao,
                                LeaderboardService leaderboardService) {
        this.scheduler = scheduler;
        this.scheduledTasks = new ConcurrentHashMap<>();
        this.registry = registry;
        this.attemptDao = attemptDao;
        this.questionDao = questionDao;
        this.leaderboardService = leaderboardService;
    }

    public void scheduleAutoSubmit(int attemptId, int quizId, int userId, int durationSeconds) {
        registry.registerSession(attemptId, quizId, userId, durationSeconds, GRACE_PERIOD_SECONDS);

        long delaySeconds = durationSeconds + GRACE_PERIOD_SECONDS;
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                handleAutoSubmit(attemptId);
            } catch (Exception e) {
                // Background exception suppressed
            }
        }, delaySeconds, TimeUnit.SECONDS);

        scheduledTasks.put(attemptId, future);
    }

    public boolean cancelScheduledAutoSubmit(int attemptId) {
        registry.removeSession(attemptId);
        ScheduledFuture<?> future = scheduledTasks.remove(attemptId);
        if (future != null) {
            return future.cancel(false);
        }
        return false;
    }

    public synchronized void handleAutoSubmit(int attemptId) throws SQLException {
        registry.removeSession(attemptId);
        scheduledTasks.remove(attemptId);

        Attempt attempt = attemptDao.findById(attemptId);
        if (attempt != null && attempt.getStatus() == AttemptStatus.IN_PROGRESS) {
            attempt.setStatus(AttemptStatus.AUTO_SUBMITTED);
            attempt.setSubmittedAt(new Timestamp(System.currentTimeMillis()));

            attemptDao.submitAttempt(attempt, List.of());
            leaderboardService.refreshAndGetLeaderboard(attempt.getQuizId());
        }
    }

    public boolean isScheduled(int attemptId) {
        return scheduledTasks.containsKey(attemptId);
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
