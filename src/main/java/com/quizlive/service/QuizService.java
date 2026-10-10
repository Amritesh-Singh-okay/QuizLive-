package com.quizlive.service;

import com.quizlive.dao.QuestionDao;
import com.quizlive.dao.QuizDao;
import com.quizlive.dao.impl.QuestionDaoImpl;
import com.quizlive.dao.impl.QuizDaoImpl;
import com.quizlive.exception.QuizClosedException;
import com.quizlive.model.Question;
import com.quizlive.model.Quiz;
import com.quizlive.model.enums.QuizStatus;

import java.sql.SQLException;
import java.util.List;

public class QuizService {

    private final QuizDao quizDao;
    private final QuestionDao questionDao;

    public QuizService() {
        this.questionDao = new QuestionDaoImpl();
        this.quizDao = new QuizDaoImpl(this.questionDao);
    }

    public QuizService(QuizDao quizDao, QuestionDao questionDao) {
        this.quizDao = quizDao;
        this.questionDao = questionDao;
    }

    public Quiz createQuiz(Quiz quiz) throws SQLException {
        if (quiz == null) {
            throw new IllegalArgumentException("Quiz cannot be null");
        }
        if (quiz.getTitle() == null || quiz.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Quiz title is required");
        }
        if (quiz.getDurationSeconds() < 30) {
            throw new IllegalArgumentException("Duration must be at least 30 seconds");
        }
        if (quiz.getQuestions() == null || quiz.getQuestions().isEmpty()) {
            throw new IllegalArgumentException("A quiz must have at least one question");
        }

        for (Question q : quiz.getQuestions()) {
            if (q.getQuestionText() == null || q.getQuestionText().trim().isEmpty()) {
                throw new IllegalArgumentException("Question text cannot be empty");
            }
            if (q.getOptionA() == null || q.getOptionB() == null || q.getOptionC() == null || q.getOptionD() == null) {
                throw new IllegalArgumentException("All 4 options (A, B, C, D) are required");
            }
            char opt = Character.toUpperCase(q.getCorrectOption());
            if (opt != 'A' && opt != 'B' && opt != 'C' && opt != 'D') {
                throw new IllegalArgumentException("Correct option must be A, B, C, or D");
            }
            q.setCorrectOption(opt);
        }

        return quizDao.createWithQuestions(quiz);
    }

    public boolean approveQuiz(int quizId) throws SQLException {
        return quizDao.updateStatus(quizId, QuizStatus.APPROVED);
    }

    public boolean rejectQuiz(int quizId) throws SQLException {
        return quizDao.updateStatus(quizId, QuizStatus.REJECTED);
    }

    public List<Quiz> getApprovedQuizzes() throws SQLException {
        return quizDao.listApproved();
    }

    public List<Quiz> getQuizzesByCreator(int creatorId) throws SQLException {
        return quizDao.listByCreator(creatorId);
    }

    public List<Quiz> getPendingQuizzes() throws SQLException {
        return quizDao.listByStatus(QuizStatus.PENDING);
    }

    public Quiz getQuizForTaking(int quizId) throws SQLException, QuizClosedException {
        Quiz quiz = quizDao.findById(quizId);
        if (quiz == null || quiz.getStatus() != QuizStatus.APPROVED) {
            throw new QuizClosedException("Quiz is not available or has not been approved");
        }

        List<Question> sanitizedQuestions = questionDao.findByQuizIdSanitized(quizId);
        quiz.setQuestions(sanitizedQuestions);
        return quiz;
    }

    public Quiz findById(int quizId) throws SQLException {
        return quizDao.findById(quizId);
    }

    public Quiz getQuizWithAnswers(int quizId) throws SQLException {
        return quizDao.findByIdWithQuestions(quizId);
    }

    public boolean startQuizSession(int quizId, int requestingUserId, boolean isAdmin)
            throws SQLException, com.quizlive.exception.UnauthorizedException {
        Quiz quiz = quizDao.findById(quizId);
        if (quiz == null) {
            throw new IllegalArgumentException("Quiz not found with ID: " + quizId);
        }

        if (!isAdmin && quiz.getCreatorId() != requestingUserId) {
            throw new com.quizlive.exception.UnauthorizedException("Only the quiz creator or an administrator can start this live session");
        }

        boolean updated = quizDao.updateHeldStatus(quizId, false);
        if (updated) {
            quizDao.updateScheduledStart(quizId, null);
            com.quizlive.websocket.WaitingRoomEndpoint.broadcastQuizStarted(quizId);
        }
        return updated;
    }

    public boolean holdQuizSession(int quizId, int requestingUserId, boolean isAdmin)
            throws SQLException, com.quizlive.exception.UnauthorizedException {
        Quiz quiz = quizDao.findById(quizId);
        if (quiz == null) {
            throw new IllegalArgumentException("Quiz not found with ID: " + quizId);
        }

        if (!isAdmin && quiz.getCreatorId() != requestingUserId) {
            throw new com.quizlive.exception.UnauthorizedException("Only the quiz creator or an administrator can hold this quiz session");
        }

        boolean updated = quizDao.updateHeldStatus(quizId, true);
        if (updated) {
            com.quizlive.websocket.WaitingRoomEndpoint.broadcastQuizHeld(quizId);
        }
        return updated;
    }

    public boolean updateScheduledStart(int quizId, java.sql.Timestamp scheduledStartAt, int requestingUserId, boolean isAdmin)
            throws SQLException, com.quizlive.exception.UnauthorizedException {
        Quiz quiz = quizDao.findById(quizId);
        if (quiz == null) {
            throw new IllegalArgumentException("Quiz not found with ID: " + quizId);
        }

        if (!isAdmin && quiz.getCreatorId() != requestingUserId) {
            throw new com.quizlive.exception.UnauthorizedException("Only the quiz creator or an administrator can schedule this quiz");
        }

        return quizDao.updateScheduledStart(quizId, scheduledStartAt);
    }
}
