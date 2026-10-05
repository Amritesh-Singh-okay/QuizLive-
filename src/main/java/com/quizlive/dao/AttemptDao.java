package com.quizlive.dao;

import com.quizlive.model.Attempt;
import com.quizlive.model.AttemptAnswer;

import java.sql.SQLException;
import java.util.List;

public interface AttemptDao {

    Attempt startAttempt(int quizId, int userId) throws SQLException;

    Attempt findById(int attemptId) throws SQLException;

    Attempt findByQuizAndUser(int quizId, int userId) throws SQLException;

    boolean submitAttempt(Attempt attempt, List<AttemptAnswer> answers) throws SQLException;

    boolean incrementTabSwitches(int attemptId) throws SQLException;

    List<Attempt> getLeaderboard(int quizId) throws SQLException;

    List<Attempt> listByUser(int userId) throws SQLException;

    List<Attempt> listByQuiz(int quizId) throws SQLException;
}
