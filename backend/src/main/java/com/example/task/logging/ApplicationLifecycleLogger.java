package com.example.task.logging;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;

/**
 * アプリケーションの起動・設定読込完了・終了を構造化ログとして残す。
 */
@Component
public class ApplicationLifecycleLogger {

    private final StructuredLogService structuredLogService;

    public ApplicationLifecycleLogger(StructuredLogService structuredLogService) {
        this.structuredLogService = structuredLogService;
    }

    @EventListener(ApplicationStartedEvent.class)
    public void logApplicationStarted() {
        structuredLogService.infoApplication("LOG-APP-001", "アプリケーション起動", new LinkedHashMap<>());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logConfigurationLoaded() {
        LinkedHashMap<String, Object> fields = new LinkedHashMap<>();
        StructuredLogService.putIfPresent(fields, "loadedProfiles", structuredLogService.getLoadedProfiles());
        structuredLogService.infoApplication("LOG-APP-003", "設定読込成功", fields);
    }

    @EventListener(ContextClosedEvent.class)
    public void logApplicationClosed() {
        structuredLogService.infoApplication("LOG-APP-002", "アプリケーション終了", new LinkedHashMap<>());
    }
}
