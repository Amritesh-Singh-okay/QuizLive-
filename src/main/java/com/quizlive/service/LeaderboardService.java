package com.quizlive.service;

import com.quizlive.dao.AttemptDao;
import com.quizlive.dao.QuizDao;
import com.quizlive.dao.impl.AttemptDaoImpl;
import com.quizlive.dao.impl.QuizDaoImpl;
import com.quizlive.model.Attempt;
import com.quizlive.model.LeaderboardEntry;
import com.quizlive.model.Quiz;
import com.quizlive.model.enums.AttemptStatus;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LeaderboardService {

    private final AttemptDao attemptDao;
    private final QuizDao quizDao;

    private static final Comparator<Attempt> RANKING_COMPARATOR = Comparator
            .comparing(Attempt::getScore, Comparator.reverseOrder())
            .thenComparing(Attempt::getDurationSeconds)
            .thenComparing(Attempt::getSubmittedAt, Comparator.nullsLast(Comparator.naturalOrder()));

    public LeaderboardService() {
        this.attemptDao = new AttemptDaoImpl();
        this.quizDao = new QuizDaoImpl();
    }

    public LeaderboardService(AttemptDao attemptDao, QuizDao quizDao) {
        this.attemptDao = attemptDao;
        this.quizDao = quizDao;
    }

    public synchronized List<LeaderboardEntry> refreshAndGetLeaderboard(int quizId) throws SQLException {
        return getLeaderboard(quizId);
    }

    public List<LeaderboardEntry> getLeaderboard(int quizId) throws SQLException {
        Quiz quiz = quizDao.findByIdWithQuestions(quizId);
        int maxScore = (quiz != null) ? quiz.getTotalPoints() : 0;

        List<Attempt> rawAttempts = attemptDao.listByQuiz(quizId);
        List<Attempt> eligibleAttempts = new ArrayList<>();

        for (Attempt a : rawAttempts) {
            if (a.getStatus() == AttemptStatus.SUBMITTED || a.getStatus() == AttemptStatus.AUTO_SUBMITTED) {
                eligibleAttempts.add(a);
            }
        }

        eligibleAttempts.sort(RANKING_COMPARATOR);

        List<LeaderboardEntry> leaderboard = new ArrayList<>();
        int currentRank = 1;

        for (Attempt a : eligibleAttempts) {
            double percentage = 0.0;
            if (maxScore > 0) {
                percentage = Math.round(((double) a.getScore() / maxScore) * 10000.0) / 100.0;
            }

            LeaderboardEntry entry = new LeaderboardEntry(
                    currentRank++,
                    a.getId(),
                    a.getUserId(),
                    a.getUserName(),
                    a.getScore(),
                    maxScore,
                    percentage,
                    a.getDurationSeconds(),
                    a.getFormattedDuration(),
                    a.getTabSwitches(),
                    a.getSubmittedAt()
            );

            leaderboard.add(entry);
        }

        return leaderboard;
    }
}
