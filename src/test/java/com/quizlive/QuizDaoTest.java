package com.quizlive;

import com.quizlive.dao.QuestionDao;
import com.quizlive.dao.QuizDao;
import com.quizlive.dao.impl.QuestionDaoImpl;
import com.quizlive.dao.impl.QuizDaoImpl;
import com.quizlive.model.Question;
import com.quizlive.model.Quiz;
import com.quizlive.model.enums.QuizStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuizDaoTest {

    private QuizDao quizDao;
    private QuestionDao questionDao;

    @BeforeEach
    void setUp() {
        this.questionDao = new QuestionDaoImpl();
        this.quizDao = new QuizDaoImpl(questionDao);
    }

    @Test
    @DisplayName("Verify findByIdWithQuestions on seeded Quiz 1")
    void testFindByIdWithQuestions() throws SQLException {
        Quiz quiz = quizDao.findByIdWithQuestions(1);
        assertNotNull(quiz);
        assertEquals("Core Java Fundamentals", quiz.getTitle());
        assertEquals("Prof. Arvind Sharma", quiz.getCreatorName());
        assertEquals(4, quiz.getQuestions().size());
        assertEquals(4, quiz.getTotalPoints());
    }

    @Test
    @DisplayName("Verify listApproved filters correctly")
    void testListApproved() throws SQLException {
        List<Quiz> approved = quizDao.listApproved();
        assertFalse(approved.isEmpty());
        for (Quiz q : approved) {
            assertEquals(QuizStatus.APPROVED, q.getStatus());
        }
    }

    @Test
    @DisplayName("Verify transactional creation of Quiz and Questions, status update, and delete")
    void testQuizTransactionLifecycle() throws SQLException {
        Quiz newQuiz = new Quiz(0, "Test Transaction Quiz", "Testing atomic inserts", 2, 180, QuizStatus.PENDING, null);

        Question q1 = new Question(0, 0, "What is JDBC?", "Database Connectivity", "Compiler", "OS", "Browser", 'A', 2);
        Question q2 = new Question(0, 0, "What is ACID?", "Atomicity, Consistency, Isolation, Durability", "None", "All", "Acidic", 'A', 3);

        newQuiz.addQuestion(q1);
        newQuiz.addQuestion(q2);

        Quiz created = quizDao.createWithQuestions(newQuiz);
        assertTrue(created.getId() > 0);
        assertTrue(q1.getId() > 0);
        assertTrue(q2.getId() > 0);
        assertEquals(created.getId(), q1.getQuizId());
        assertEquals(created.getId(), q2.getQuizId());

        boolean statusUpdated = quizDao.updateStatus(created.getId(), QuizStatus.APPROVED);
        assertTrue(statusUpdated);
        assertEquals(QuizStatus.APPROVED, quizDao.findById(created.getId()).getStatus());

        List<Question> savedQuestions = questionDao.findByQuizId(created.getId());
        assertEquals(2, savedQuestions.size());

        boolean deleted = quizDao.delete(created.getId());
        assertTrue(deleted);
        assertNull(quizDao.findById(created.getId()));

        List<Question> afterCascade = questionDao.findByQuizId(created.getId());
        assertTrue(afterCascade.isEmpty(), "Questions should be deleted via ON DELETE CASCADE");
    }
}
