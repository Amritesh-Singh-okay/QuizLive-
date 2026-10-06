package com.quizlive;

import com.quizlive.dao.MessageDao;
import com.quizlive.dao.SettingsDao;
import com.quizlive.dao.UserDao;
import com.quizlive.dao.impl.MessageDaoImpl;
import com.quizlive.dao.impl.SettingsDaoImpl;
import com.quizlive.dao.impl.UserDaoImpl;
import com.quizlive.model.AppUser;
import com.quizlive.model.enums.Role;
import com.quizlive.servlet.MessageServlet;
import com.quizlive.servlet.SettingsServlet;
import com.quizlive.servlet.UserManagementServlet;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminAndMessagingServletTest {

    private UserManagementServlet userServlet;
    private MessageServlet messageServlet;
    private SettingsServlet settingsServlet;

    private UserDao userDao;
    private SettingsDao settingsDao;
    private final List<Integer> createdUserIds = new ArrayList<>();

    private AppUser adminUser;
    private AppUser studentUser;

    @BeforeEach
    void setUp() throws SQLException {
        this.userDao = new UserDaoImpl();
        MessageDao messageDao = new MessageDaoImpl();
        this.settingsDao = new SettingsDaoImpl();

        this.userServlet = new UserManagementServlet(userDao);
        this.messageServlet = new MessageServlet(messageDao, userDao);
        this.settingsServlet = new SettingsServlet(settingsDao);

        this.adminUser = AppUser.create(1, "Admin", "admin@quizlive.com", "h", "s", Role.ADMIN, null);
        this.studentUser = AppUser.create(3, "Student", "alice@quizlive.com", "h", "s", Role.PARTICIPANT, null);
    }

    @AfterEach
    void tearDown() throws SQLException {
        for (int id : createdUserIds) {
            userDao.delete(id);
        }
        createdUserIds.clear();
        settingsDao.setSetting("platform_name", "QuizLive");
    }

    @Test
    @DisplayName("UserManagementServlet allows admin to list and manage users")
    void testUserManagementAdminFlow() throws Exception {
        String testEmail = "tempuser_" + System.currentTimeMillis() + "@quizlive.com";
        AppUser tempUser = userDao.create(AppUser.create(0, "Temp", testEmail, "h", "s", Role.PARTICIPANT, null));
        createdUserIds.add(tempUser.getId());

        TestContext listContext = new TestContext();
        listContext.setMethod("GET");
        listContext.sessionHolder.attributes.put("user", adminUser);

        userServlet.service(listContext.request, listContext.response);

        assertEquals(200, listContext.status);
        Map<?, ?> listBody = JsonUtil.fromJson(listContext.getResponseBody(), Map.class);
        assertTrue((Boolean) listBody.get("success"));
        List<?> users = (List<?>) listBody.get("data");
        assertNotNull(users);
        assertFalse(users.isEmpty());

        Map<?, ?> firstUser = (Map<?, ?>) users.get(0);
        assertNull(firstUser.get("passwordHash"));
        assertNull(firstUser.get("salt"));

        TestContext roleContext = new TestContext();
        roleContext.sessionHolder.attributes.put("user", adminUser);
        roleContext.setRequestUri("/quizlive/admin/users/update-role");
        roleContext.setContentType("application/json");
        roleContext.setBody(String.format("{\"userId\": %d, \"role\": \"CREATOR\"}", tempUser.getId()));

        userServlet.service(roleContext.request, roleContext.response);
        assertEquals(200, roleContext.status);
        AppUser updated = userDao.findById(tempUser.getId());
        assertEquals(Role.CREATOR, updated.getRole());

        TestContext deleteSelfContext = new TestContext();
        deleteSelfContext.sessionHolder.attributes.put("user", adminUser);
        deleteSelfContext.setRequestUri("/quizlive/admin/users/delete");
        deleteSelfContext.setContentType("application/json");
        deleteSelfContext.setBody("{\"userId\": 1}");

        userServlet.service(deleteSelfContext.request, deleteSelfContext.response);
        assertEquals(400, deleteSelfContext.status);

        TestContext deleteTempContext = new TestContext();
        deleteTempContext.sessionHolder.attributes.put("user", adminUser);
        deleteTempContext.setRequestUri("/quizlive/admin/users/delete");
        deleteTempContext.setContentType("application/json");
        deleteTempContext.setBody(String.format("{\"userId\": %d}", tempUser.getId()));

        userServlet.service(deleteTempContext.request, deleteTempContext.response);
        assertEquals(200, deleteTempContext.status);
        assertNull(userDao.findById(tempUser.getId()));
    }

    @Test
    @DisplayName("UserManagementServlet restricts operations to admin only")
    void testUserManagementForbiddenForParticipant() throws Exception {
        TestContext context = new TestContext();
        context.setMethod("GET");
        context.sessionHolder.attributes.put("user", studentUser);

        userServlet.service(context.request, context.response);
        assertEquals(403, context.status);
    }

    @Test
    @DisplayName("MessageServlet allows users to exchange messages and read thread history")
    void testMessageFlow() throws Exception {
        TestContext postContext = new TestContext();
        postContext.sessionHolder.attributes.put("user", studentUser);
        postContext.setContentType("application/json");
        postContext.setBody("{\"toUserId\": 2, \"quizId\": 1, \"content\": \"Question regarding quiz 1\"}");

        messageServlet.service(postContext.request, postContext.response);

        assertEquals(201, postContext.status);
        Map<?, ?> postBody = JsonUtil.fromJson(postContext.getResponseBody(), Map.class);
        assertTrue((Boolean) postBody.get("success"));

        Map<?, ?> data = (Map<?, ?>) postBody.get("data");
        assertEquals("Question regarding quiz 1", data.get("content"));

        TestContext threadContext = new TestContext();
        threadContext.setMethod("GET");
        threadContext.sessionHolder.attributes.put("user", studentUser);
        threadContext.setParameter("withUser", "2");

        messageServlet.service(threadContext.request, threadContext.response);

        assertEquals(200, threadContext.status);
        Map<?, ?> threadBody = JsonUtil.fromJson(threadContext.getResponseBody(), Map.class);
        List<?> threadList = (List<?>) threadBody.get("data");
        assertNotNull(threadList);
        assertFalse(threadList.isEmpty());

        TestContext selfContext = new TestContext();
        selfContext.sessionHolder.attributes.put("user", studentUser);
        selfContext.setContentType("application/json");
        selfContext.setBody("{\"toUserId\": 3, \"content\": \"Self message\"}");

        messageServlet.service(selfContext.request, selfContext.response);
        assertEquals(400, selfContext.status);
    }

    @Test
    @DisplayName("SettingsServlet allows reading settings and restricts updates to admin")
    void testSettingsLifecycle() throws Exception {
        TestContext getContext = new TestContext();
        getContext.setMethod("GET");

        settingsServlet.service(getContext.request, getContext.response);

        assertEquals(200, getContext.status);
        Map<?, ?> getBody = JsonUtil.fromJson(getContext.getResponseBody(), Map.class);
        assertTrue((Boolean) getBody.get("success"));
        Map<?, ?> settings = (Map<?, ?>) getBody.get("data");
        assertNotNull(settings.get("platform_name"));

        TestContext studentUpdateContext = new TestContext();
        studentUpdateContext.sessionHolder.attributes.put("user", studentUser);
        studentUpdateContext.setContentType("application/json");
        studentUpdateContext.setBody("{\"platform_name\": \"Hacked Platform\"}");

        settingsServlet.service(studentUpdateContext.request, studentUpdateContext.response);
        assertEquals(403, studentUpdateContext.status);

        TestContext adminUpdateContext = new TestContext();
        adminUpdateContext.sessionHolder.attributes.put("user", adminUser);
        adminUpdateContext.setContentType("application/json");
        adminUpdateContext.setBody("{\"platform_name\": \"QuizLive Enterprise\"}");

        settingsServlet.service(adminUpdateContext.request, adminUpdateContext.response);
        assertEquals(200, adminUpdateContext.status);

        assertEquals("QuizLive Enterprise", settingsDao.getSetting("platform_name", ""));
    }

    private static class TestContext {
        private String method = "POST";
        private String contextPath = "/quizlive";
        private String requestUri = "/quizlive/admin/users";
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

        void setRequestUri(String requestUri) {
            this.requestUri = requestUri;
        }

        void setContentType(String contentType) {
            this.contentType = contentType;
        }

        void setBody(String body) {
            this.body = body;
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
