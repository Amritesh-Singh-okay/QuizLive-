package com.quizlive;

import com.quizlive.concurrency.ActiveAttemptRegistry;
import com.quizlive.concurrency.QuizSchedulerService;
import com.quizlive.dao.AttemptDao;
import com.quizlive.dao.UserDao;
import com.quizlive.dao.impl.AttemptDaoImpl;
import com.quizlive.dao.impl.UserDaoImpl;
import com.quizlive.model.AppUser;
import com.quizlive.model.Attempt;
import com.quizlive.model.enums.AttemptStatus;
import com.quizlive.model.enums.Role;
import com.quizlive.service.QuizService;
import com.quizlive.service.ScoringService;
import com.quizlive.servlet.StartAttemptServlet;
import com.quizlive.servlet.SubmitAttemptServlet;
import com.quizlive.servlet.TabSwitchServlet;
import com.quizlive.util.JsonUtil;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuizAttemptLifecycleServletTest {

    private StartAttemptServlet startServlet;
    private SubmitAttemptServlet submitServlet;
    private TabSwitchServlet tabSwitchServlet;

    private UserDao userDao;
    private AttemptDao attemptDao;
    private QuizSchedulerService schedulerService;

    private AppUser testParticipant;
    private AppUser otherParticipant;

    @BeforeEach
    void setUp() throws SQLException {
        this.userDao = new UserDaoImpl();
        this.attemptDao = new AttemptDaoImpl();
        this.schedulerService = new QuizSchedulerService();
        QuizService quizService = new QuizService();
        ScoringService scoringService = new ScoringService();
        ActiveAttemptRegistry registry = ActiveAttemptRegistry.getInstance();

        this.startServlet = new StartAttemptServlet(attemptDao, quizService, schedulerService);
        this.submitServlet = new SubmitAttemptServlet(scoringService, schedulerService, attemptDao);
        this.tabSwitchServlet = new TabSwitchServlet(registry, attemptDao);

        String email1 = "testattempt_" + System.currentTimeMillis() + "@quizlive.com";
        String email2 = "otherattempt_" + System.currentTimeMillis() + "@quizlive.com";
        this.testParticipant = userDao.create(AppUser.create(0, "Test Runner", email1, "h", "s", Role.PARTICIPANT, null));
        this.otherParticipant = userDao.create(AppUser.create(0, "Other Runner", email2, "h", "s", Role.PARTICIPANT, null));
    }

    @AfterEach
    void tearDown() throws SQLException {
        schedulerService.shutdown();
        if (testParticipant != null && testParticipant.getId() > 0) {
            userDao.delete(testParticipant.getId());
        }
        if (otherParticipant != null && otherParticipant.getId() > 0) {
            userDao.delete(otherParticipant.getId());
        }
    }

    @Test
    @DisplayName("Complete attempt lifecycle: start attempt, record tab switches, and submit answers")
    void testFullAttemptLifecycle() throws Exception {
        TestContext startContext = new TestContext();
        startContext.sessionHolder.attributes.put("user", testParticipant);
        startContext.setContentType("application/json");
        startContext.setBody("{\"quizId\": 1}");

        startServlet.service(startContext.request, startContext.response);

        assertEquals(201, startContext.status);
        Map<?, ?> startBody = JsonUtil.fromJson(startContext.getResponseBody(), Map.class);
        assertTrue((Boolean) startBody.get("success"));

        Map<?, ?> startData = (Map<?, ?>) startBody.get("data");
        assertNotNull(startData);
        int attemptId = ((Number) startData.get("attemptId")).intValue();
        assertTrue(attemptId > 0);

        List<?> questions = (List<?>) startData.get("questions");
        assertNotNull(questions);
        assertEquals(4, questions.size());

        TestContext resumeContext = new TestContext();
        resumeContext.sessionHolder.attributes.put("user", testParticipant);
        resumeContext.setContentType("application/json");
        resumeContext.setBody(String.format("{\"quizId\": 1}"));

        startServlet.service(resumeContext.request, resumeContext.response);
        assertEquals(200, resumeContext.status);

        for (int i = 1; i <= 3; i++) {
            TestContext tabContext = new TestContext();
            tabContext.sessionHolder.attributes.put("user", testParticipant);
            tabContext.setContentType("application/json");
            tabContext.setBody(String.format("{\"attemptId\": %d}", attemptId));

            tabSwitchServlet.service(tabContext.request, tabContext.response);
            assertEquals(200, tabContext.status);

            Map<?, ?> tabBody = JsonUtil.fromJson(tabContext.getResponseBody(), Map.class);
            Map<?, ?> tabData = (Map<?, ?>) tabBody.get("data");
            assertEquals(i, ((Number) tabData.get("tabSwitches")).intValue());
            if (i == 3) {
                assertNotNull(tabData.get("warning"));
            }
        }

        TestContext submitContext = new TestContext();
        submitContext.sessionHolder.attributes.put("user", testParticipant);
        submitContext.setContentType("application/json");
        String submitJson = String.format("""
            {
              "attemptId": %d,
              "answers": {
                "1": "B",
                "2": "B",
                "3": "B",
                "4": "C"
              }
            }
            """, attemptId);
        submitContext.setBody(submitJson);

        submitServlet.service(submitContext.request, submitContext.response);

        assertEquals(200, submitContext.status);
        Map<?, ?> submitBody = JsonUtil.fromJson(submitContext.getResponseBody(), Map.class);
        assertTrue((Boolean) submitBody.get("success"));

        Map<?, ?> submitData = (Map<?, ?>) submitBody.get("data");
        assertNotNull(submitData);
        assertEquals(4, ((Number) submitData.get("score")).intValue());
        assertEquals(4, ((Number) submitData.get("maxScore")).intValue());
        assertEquals(100.0, ((Number) submitData.get("percentage")).doubleValue());
        assertEquals("SUBMITTED", submitData.get("status"));
        assertEquals(3, ((Number) submitData.get("tabSwitches")).intValue());

        Attempt persisted = attemptDao.findById(attemptId);
        assertNotNull(persisted);
        assertEquals(AttemptStatus.SUBMITTED, persisted.getStatus());
        assertEquals(4, persisted.getScore());
        assertEquals(3, persisted.getTabSwitches());
    }

    @Test
    @DisplayName("SubmitAttemptServlet rejects unauthenticated, unauthorized, and duplicate submissions")
    void testSubmitAttemptValidation() throws Exception {
        Attempt attempt = attemptDao.startAttempt(1, testParticipant.getId());
        int attemptId = attempt.getId();

        TestContext unauthContext = new TestContext();
        unauthContext.setContentType("application/json");
        unauthContext.setBody(String.format("{\"attemptId\": %d}", attemptId));
        submitServlet.service(unauthContext.request, unauthContext.response);
        assertEquals(401, unauthContext.status);

        TestContext foreignContext = new TestContext();
        foreignContext.sessionHolder.attributes.put("user", otherParticipant);
        foreignContext.setContentType("application/json");
        foreignContext.setBody(String.format("{\"attemptId\": %d}", attemptId));
        submitServlet.service(foreignContext.request, foreignContext.response);
        assertEquals(403, foreignContext.status);

        TestContext validContext = new TestContext();
        validContext.sessionHolder.attributes.put("user", testParticipant);
        validContext.setContentType("application/json");
        validContext.setBody(String.format("{\"attemptId\": %d, \"answers\": {\"1\":\"B\"}}", attemptId));
        submitServlet.service(validContext.request, validContext.response);
        assertEquals(200, validContext.status);

        TestContext duplicateContext = new TestContext();
        duplicateContext.sessionHolder.attributes.put("user", testParticipant);
        duplicateContext.setContentType("application/json");
        duplicateContext.setBody(String.format("{\"attemptId\": %d, \"answers\": {\"1\":\"B\"}}", attemptId));
        submitServlet.service(duplicateContext.request, duplicateContext.response);
        assertEquals(400, duplicateContext.status);
    }

    private static class TestContext {
        private String method = "POST";
        private String contextPath = "/quizlive";
        private String contentType = null;
        private String body = "";
        private final Map<String, String> headers = new HashMap<>();
        private final Map<String, String[]> parameters = new HashMap<>();
        private int status = 200;
        private String redirectUrl;
        private final StringWriter responseWriter = new StringWriter();
        private final SessionHolder sessionHolder = new SessionHolder();

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
                        case "invalidate" -> {
                            sessionHolder.invalidated = true;
                            sessionHolder.attributes.clear();
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
                        case "sendRedirect" -> {
                            redirectUrl = (String) args[0];
                            yield null;
                        }
                        case "getWriter" -> new PrintWriter(responseWriter);
                        case "setContentType", "setCharacterEncoding" -> null;
                        default -> defaultPrimitive(m.getReturnType());
                    }
            );
        }

        void setMethod(String method) {
            this.method = method;
        }

        void setContentType(String contentType) {
            this.contentType = contentType;
        }

        void setBody(String body) {
            this.body = body;
        }

        void setHeader(String name, String value) {
            headers.put(name, value);
        }

        String getResponseBody() {
            return responseWriter.toString();
        }
    }

    private static class SessionHolder {
        final Map<String, Object> attributes = new HashMap<>();
        boolean invalidated = false;
    }

    private static Object defaultPrimitive(Class<?> returnType) {
        if (returnType.equals(boolean.class)) return false;
        if (returnType.equals(int.class)) return 0;
        if (returnType.equals(long.class)) return 0L;
        if (returnType.equals(double.class)) return 0.0;
        if (returnType.equals(float.class)) return 0.0f;
        return null;
    }
}
