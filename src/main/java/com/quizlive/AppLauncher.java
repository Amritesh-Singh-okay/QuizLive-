package com.quizlive;

import com.quizlive.util.DatabaseInitializer;
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
    private static final String CONTEXT_PATH = "/quizlive";

    public static void main(String[] args) throws Exception {
        int port = resolvePort();
        Tomcat tomcat = createServer(port);

        tomcat.start();
        System.out.println("QuizLive server running at http://localhost:" + port + CONTEXT_PATH);
        System.out.println("Health check available at http://localhost:" + port + CONTEXT_PATH + "/health");
        tomcat.getServer().await();
    }

    public static Tomcat createServer(int port) throws IOException {
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

        File webappDir = new File("src/main/webapp");
        if (!webappDir.exists()) {
            webappDir = baseDir;
        }

        StandardContext ctx = (StandardContext) tomcat.addWebapp(CONTEXT_PATH, webappDir.getAbsolutePath());
        ctx.setParentClassLoader(AppLauncher.class.getClassLoader());

        File additionWebInfClasses = new File("target/classes");
        if (additionWebInfClasses.exists()) {
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

    private static int resolvePort() {
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.trim().isEmpty()) {
            try {
                return Integer.parseInt(envPort.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        String propPort = System.getProperty("server.port");
        if (propPort != null && !propPort.trim().isEmpty()) {
            try {
                return Integer.parseInt(propPort.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        return DEFAULT_PORT;
    }
}
