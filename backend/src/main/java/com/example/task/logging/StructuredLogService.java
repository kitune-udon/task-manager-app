package com.example.task.logging;

import org.slf4j.event.Level;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 設計書に定義した application / security / audit ログを共通形式で出力する。
 */
@Service
public class StructuredLogService {

    private static final String SERVICE_NAME = "task-app";

    private final Environment environment;
    private final RequestLogContext requestLogContext;
    private final String version;

    public StructuredLogService(
            Environment environment,
            RequestLogContext requestLogContext,
            ObjectProvider<BuildProperties> buildPropertiesProvider
    ) {
        this.environment = environment;
        this.requestLogContext = requestLogContext;
        BuildProperties buildProperties = buildPropertiesProvider.getIfAvailable();
        this.version = buildProperties != null
                ? buildProperties.getVersion()
                : BuildInfoResourceLoader.loadVersion();
    }

    public void debugApplication(String eventId, String message, Map<String, Object> fields) {
        log(LogChannel.APPLICATION, Level.DEBUG, eventId, message, fields);
    }

    public void infoApplication(String eventId, String message, Map<String, Object> fields) {
        log(LogChannel.APPLICATION, Level.INFO, eventId, message, fields);
    }

    public void warnApplication(String eventId, String message, Map<String, Object> fields) {
        log(LogChannel.APPLICATION, Level.WARN, eventId, message, fields);
    }

    public void errorApplication(String eventId, String message, Map<String, Object> fields) {
        log(LogChannel.APPLICATION, Level.ERROR, eventId, message, fields);
    }

    public void infoSecurity(String eventId, String message, Map<String, Object> fields) {
        log(LogChannel.SECURITY, Level.INFO, eventId, message, fields);
    }

    public void warnSecurity(String eventId, String message, Map<String, Object> fields) {
        log(LogChannel.SECURITY, Level.WARN, eventId, message, fields);
    }

    public void infoAudit(String eventId, String message, Map<String, Object> fields) {
        log(LogChannel.AUDIT, Level.INFO, eventId, message, fields);
    }

    public LinkedHashMap<String, Object> requestFields(
            HttpServletRequest request,
            Integer status,
            boolean includeUserId,
            boolean includeIp,
            boolean includeDuration
    ) {
        LinkedHashMap<String, Object> fields = new LinkedHashMap<>();
        putIfPresent(fields, "requestId", requestLogContext.getRequestId(request));
        putIfPresent(fields, "userId", includeUserId ? requestLogContext.getAuthenticatedUserId() : null);
        putIfPresent(fields, "path", request.getRequestURI());
        putIfPresent(fields, "method", request.getMethod());
        putIfPresent(fields, "status", status);
        putIfPresent(fields, "durationMs", includeDuration ? requestLogContext.getDurationMs(request) : null);
        putIfPresent(fields, "ip", includeIp ? requestLogContext.getClientIp(request) : null);
        return fields;
    }

    public LinkedHashMap<String, Object> currentRequestFields(
            Integer status,
            boolean includeUserId,
            boolean includeIp,
            boolean includeDuration
    ) {
        HttpServletRequest request = requestLogContext.getCurrentRequest();
        return request != null
                ? requestFields(request, status, includeUserId, includeIp, includeDuration)
                : new LinkedHashMap<>();
    }

    public String maskEmail(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            return null;
        }

        String[] parts = email.split("@", 2);
        String localPart = parts[0];
        String domainPart = parts[1];

        String maskedLocalPart;
        if (localPart.length() <= 1) {
            maskedLocalPart = "*";
        } else if (localPart.length() == 2) {
            maskedLocalPart = localPart.charAt(0) + "*";
        } else {
            maskedLocalPart = localPart.substring(0, 2) + "***";
        }

        return maskedLocalPart + "@" + domainPart;
    }

    public String getEnvironmentName() {
        String[] activeProfiles = environment.getActiveProfiles();
        return activeProfiles.length == 0 ? "default" : String.join(",", activeProfiles);
    }

    public String getLoadedProfiles() {
        return getEnvironmentName();
    }

    public String getVersion() {
        return version;
    }

    public static void putIfPresent(Map<String, Object> fields, String key, Object value) {
        if (value != null) {
            fields.put(key, value);
        }
    }

    private void log(LogChannel channel, Level level, String eventId, String message, Map<String, Object> fields) {
        String payload = StructuredLogJsonFormatter.format(
                level.name(),
                SERVICE_NAME,
                getEnvironmentName(),
                version,
                eventId,
                message,
                fields
        );

        switch (level) {
            case ERROR -> channel.getLogger().error(payload);
            case WARN -> channel.getLogger().warn(payload);
            case INFO -> channel.getLogger().info(payload);
            case DEBUG -> channel.getLogger().debug(payload);
            default -> channel.getLogger().info(payload);
        }
    }
}
