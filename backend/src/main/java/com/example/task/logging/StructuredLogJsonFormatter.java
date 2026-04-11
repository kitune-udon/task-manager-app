package com.example.task.logging;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 設計書に沿った構造化 JSON へログイベントを整形する。
 */
public final class StructuredLogJsonFormatter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private StructuredLogJsonFormatter() {
    }

    public static String format(
            String level,
            String serviceName,
            String environment,
            String version,
            String eventId,
            String message,
            Map<String, Object> fields
    ) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("timestamp", OffsetDateTime.now().toString());
        payload.put("level", level);
        payload.put("serviceName", serviceName);
        payload.put("environment", environment);
        payload.put("version", version);
        payload.put("eventId", eventId);

        Map<String, Object> remainingFields = new LinkedHashMap<>(fields);
        remainingFields.put("message", message);

        appendField(payload, remainingFields, "requestId");
        appendField(payload, remainingFields, "userId");
        appendField(payload, remainingFields, "path");
        appendField(payload, remainingFields, "method");
        appendField(payload, remainingFields, "status");
        appendField(payload, remainingFields, "durationMs");
        appendField(payload, remainingFields, "message");
        appendField(payload, remainingFields, "safeMessage");
        appendField(payload, remainingFields, "errorCode");
        appendField(payload, remainingFields, "details");
        appendField(payload, remainingFields, "email");
        appendField(payload, remainingFields, "ip");
        appendField(payload, remainingFields, "loadedProfiles");
        appendField(payload, remainingFields, "reason");
        appendField(payload, remainingFields, "taskId");
        appendField(payload, remainingFields, "changedFields");
        appendField(payload, remainingFields, "commentId");
        appendField(payload, remainingFields, "attachmentId");
        appendField(payload, remainingFields, "fileName");
        appendField(payload, remainingFields, "size");
        appendField(payload, remainingFields, "teamId");
        appendField(payload, remainingFields, "memberUserId");
        appendField(payload, remainingFields, "newRole");
        appendField(payload, remainingFields, "jobName");
        appendField(payload, remainingFields, "startedAt");
        appendField(payload, remainingFields, "finishedAt");
        appendField(payload, remainingFields, "targetCount");
        appendField(payload, remainingFields, "processedCount");
        appendField(payload, remainingFields, "successCount");
        appendField(payload, remainingFields, "failureCount");
        appendField(payload, remainingFields, "exceptionClass");
        appendField(payload, remainingFields, "stackTrace");

        remainingFields.forEach((key, value) -> {
            if (value != null) {
                payload.put(key, value);
            }
        });

        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return "{\"timestamp\":\"" + OffsetDateTime.now() + "\",\"level\":\"ERROR\",\"serviceName\":\""
                    + serviceName + "\",\"environment\":\"" + environment + "\",\"version\":\"" + version
                    + "\",\"eventId\":\"LOG-SYS-JSON-001\",\"message\":\"ログJSON整形に失敗しました\"}";
        }
    }

    public static String stackTrace(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    private static void appendField(
            LinkedHashMap<String, Object> payload,
            Map<String, Object> remainingFields,
            String key
    ) {
        if (!remainingFields.containsKey(key)) {
            return;
        }

        Object value = remainingFields.remove(key);
        if (value != null) {
            payload.put(key, value);
        }
    }
}
