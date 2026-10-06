package com.quizlive;

import com.quizlive.dao.AttemptDao;
import com.quizlive.dao.QuestionDao;
import com.quizlive.dao.QuizDao;
import com.quizlive.dao.impl.AttemptDaoImpl;
import com.quizlive.dao.impl.QuestionDaoImpl;
import com.quizlive.dao.impl.QuizDaoImpl;
import com.quizlive.exception.InvalidAttemptException;
import com.quizlive.exception.QuizClosedException;
import com.quizlive.model.Attempt;
import com.quizlive.model.Question;
import com.quizlive.model.Quiz;
import com.quizlive.service.QuizService;
import com.quizlive.service.ScoringService;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuizAndScoringServiceTest {

    private QuizService quizService;
    private ScoringService scoringService;
    private AttemptDao attemptDao;

    @BeforeEach
    void setUp() {
        QuestionDao questionDao = new QuestionDaoImpl();
        QuizDao quizDao = new QuizDaoImpl(questionDao);
        this.attemptDao = new AttemptDaoImpl();

        this.quizService = new QuizService(quizDao, questionDao);
        this.scoringService = new ScoringService(questionDao, attemptDao);
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
        // Start a test attempt on Quiz 1 for user 5 (Charlie)
        Attempt attempt = attemptDao.findByQuizAndUser(1, 5);
        if (attempt == null) {
            attempt = attemptDao.startAttempt(1, 5);
        }

        // Quiz 1 true answers from seed: Q1: B, Q2: B, Q3: B, Q4: C
        Map<Integer, Character> userAnswers = new HashMap<>();
        userAnswers.put(1, 'B'); // correct (1pt)
        userAnswers.put(2, 'B'); // correct (1pt)
        userAnswers.put(3, 'A'); // wrong (0pt)
        userAnswers.put(4, 'C'); // correct (1pt)

        Attempt finalized = scoringService.scoreAndSubmit(attempt.getId(), userAnswers);
        assertNotNull(finalized);
        assertEquals(3, finalized.getScore());
        assertEquals(4, finalized.getMaxScore());
        assertEquals(75.0, finalized.getPercentage());

        // Re-submission should fail
        assertThrows(InvalidAttemptException.class, () ->
                scoringService.scoreAndSubmit(finalized.getId(), userAnswers)
        );
    }
}
