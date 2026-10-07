package com.quizlive;

import com.quizlive.util.DatabaseInitializer;
import com.quizlive.util.EnvConfig;
import com.quizlive.websocket.LeaderboardEndpoint;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.tomcat.websocket.server.WsSci;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;

public class AppLauncher {

    private static final int DEFAULT_PORT = 8080;
    private static final String DEFAULT_CONTEXT_PATH = "/quizlive";

    public static void main(String[] args) throws Exception {
        int port = resolvePort();
        String contextPath = resolveContextPath();
        Tomcat tomcat = createServer(port, contextPath);

        tomcat.start();
        String displayContext = contextPath.isEmpty() ? "" : contextPath;
        System.out.println("QuizLive server running at http://localhost:" + port + displayContext);
        System.out.println("Health check available at http://localhost:" + port + displayContext + "/health");
        tomcat.getServer().await();
    }

    public static Tomcat createServer(int port) throws IOException {
        return createServer(port, resolveContextPath());
    }

    public static Tomcat createServer(int port, String contextPath) throws IOException {
        try {
            DatabaseInitializer.initialize();
        } catch (Exception e) {
            System.err.println("Database auto-initialization skipped: " + e.getMessage());
        }

        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);

        File baseDir = Files.createTempDirectory("quizlive-tomcat").toFile();
        baseDir.deleteOnExit();
        tomcat.setBaseDir(baseDir.getAbsolutePath());

        Connector connector = tomcat.getConnector();
        connector.setPort(port);
        connector.setProperty("relaxedPathChars", "<>[\\]^`{|}");
        connector.setProperty("relaxedQueryChars", "<>[\\]^`{|}");

        File webappDir = resolveWebappDir(baseDir);

        StandardContext ctx = (StandardContext) tomcat.addWebapp(contextPath, webappDir.getAbsolutePath());
        ctx.setParentClassLoader(AppLauncher.class.getClassLoader());

        File additionWebInfClasses = resolveClassesDir();
        if (additionWebInfClasses != null && additionWebInfClasses.exists()) {
            WebResourceRoot resources = new StandardRoot(ctx);
            resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes",
                    additionWebInfClasses.getAbsolutePath(), "/"));
            ctx.setResources(resources);
        }

        ctx.addServletContainerInitializer(new WsSci(), Set.of(LeaderboardEndpoint.class));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                tomcat.stop();
                tomcat.destroy();
            } catch (Exception ignored) {
            }
        }));

        return tomcat;
    }

    private static File resolveWebappDir(File fallback) {
        String[] candidates = new String[]{
                "src/main/webapp",
                "webapp",
                "target/quizlive"
        };
        for (String c : candidates) {
            File f = new File(c);
            if (f.exists() && f.isDirectory()) {
                return f;
            }
        }
        return fallback;
    }

    private static File resolveClassesDir() {
        String[] candidates = new String[]{
                "target/classes",
                "build/classes",
                "WEB-INF/classes"
        };
        for (String c : candidates) {
            File f = new File(c);
            if (f.exists() && f.isDirectory()) {
                return f;
            }
        }
        return null;
    }

    public static int resolvePort() {
        return EnvConfig.getPort(DEFAULT_PORT);
    }

    public static String resolveContextPath() {
        return EnvConfig.getContextPath(DEFAULT_CONTEXT_PATH);
    }
}
