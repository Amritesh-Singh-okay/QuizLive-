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
            .registerTypeAdapter(java.sql.Timestamp.class, (com.google.gson.JsonDeserializer<java.sql.Timestamp>) (json, typeOfT, context) -> {
                if (json == null || json.isJsonNull()) {
                    return null;
                }
                String s = json.getAsString().trim();
                if (s.isEmpty()) {
                    return null;
                }
                try {
                    return new java.sql.Timestamp(Long.parseLong(s));
                } catch (NumberFormatException ignored) {}
                s = s.replace("T", " ");
                if (s.length() == 16) {
                    s += ":00";
                }
                try {
                    return java.sql.Timestamp.valueOf(s);
                } catch (Exception e) {
                    return null;
                }
            })
            .registerTypeAdapter(java.sql.Timestamp.class, (com.google.gson.JsonSerializer<java.sql.Timestamp>) (src, typeOfSrc, context) -> {
                if (src == null) return com.google.gson.JsonNull.INSTANCE;
                return new com.google.gson.JsonPrimitive(src.toString());
            })
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
