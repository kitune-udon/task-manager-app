package com.example.task.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;

/**
 * Spring コンテキスト初期化途中で失敗した場合も設定読込失敗ログを残す。
 */
public class StartupFailureLoggingListener implements ApplicationListener<ApplicationFailedEvent> {

    private static final Logger APPLICATION_LOGGER = LoggerFactory.getLogger("application");

    @Override
    public void onApplicationEvent(ApplicationFailedEvent event) {
        LinkedHashMap<String, Object> fields = new LinkedHashMap<>();
        fields.put("exceptionClass", event.getException().getClass().getName());
        fields.put("safeMessage", "設定読込に失敗しました");

        String payload = StructuredLogJsonFormatter.format(
                "ERROR",
                "task-app",
                resolveEnvironment(event),
                BuildInfoResourceLoader.loadVersion(),
                "LOG-APP-004",
                "設定読込失敗",
                fields
        );

        APPLICATION_LOGGER.error(payload);
    }

    private String resolveEnvironment(ApplicationFailedEvent event) {
        if (event.getApplicationContext() != null) {
            String[] activeProfiles = event.getApplicationContext().getEnvironment().getActiveProfiles();
            if (activeProfiles.length > 0) {
                return String.join(",", activeProfiles);
            }
        }

        String activeProfiles = System.getProperty("spring.profiles.active");
        if (!StringUtils.hasText(activeProfiles)) {
            activeProfiles = System.getenv("SPRING_PROFILES_ACTIVE");
        }

        return StringUtils.hasText(activeProfiles) ? activeProfiles : "default";
    }
}
