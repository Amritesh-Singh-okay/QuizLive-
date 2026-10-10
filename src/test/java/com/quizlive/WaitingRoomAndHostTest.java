package com.quizlive;

import com.quizlive.dao.AttemptDao;
import com.quizlive.dao.QuizDao;
import com.quizlive.dao.impl.AttemptDaoImpl;
import com.quizlive.dao.impl.QuizDaoImpl;
import com.quizlive.model.AppUser;
import com.quizlive.model.Question;
import com.quizlive.model.Quiz;
import com.quizlive.model.enums.QuizStatus;
import com.quizlive.model.enums.Role;
import com.quizlive.service.QuizService;
import com.quizlive.servlet.HostQuizServlet;
import com.quizlive.servlet.StartAttemptServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaitingRoomAndHostTest {

    private QuizService quizService;
    private QuizDao quizDao;
    private AttemptDao attemptDao;
    private HostQuizServlet hostQuizServlet;
    private StartAttemptServlet startAttemptServlet;

    private AppUser creatorUser;
    private AppUser participantUser;
    private AppUser unauthorizedUser;
    private final List<Integer> createdQuizIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        this.quizDao = new QuizDaoImpl();
        this.attemptDao = new AttemptDaoImpl();
        this.quizService = new QuizService();
        this.hostQuizServlet = new HostQuizServlet(this.quizService);
        this.startAttemptServlet = new StartAttemptServlet();

        this.creatorUser = AppUser.create(2, "Professor Smith", "creator1@quizlive.com", "hash", "salt", Role.CREATOR, null);
        this.participantUser = AppUser.create(3, "Alice Student", "student1@quizlive.com", "hash", "salt", Role.PARTICIPANT, null);
        this.unauthorizedUser = AppUser.create(4, "Bob Student", "student2@quizlive.com", "hash", "salt", Role.PARTICIPANT, null);
    }

    @AfterEach
    void tearDown() throws SQLException {
        for (int id : createdQuizIds) {
            quizDao.delete(id);
        }
        createdQuizIds.clear();
    }

    private Quiz createApprovedQuiz(boolean held, Timestamp scheduledAt) throws SQLException {
        Quiz quiz = new Quiz();
        quiz.setTitle("Classroom Live Quiz " + System.currentTimeMillis());
        quiz.setDescription("Unit test assessment for waiting room");
        quiz.setCreatorId(creatorUser.getId());
        quiz.setDurationSeconds(300);
        quiz.setStatus(QuizStatus.PENDING);
        quiz.setHeld(held);
        quiz.setScheduledStartAt(scheduledAt);

        Question q = new Question();
        q.setQuestionText("What is 2 + 2?");
        q.setPoints(10);
        q.setOptionA("1");
        q.setOptionB("2");
        q.setOptionC("4");
        q.setOptionD("5");
        q.setCorrectOption('C');
        quiz.addQuestion(q);

        Quiz created = quizService.createQuiz(quiz);
        quizService.approveQuiz(created.getId());
        createdQuizIds.add(created.getId());
        return created;
    }

    @Test
    @DisplayName("canParticipantsStartNow correctly evaluates isHeld and scheduled start timestamps")
    void testCanParticipantsStartNow() {
        Quiz quiz = new Quiz();
        quiz.setHeld(false);
        quiz.setScheduledStartAt(null);
        assertTrue(quiz.canParticipantsStartNow(), "Free quiz should allow immediate starting");

        quiz.setHeld(true);
        assertFalse(quiz.canParticipantsStartNow(), "Held quiz should NOT allow starting");

        quiz.setHeld(false);
        // Scheduled 1 hour in future
        quiz.setScheduledStartAt(new Timestamp(System.currentTimeMillis() + 3600000));
        assertFalse(quiz.canParticipantsStartNow(), "Future scheduled quiz should NOT allow starting");

        // Scheduled 1 minute in past
        quiz.setScheduledStartAt(new Timestamp(System.currentTimeMillis() - 60000));
        assertTrue(quiz.canParticipantsStartNow(), "Past scheduled quiz should allow starting");
    }

    @Test
    @DisplayName("Host servlet returns lobby status via GET")
    void testGetLobbyStatus() throws Exception {
        Quiz quiz = createApprovedQuiz(true, null);

        TestContext ctx = new TestContext();
        ctx.method = "GET";
        ctx.parameters.put("quizId", new String[]{String.valueOf(quiz.getId())});

        hostQuizServlet.service(ctx.request, ctx.response);

        assertEquals(200, ctx.status);
        String resp = ctx.responseWriter.toString();
        assertTrue(resp.contains("\"success\":true"));
        assertTrue(resp.contains("\"isHeld\":true"));
        assertTrue(resp.contains("\"canStart\":false"));
        assertTrue(resp.contains("Classroom Live Quiz"));
    }

    @Test
    @DisplayName("StartAttemptServlet returns HTTP 423 Locked when quiz is held")
    void testStartAttemptLockedWhenHeld() throws Exception {
        Quiz quiz = createApprovedQuiz(true, null);

        TestContext ctx = new TestContext();
        ctx.method = "POST";
        ctx.sessionHolder.attributes.put("user", participantUser);
        ctx.contentType = "application/json";
        ctx.body = "{\"quizId\":" + quiz.getId() + "}";

        startAttemptServlet.service(ctx.request, ctx.response);

        assertEquals(423, ctx.status, "Should return HTTP 423 Locked when quiz is held in lobby");
        String resp = ctx.responseWriter.toString();
        assertTrue(resp.contains("\"waitingRoom\":true"));
        assertTrue(resp.contains("waiting room"));
    }

    @Test
    @DisplayName("Host can start held quiz, unlocking participants into the assessment")
    void testHostStartAndParticipantUnlock() throws Exception {
        Quiz quiz = createApprovedQuiz(true, null);

        // 1. Participant tries to start -> locked (423)
        TestContext partCtx1 = new TestContext();
        partCtx1.method = "POST";
        partCtx1.sessionHolder.attributes.put("user", participantUser);
        partCtx1.contentType = "application/json";
        partCtx1.body = "{\"quizId\":" + quiz.getId() + "}";
        startAttemptServlet.service(partCtx1.request, partCtx1.response);
        assertEquals(423, partCtx1.status);

        // 2. Host clicks 'Start Quiz Now'
        TestContext hostCtx = new TestContext();
        hostCtx.method = "POST";
        hostCtx.sessionHolder.attributes.put("user", creatorUser);
        hostCtx.contentType = "application/json";
        hostCtx.body = "{\"quizId\":" + quiz.getId() + ",\"action\":\"start\"}";
        hostQuizServlet.service(hostCtx.request, hostCtx.response);
        assertEquals(200, hostCtx.status);
        assertTrue(hostCtx.responseWriter.toString().contains("\"started\":true"));

        // 3. Quiz status in DB is now unheld
        Quiz refreshed = quizDao.findById(quiz.getId());
        assertFalse(refreshed.isHeld());

        // 4. Participant retries -> succeeds with 201 Created!
        TestContext partCtx2 = new TestContext();
        partCtx2.method = "POST";
        partCtx2.sessionHolder.attributes.put("user", participantUser);
        partCtx2.contentType = "application/json";
        partCtx2.body = "{\"quizId\":" + quiz.getId() + "}";
        startAttemptServlet.service(partCtx2.request, partCtx2.response);
        assertEquals(201, partCtx2.status);
        assertTrue(partCtx2.responseWriter.toString().contains("\"attemptId\""));
    }

    @Test
    @DisplayName("Host can hold a live quiz session putting it back into lobby mode")
    void testHostHoldSession() throws Exception {
        Quiz quiz = createApprovedQuiz(false, null);

        TestContext hostCtx = new TestContext();
        hostCtx.method = "POST";
        hostCtx.sessionHolder.attributes.put("user", creatorUser);
        hostCtx.contentType = "application/json";
        hostCtx.body = "{\"quizId\":" + quiz.getId() + ",\"action\":\"hold\"}";
        hostQuizServlet.service(hostCtx.request, hostCtx.response);

        assertEquals(200, hostCtx.status);
        assertTrue(hostCtx.responseWriter.toString().contains("\"isHeld\":true"));

        Quiz refreshed = quizDao.findById(quiz.getId());
        assertTrue(refreshed.isHeld());
    }

    @Test
    @DisplayName("Unauthorized users cannot trigger host actions")
    void testUnauthorizedHostActions() throws Exception {
        Quiz quiz = createApprovedQuiz(true, null);

        // Participant role cannot manage host session -> 403
        TestContext unauthCtx = new TestContext();
        unauthCtx.method = "POST";
        unauthCtx.sessionHolder.attributes.put("user", unauthorizedUser);
        unauthCtx.contentType = "application/json";
        unauthCtx.body = "{\"quizId\":" + quiz.getId() + ",\"action\":\"start\"}";
        hostQuizServlet.service(unauthCtx.request, unauthCtx.response);

        assertEquals(403, unauthCtx.status);

        // Anonymous user -> 401
        TestContext anonCtx = new TestContext();
        anonCtx.method = "POST";
        anonCtx.contentType = "application/json";
        anonCtx.body = "{\"quizId\":" + quiz.getId() + ",\"action\":\"start\"}";
        hostQuizServlet.service(anonCtx.request, anonCtx.response);

        assertEquals(401, anonCtx.status);
    }

    // ==========================================
    // Test Mock Context
    // ==========================================

    private static class SessionHolder {
        final Map<String, Object> attributes = new HashMap<>();
    }

    private static class TestContext {
        String method = "GET";
        String contextPath = "/quizlive";
        String requestUri = "/quizlive/api/quizzes/host";
        String contentType;
        String body = "";
        final Map<String, String> headers = new HashMap<>();
        final Map<String, String[]> parameters = new HashMap<>();
        int status = 200;
        final StringWriter responseWriter = new StringWriter();
        final SessionHolder sessionHolder = new SessionHolder();

        final HttpServletRequest request;
        final HttpServletResponse response;
        final HttpSession session;

        TestContext() {
            this.session = (HttpSession) Proxy.newProxyInstance(
                    HttpSession.class.getClassLoader(),
                    new Class<?>[]{HttpSession.class},
                    (proxy, m, args) -> switch (m.getName()) {
                        case "getAttribute" -> sessionHolder.attributes.get((String) args[0]);
                        case "setAttribute" -> {
                            sessionHolder.attributes.put((String) args[0], args[1]);
                            yield null;
                        }
                        case "removeAttribute" -> {
                            sessionHolder.attributes.remove((String) args[0]);
                            yield null;
                        }
                        default -> defaultPrimitive(m.getReturnType());
                    }
            );

            this.request = (HttpServletRequest) Proxy.newProxyInstance(
                    HttpServletRequest.class.getClassLoader(),
                    new Class<?>[]{HttpServletRequest.class},
                    (proxy, m, args) -> switch (m.getName()) {
                        case "getMethod" -> method;
                        case "getContextPath" -> contextPath;
                        case "getRequestURI" -> requestUri;
                        case "getContentType" -> contentType;
                        case "getHeader" -> headers.get((String) args[0]);
                        case "getParameter" -> {
                            String[] vals = parameters.get((String) args[0]);
                            yield (vals != null && vals.length > 0) ? vals[0] : null;
                        }
                        case "getParameterMap" -> parameters;
                        case "getReader" -> new BufferedReader(new StringReader(body));
                        case "getSession" -> session;
                        default -> defaultPrimitive(m.getReturnType());
                    }
            );

            this.response = (HttpServletResponse) Proxy.newProxyInstance(
                    HttpServletResponse.class.getClassLoader(),
                    new Class<?>[]{HttpServletResponse.class},
                    (proxy, m, args) -> switch (m.getName()) {
                        case "setStatus" -> {
                            status = (int) args[0];
                            yield null;
                        }
                        case "getStatus" -> status;
                        case "getWriter" -> new PrintWriter(responseWriter);
                        case "setContentType", "setCharacterEncoding" -> null;
                        default -> defaultPrimitive(m.getReturnType());
                    }
            );
        }

        private static Object defaultPrimitive(Class<?> returnType) {
            if (returnType == boolean.class) return false;
            if (returnType == int.class) return 0;
            if (returnType == long.class) return 0L;
            return null;
        }
    }
}
