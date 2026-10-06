package com.quizlive;

import com.quizlive.dao.AttemptDao;
import com.quizlive.dao.QuestionDao;
import com.quizlive.dao.QuizDao;
import com.quizlive.dao.UserDao;
import com.quizlive.dao.impl.AttemptDaoImpl;
import com.quizlive.dao.impl.QuestionDaoImpl;
import com.quizlive.dao.impl.QuizDaoImpl;
import com.quizlive.dao.impl.UserDaoImpl;
import com.quizlive.exception.InvalidAttemptException;
import com.quizlive.exception.QuizClosedException;
import com.quizlive.model.AppUser;
import com.quizlive.model.Attempt;
import com.quizlive.model.Question;
import com.quizlive.model.Quiz;
import com.quizlive.model.enums.Role;
import com.quizlive.service.QuizService;
import com.quizlive.service.ScoringService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuizAndScoringServiceTest {

    private QuizService quizService;
    private ScoringService scoringService;
    private AttemptDao attemptDao;
    private UserDao userDao;
    private int tempUserId = 0;

    @BeforeEach
    void setUp() {
        QuestionDao questionDao = new QuestionDaoImpl();
        QuizDao quizDao = new QuizDaoImpl(questionDao);
        this.attemptDao = new AttemptDaoImpl();
        this.userDao = new UserDaoImpl();

        this.quizService = new QuizService(quizDao, questionDao);
        this.scoringService = new ScoringService(questionDao, attemptDao);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (tempUserId > 0) {
            userDao.delete(tempUserId);
            tempUserId = 0;
        }
    }

    @Test
    @DisplayName("Verify getQuizForTaking strips answer keys from questions")
    void testGetQuizForTakingSanitization() throws SQLException, QuizClosedException {
        Quiz quiz = quizService.getQuizForTaking(1);
        assertNotNull(quiz);
        assertFalse(quiz.getQuestions().isEmpty());

        for (Question q : quiz.getQuestions()) {
            assertEquals(' ', q.getCorrectOption(), "Answer keys must be stripped for participants");
        }
    }

    @Test
    @DisplayName("Verify unapproved quiz throws QuizClosedException")
    void testUnapprovedQuizThrows() {
        assertThrows(QuizClosedException.class, () ->
                quizService.getQuizForTaking(3)
        );
    }

    @Test
    @DisplayName("Verify server-side scoring computes exact score and rejects double submissions")
    void testScoringAndDoubleSubmit() throws SQLException, InvalidAttemptException {
        String email = "scoring_temp_" + System.currentTimeMillis() + "@quizlive.com";
        AppUser tempUser = userDao.create(AppUser.create(0, "Temp Scoring User", email, "hash", "salt", Role.PARTICIPANT, null));
        tempUserId = tempUser.getId();

        Attempt attempt = attemptDao.startAttempt(1, tempUserId);

        Map<Integer, Character> userAnswers = new HashMap<>();
        userAnswers.put(1, 'B');
        userAnswers.put(2, 'B');
        userAnswers.put(3, 'A');
        userAnswers.put(4, 'C');

        Attempt finalized = scoringService.scoreAndSubmit(attempt.getId(), userAnswers);
        assertNotNull(finalized);
        assertEquals(3, finalized.getScore());
        assertEquals(4, finalized.getMaxScore());
        assertEquals(75.0, finalized.getPercentage());

        assertThrows(InvalidAttemptException.class, () ->
                scoringService.scoreAndSubmit(finalized.getId(), userAnswers)
        );
    }
}
