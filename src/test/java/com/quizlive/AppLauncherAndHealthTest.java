package com.quizlive;

import com.quizlive.servlet.HealthServlet;
import com.quizlive.util.JsonUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.startup.Tomcat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppLauncherAndHealthTest {

    @Test
    @DisplayName("HealthServlet returns 200 with UP and CONNECTED status")
    void testHealthServlet() throws Exception {
        HealthServlet servlet = new HealthServlet();
        StringWriter sw = new StringWriter();
        int[] statusHolder = new int[]{200};

        HttpServletRequest req = (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getMethod" -> "GET";
                    case "getContextPath" -> "/quizlive";
                    case "getRequestURI" -> "/quizlive/health";
                    default -> null;
                }
        );

        HttpServletResponse resp = (HttpServletResponse) Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(),
                new Class<?>[]{HttpServletResponse.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "setStatus" -> {
                        statusHolder[0] = (int) args[0];
                        yield null;
                    }
                    case "getWriter" -> new PrintWriter(sw);
                    default -> null;
                }
        );

        servlet.service(req, resp);

        assertEquals(200, statusHolder[0]);
        Map<?, ?> body = JsonUtil.fromJson(sw.toString(), Map.class);
        assertTrue((Boolean) body.get("success"));

        Map<?, ?> data = (Map<?, ?>) body.get("data");
        assertEquals("UP", data.get("status"));
        assertEquals("CONNECTED", data.get("database"));
    }

    @Test
    @DisplayName("AppLauncher successfully creates and configures embedded Tomcat instance")
    void testAppLauncherCreateServer() throws Exception {
        int testPort = 8089;
        Tomcat tomcat = AppLauncher.createServer(testPort);

        assertNotNull(tomcat);
        assertEquals(testPort, tomcat.getConnector().getPort());
        assertNotNull(tomcat.getHost().findChild("/quizlive"));

        try {
            tomcat.stop();
            tomcat.destroy();
        } catch (Exception ignored) {
        }
    }
}
