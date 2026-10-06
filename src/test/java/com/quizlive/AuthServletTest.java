package com.quizlive;

import com.quizlive.dao.UserDao;
import com.quizlive.dao.impl.UserDaoImpl;
import com.quizlive.model.AppUser;
import com.quizlive.model.enums.Role;
import com.quizlive.servlet.LoginServlet;
import com.quizlive.servlet.LogoutServlet;
import com.quizlive.servlet.RegisterServlet;
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

class AuthServletTest {

    private LoginServlet loginServlet;
    private RegisterServlet registerServlet;
    private LogoutServlet logoutServlet;
    private UserDao userDao;
    private final List<Integer> createdUserIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        this.loginServlet = new LoginServlet();
        this.registerServlet = new RegisterServlet();
        this.logoutServlet = new LogoutServlet();
        this.userDao = new UserDaoImpl();
    }

    @AfterEach
    void tearDown() throws SQLException {
        for (int id : createdUserIds) {
            userDao.delete(id);
        }
        createdUserIds.clear();
    }

    @Test
    @DisplayName("LoginServlet handles valid JSON credentials and returns 200 with user data")
    void testLoginSuccessJson() throws Exception {
        TestContext context = new TestContext();
        context.setContentType("application/json");
        context.setBody("{\"email\":\"alice@quizlive.com\",\"password\":\"password123\"}");

        loginServlet.service(context.request, context.response);

        assertEquals(200, context.status);
        Map<?, ?> body = JsonUtil.fromJson(context.getResponseBody(), Map.class);
        assertTrue((Boolean) body.get("success"));

        Map<?, ?> data = (Map<?, ?>) body.get("data");
        assertNotNull(data);
        assertEquals("alice@quizlive.com", data.get("email"));
        assertEquals("PARTICIPANT", data.get("role"));
        assertTrue(data.get("redirectUrl").toString().contains("/participant/dashboard.jsp"));

        AppUser sessionUser = (AppUser) context.sessionHolder.attributes.get("user");
        assertNotNull(sessionUser);
        assertEquals("alice@quizlive.com", sessionUser.getEmail());
    }

    @Test
    @DisplayName("LoginServlet returns 401 on incorrect credentials")
    void testLoginFailureInvalidPassword() throws Exception {
        TestContext context = new TestContext();
        context.setContentType("application/json");
        context.setBody("{\"email\":\"alice@quizlive.com\",\"password\":\"wrongPass\"}");

        loginServlet.service(context.request, context.response);

        assertEquals(401, context.status);
        Map<?, ?> body = JsonUtil.fromJson(context.getResponseBody(), Map.class);
        assertFalse((Boolean) body.get("success"));
        assertNotNull(body.get("error"));
        assertNull(context.sessionHolder.attributes.get("user"));
    }

    @Test
    @DisplayName("LoginServlet returns 400 on missing credentials")
    void testLoginMissingCredentials() throws Exception {
        TestContext context = new TestContext();
        context.setContentType("application/json");
        context.setBody("{\"email\":\"\",\"password\":\"\"}");

        loginServlet.service(context.request, context.response);

        assertEquals(400, context.status);
        Map<?, ?> body = JsonUtil.fromJson(context.getResponseBody(), Map.class);
        assertFalse((Boolean) body.get("success"));
    }

    @Test
    @DisplayName("LoginServlet GET redirects authenticated user or returns 401 for unauthenticated JSON")
    void testLoginGetRouting() throws Exception {
        TestContext unauthContext = new TestContext();
        unauthContext.setMethod("GET");
        unauthContext.setHeader("Accept", "application/json");

        loginServlet.service(unauthContext.request, unauthContext.response);
        assertEquals(401, unauthContext.status);

        TestContext authContext = new TestContext();
        authContext.setMethod("GET");
        AppUser adminUser = AppUser.create(1, "Admin", "admin@quizlive.com", "h", "s", Role.ADMIN, null);
        authContext.sessionHolder.attributes.put("user", adminUser);

        loginServlet.service(authContext.request, authContext.response);
        assertEquals("/quizlive/admin/dashboard.jsp", authContext.redirectUrl);
    }

    @Test
    @DisplayName("RegisterServlet handles JSON payload and registers new participant with 201 Created")
    void testRegisterSuccessJson() throws Exception {
        String testEmail = "servletreg_" + System.currentTimeMillis() + "@quizlive.com";
        TestContext context = new TestContext();
        context.setContentType("application/json");
        context.setBody(String.format("{\"name\":\"Servlet Test\",\"email\":\"%s\",\"password\":\"secure123\",\"role\":\"PARTICIPANT\"}", testEmail));

        registerServlet.service(context.request, context.response);

        assertEquals(201, context.status);
        Map<?, ?> body = JsonUtil.fromJson(context.getResponseBody(), Map.class);
        assertTrue((Boolean) body.get("success"));

        Map<?, ?> data = (Map<?, ?>) body.get("data");
        assertNotNull(data);
        assertEquals(testEmail, data.get("email"));
        assertEquals("PARTICIPANT", data.get("role"));

        int createdId = ((Number) data.get("id")).intValue();
        createdUserIds.add(createdId);

        AppUser sessionUser = (AppUser) context.sessionHolder.attributes.get("user");
        assertNotNull(sessionUser);
        assertEquals(createdId, sessionUser.getId());
    }

    @Test
    @DisplayName("RegisterServlet returns 400 for duplicate email or weak password")
    void testRegisterValidationFailure() throws Exception {
        TestContext dupContext = new TestContext();
        dupContext.setContentType("application/json");
        dupContext.setBody("{\"name\":\"Dup\",\"email\":\"admin@quizlive.com\",\"password\":\"secure123\"}");

        registerServlet.service(dupContext.request, dupContext.response);
        assertEquals(400, dupContext.status);

        TestContext weakContext = new TestContext();
        weakContext.setContentType("application/json");
        weakContext.setBody("{\"name\":\"Weak\",\"email\":\"valid@email.com\",\"password\":\"123\"}");

        registerServlet.service(weakContext.request, weakContext.response);
        assertEquals(400, weakContext.status);
    }

    @Test
    @DisplayName("RegisterServlet rejects admin self-registration with 400 Bad Request")
    void testRegisterAdminRoleForbidden() throws Exception {
        TestContext adminContext = new TestContext();
        adminContext.setContentType("application/json");
        adminContext.setBody("{\"name\":\"Admin Attempter\",\"email\":\"hacker@quizlive.com\",\"password\":\"secure123\",\"role\":\"ADMIN\"}");

        registerServlet.service(adminContext.request, adminContext.response);
        assertEquals(400, adminContext.status);
        Map<?, ?> body = JsonUtil.fromJson(adminContext.getResponseBody(), Map.class);
        assertFalse((Boolean) body.get("success"));
        assertEquals("Registration with ADMIN role is not permitted", body.get("error"));
    }

    @Test
    @DisplayName("RegisterServlet rejects admin self-registration with lowercase role")
    void testRegisterAdminRoleLowercaseForbidden() throws Exception {
        TestContext adminContext = new TestContext();
        adminContext.setContentType("application/json");
        adminContext.setBody("{\"name\":\"Admin Attempter\",\"email\":\"hacker_lc@quizlive.com\",\"password\":\"secure123\",\"role\":\"admin\"}");

        registerServlet.service(adminContext.request, adminContext.response);
        assertEquals(400, adminContext.status);
        Map<?, ?> body = JsonUtil.fromJson(adminContext.getResponseBody(), Map.class);
        assertFalse((Boolean) body.get("success"));
        assertEquals("Registration with ADMIN role is not permitted", body.get("error"));
    }

    @Test
    @DisplayName("RegisterServlet rejects admin self-registration via form post")
    void testRegisterAdminRoleFormForbidden() throws Exception {
        TestContext adminContext = new TestContext();
        adminContext.setContentType("application/x-www-form-urlencoded");
        adminContext.setParameter("name", "Form Admin");
        adminContext.setParameter("email", "hacker_form@quizlive.com");
        adminContext.setParameter("password", "secure123");
        adminContext.setParameter("role", "ADMIN");

        registerServlet.service(adminContext.request, adminContext.response);
        assertEquals(400, adminContext.status);
        Map<?, ?> body = JsonUtil.fromJson(adminContext.getResponseBody(), Map.class);
        assertFalse((Boolean) body.get("success"));
        assertEquals("Registration with ADMIN role is not permitted", body.get("error"));
    }

    @Test
    @DisplayName("RegisterServlet allows registering with CREATOR role")
    void testRegisterCreatorRoleSuccess() throws Exception {
        String testEmail = "servletcreator_" + System.currentTimeMillis() + "@quizlive.com";
        TestContext context = new TestContext();
        context.setContentType("application/json");
        context.setBody(String.format("{\"name\":\"Creator Test\",\"email\":\"%s\",\"password\":\"secure123\",\"role\":\"CREATOR\"}", testEmail));

        registerServlet.service(context.request, context.response);

        assertEquals(201, context.status);
        Map<?, ?> body = JsonUtil.fromJson(context.getResponseBody(), Map.class);
        assertTrue((Boolean) body.get("success"));

        Map<?, ?> data = (Map<?, ?>) body.get("data");
        assertNotNull(data);
        assertEquals(testEmail, data.get("email"));
        assertEquals("CREATOR", data.get("role"));

        int createdId = ((Number) data.get("id")).intValue();
        createdUserIds.add(createdId);
    }

    @Test
    @DisplayName("LogoutServlet invalidates active session and responds with JSON")
    void testLogoutSuccess() throws Exception {
        TestContext context = new TestContext();
        context.setMethod("POST");
        context.setHeader("Accept", "application/json");
        context.sessionHolder.attributes.put("user", AppUser.create(1, "Test", "test@test.com", "h", "s", Role.PARTICIPANT, null));

        logoutServlet.service(context.request, context.response);

        assertEquals(200, context.status);
        assertTrue(context.sessionHolder.invalidated);
        Map<?, ?> body = JsonUtil.fromJson(context.getResponseBody(), Map.class);
        assertTrue((Boolean) body.get("success"));
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
