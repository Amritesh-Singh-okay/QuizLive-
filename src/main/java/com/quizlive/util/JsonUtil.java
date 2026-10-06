package com.quizlive.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

public final class JsonUtil {

    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .serializeNulls()
            .create();

    private JsonUtil() {
    }

    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }

    public static void sendSuccess(HttpServletResponse resp, Object data) throws IOException {
        sendResponse(resp, HttpServletResponse.SC_OK, true, data, null);
    }

    public static void sendCreated(HttpServletResponse resp, Object data) throws IOException {
        sendResponse(resp, HttpServletResponse.SC_CREATED, true, data, null);
    }

    public static void sendError(HttpServletResponse resp, int statusCode, String message) throws IOException {
        sendResponse(resp, statusCode, false, null, message);
    }

    private static void sendResponse(HttpServletResponse resp, int statusCode, boolean success,
                                     Object data, String error) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(statusCode);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", success);
        if (data != null) {
            body.put("data", data);
        }
        if (error != null) {
            body.put("error", error);
        }

        try (PrintWriter writer = resp.getWriter()) {
            writer.write(GSON.toJson(body));
            writer.flush();
        }
    }
}
