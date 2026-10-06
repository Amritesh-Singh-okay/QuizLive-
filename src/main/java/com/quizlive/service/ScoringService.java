package com.quizlive.service;

import com.quizlive.dao.AttemptDao;
import com.quizlive.dao.QuestionDao;
import com.quizlive.dao.impl.AttemptDaoImpl;
import com.quizlive.dao.impl.QuestionDaoImpl;
import com.quizlive.exception.InvalidAttemptException;
import com.quizlive.model.Attempt;
import com.quizlive.model.AttemptAnswer;
import com.quizlive.model.Question;
import com.quizlive.model.enums.AttemptStatus;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ScoringService {

    private final QuestionDao questionDao;
    private final AttemptDao attemptDao;

    public ScoringService() {
        this.questionDao = new QuestionDaoImpl();
        this.attemptDao = new AttemptDaoImpl();
    }

    public ScoringService(QuestionDao questionDao, AttemptDao attemptDao) {
        this.questionDao = questionDao;
        this.attemptDao = attemptDao;
    }

    public Attempt scoreAndSubmit(int attemptId, Map<Integer, Character> userAnswers)
            throws SQLException, InvalidAttemptException {
        Attempt attempt = attemptDao.findById(attemptId);
        if (attempt == null) {
            throw new InvalidAttemptException("Attempt not found with ID: " + attemptId);
        }
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new InvalidAttemptException("Attempt has already been finalized");
        }

        List<Question> questions = questionDao.findByQuizId(attempt.getQuizId());
        int totalScore = 0;
        int maxScore = 0;
        List<AttemptAnswer> answers = new ArrayList<>();

        for (Question q : questions) {
            maxScore += q.getPoints();
            Character selected = (userAnswers != null) ? userAnswers.get(q.getId()) : null;

            boolean correct = false;
            int pointsEarned = 0;

            if (selected != null && Character.toUpperCase(selected) == q.getCorrectOption()) {
                correct = true;
                pointsEarned = q.getPoints();
                totalScore += pointsEarned;
            }

            AttemptAnswer ans = new AttemptAnswer(0, attemptId, q.getId(), selected, correct);
            ans.setPointsEarned(pointsEarned);
            answers.add(ans);
        }

        attempt.setScore(totalScore);
        attempt.setMaxScore(maxScore);
        attempt.setStatus(AttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(new Timestamp(System.currentTimeMillis()));
        attempt.setAnswers(answers);

        boolean updated = attemptDao.submitAttempt(attempt, answers);
        if (!updated) {
            throw new SQLException("Failed to record final attempt submission");
        }

        return attempt;
    }
}
