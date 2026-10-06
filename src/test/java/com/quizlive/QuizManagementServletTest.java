package com.quizlive;

import com.quizlive.dao.QuizDao;
import com.quizlive.dao.impl.QuizDaoImpl;
import com.quizlive.model.AppUser;
import com.quizlive.model.Question;
import com.quizlive.model.Quiz;
import com.quizlive.model.enums.QuizStatus;
import com.quizlive.model.enums.Role;
import com.quizlive.servlet.ApproveQuizServlet;
import com.quizlive.servlet.CreateQuizServlet;
import com.quizlive.servlet.ListQuizzesServlet;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuizManagementServletTest {

    private CreateQuizServlet createQuizServlet;
    private ApproveQuizServlet approveQuizServlet;
    private ListQuizzesServlet listQuizzesServlet;
    private QuizDao quizDao;
    private final List<Integer> createdQuizIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        this.createQuizServlet = new CreateQuizServlet();
        this.approveQuizServlet = new ApproveQuizServlet();
        this.listQuizzesServlet = new ListQuizzesServlet();
        this.quizDao = new QuizDaoImpl();
    }

    @AfterEach
    void tearDown() throws SQLException {
        for (int id : createdQuizIds) {
            quizDao.delete(id);
        }
        createdQuizIds.clear();
    }

    @Test
    @DisplayName("CreateQuizServlet allows creator to create quiz with questions")
    void testCreateQuizSuccess() throws Exception {
        AppUser creator = AppUser.create(2, "Creator", "creator@quizlive.com", "h", "s", Role.CREATOR, null);
        TestContext context = new TestContext();
        context.sessionHolder.attributes.put("user", creator);
        context.setContentType("application/json");

        String quizJson = """
            {
              "title": "Servlet Integration Quiz",
              "description": "A quiz created via CreateQuizServlet",
              "durationSeconds": 180,
              "questions": [
                {
                  "questionText": "What does HTTP status 201 indicate?",
                  "optionA": "Bad Request",
                  "optionB": "Created",
                  "optionC": "Unauthorized",
                  "optionD": "Forbidden",
                  "correctOption": "B",
                  "points": 2
                }
              ]
            }
            """;
        context.setBody(quizJson);

        createQuizServlet.service(context.request, context.response);

        assertEquals(201, context.status);
        Map<?, ?> body = JsonUtil.fromJson(context.getResponseBody(), Map.class);
        assertTrue((Boolean) body.get("success"));

        Map<?, ?> data = (Map<?, ?>) body.get("data");
        assertNotNull(data);
        int quizId = ((Number) data.get("id")).intValue();
        assertTrue(quizId > 0);
        createdQuizIds.add(quizId);
        assertEquals("Servlet Integration Quiz", data.get("title"));
    }

    @Test
    @DisplayName("CreateQuizServlet rejects unauthorized and participant users")
    void testCreateQuizAccessControl() throws Exception {
        TestContext unauthContext = new TestContext();
        unauthContext.setContentType("application/json");
        unauthContext.setBody("{\"title\":\"No Auth\"}");
        createQuizServlet.service(unauthContext.request, unauthContext.response);
        assertEquals(401, unauthContext.status);

        AppUser student = AppUser.create(3, "Student", "student@quizlive.com", "h", "s", Role.PARTICIPANT, null);
        TestContext forbiddenContext = new TestContext();
        forbiddenContext.sessionHolder.attributes.put("user", student);
        forbiddenContext.setContentType("application/json");
        forbiddenContext.setBody("{\"title\":\"Student Quiz\"}");
        createQuizServlet.service(forbiddenContext.request, forbiddenContext.response);
        assertEquals(403, forbiddenContext.status);
    }

    @Test
    @DisplayName("ApproveQuizServlet allows admin to approve or reject quizzes")
    void testApproveAndRejectQuiz() throws Exception {
        Quiz tempQuiz = new Quiz(0, "Temp Quiz", "Desc", 2, 120, QuizStatus.PENDING, null);
        tempQuiz.addQuestion(new Question(0, 0, "Q1?", "A", "B", "C", "D", 'A', 1));
        Quiz created = quizDao.createWithQuestions(tempQuiz);
        int quizId = created.getId();
        createdQuizIds.add(quizId);

        AppUser admin = AppUser.create(1, "Admin", "admin@quizlive.com", "h", "s", Role.ADMIN, null);

        TestContext approveContext = new TestContext();
        approveContext.sessionHolder.attributes.put("user", admin);
        approveContext.setContentType("application/json");
        approveContext.setBody(String.format("{\"quizId\": %d, \"action\": \"approve\"}", quizId));

        approveQuizServlet.service(approveContext.request, approveContext.response);
        assertEquals(200, approveContext.status);
        Quiz approvedQuiz = quizDao.findById(quizId);
        assertEquals(QuizStatus.APPROVED, approvedQuiz.getStatus());

        TestContext rejectContext = new TestContext();
        rejectContext.sessionHolder.attributes.put("user", admin);
        rejectContext.setContentType("application/json");
        rejectContext.setBody(String.format("{\"quizId\": %d, \"action\": \"reject\"}", quizId));

        approveQuizServlet.service(rejectContext.request, rejectContext.response);
        assertEquals(200, rejectContext.status);
        Quiz rejectedQuiz = quizDao.findById(quizId);
        assertEquals(QuizStatus.REJECTED, rejectedQuiz.getStatus());
    }

    @Test
    @DisplayName("ApproveQuizServlet rejects non-admin users with 403 Forbidden")
    void testApproveQuizForbiddenForNonAdmin() throws Exception {
        AppUser creator = AppUser.create(2, "Creator", "creator@quizlive.com", "h", "s", Role.CREATOR, null);
        TestContext context = new TestContext();
        context.sessionHolder.attributes.put("user", creator);
        context.setContentType("application/json");
        context.setBody("{\"quizId\": 1, \"action\": \"approve\"}");

        approveQuizServlet.service(context.request, context.response);
        assertEquals(403, context.status);
    }

    @Test
    @DisplayName("ListQuizzesServlet returns approved quizzes to public or participant")
    void testListApprovedQuizzes() throws Exception {
        TestContext context = new TestContext();
        context.setMethod("GET");

        listQuizzesServlet.service(context.request, context.response);

        assertEquals(200, context.status);
        Map<?, ?> body = JsonUtil.fromJson(context.getResponseBody(), Map.class);
        assertTrue((Boolean) body.get("success"));
        List<?> data = (List<?>) body.get("data");
        assertNotNull(data);
        assertFalse(data.isEmpty());
    }

    @Test
    @DisplayName("ListQuizzesServlet returns sanitized questions for participants and complete questions for creators")
    void testGetSingleQuizSanitization() throws Exception {
        TestContext participantContext = new TestContext();
        participantContext.setMethod("GET");
        participantContext.setParameter("id", "1");
        AppUser student = AppUser.create(3, "Student", "student@quizlive.com", "h", "s", Role.PARTICIPANT, null);
        participantContext.sessionHolder.attributes.put("user", student);

        listQuizzesServlet.service(participantContext.request, participantContext.response);

        assertEquals(200, participantContext.status);
        Map<?, ?> partBody = JsonUtil.fromJson(participantContext.getResponseBody(), Map.class);
        Map<?, ?> partData = (Map<?, ?>) partBody.get("data");
        List<?> questions = (List<?>) partData.get("questions");
        assertNotNull(questions);
        assertFalse(questions.isEmpty());
        Map<?, ?> q1 = (Map<?, ?>) questions.get(0);
        Object correctOpt = q1.get("correctOption");
        assertTrue(correctOpt == null || correctOpt.toString().trim().isEmpty());

        TestContext creatorContext = new TestContext();
        creatorContext.setMethod("GET");
        creatorContext.setParameter("id", "1");
        AppUser creator = AppUser.create(2, "Creator", "creator@quizlive.com", "h", "s", Role.CREATOR, null);
        creatorContext.sessionHolder.attributes.put("user", creator);

        listQuizzesServlet.service(creatorContext.request, creatorContext.response);

        assertEquals(200, creatorContext.status);
        Map<?, ?> creatorBody = JsonUtil.fromJson(creatorContext.getResponseBody(), Map.class);
        Map<?, ?> creatorData = (Map<?, ?>) creatorBody.get("data");
        List<?> creatorQuestions = (List<?>) creatorData.get("questions");
        assertNotNull(creatorQuestions);
        Map<?, ?> creatorQ1 = (Map<?, ?>) creatorQuestions.get(0);
        assertNotNull(creatorQ1.get("correctOption"));
        assertFalse(creatorQ1.get("correctOption").toString().trim().isEmpty());
    }

    @Test
    @DisplayName("ListQuizzesServlet restricts pending filter to admin only")
    void testListPendingFilterPermissions() throws Exception {
        AppUser student = AppUser.create(3, "Student", "student@quizlive.com", "h", "s", Role.PARTICIPANT, null);
        TestContext studentContext = new TestContext();
        studentContext.setMethod("GET");
        studentContext.setParameter("filter", "pending");
        studentContext.sessionHolder.attributes.put("user", student);

        listQuizzesServlet.service(studentContext.request, studentContext.response);
        assertEquals(403, studentContext.status);

        AppUser admin = AppUser.create(1, "Admin", "admin@quizlive.com", "h", "s", Role.ADMIN, null);
        TestContext adminContext = new TestContext();
        adminContext.setMethod("GET");
        adminContext.setParameter("filter", "pending");
        adminContext.sessionHolder.attributes.put("user", admin);

        listQuizzesServlet.service(adminContext.request, adminContext.response);
        assertEquals(200, adminContext.status);
    }

    private static class TestContext {
        private String method = "POST";
        private String contextPath = "/quizlive";
        private String requestUri = "/quizlive/quizzes";
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

        void setParameter(String name, String value) {
            parameters.put(name, new String[]{value});
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
