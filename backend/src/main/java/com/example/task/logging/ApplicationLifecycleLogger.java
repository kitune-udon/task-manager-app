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

    /**
     * アプリケーションライフサイクルロガーを生成する。
     *
     * @param structuredLogService 構造化ログサービス
     */
    public ApplicationLifecycleLogger(StructuredLogService structuredLogService) {
        this.structuredLogService = structuredLogService;
    }

    /**
     * Springアプリケーションの起動開始後に起動ログを出力する。
     */
    @EventListener(ApplicationStartedEvent.class)
    public void logApplicationStarted() {
        structuredLogService.infoApplication("LOG-APP-001", "アプリケーション起動", new LinkedHashMap<>());
    }

    /**
     * アプリケーションがリクエストを受け付けられる状態になった後、設定読込完了ログを出力する。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void logConfigurationLoaded() {
        LinkedHashMap<String, Object> fields = new LinkedHashMap<>();
        // 起動プロファイルは未設定の環境もあるため、値がある場合だけログフィールドに含める。
        StructuredLogService.putIfPresent(fields, "loadedProfiles", structuredLogService.getLoadedProfiles());
        structuredLogService.infoApplication("LOG-APP-003", "設定読込成功", fields);
    }

    /**
     * Springアプリケーションコンテキストの終了時に終了ログを出力する。
     */
    @EventListener(ContextClosedEvent.class)
    public void logApplicationClosed() {
        structuredLogService.infoApplication("LOG-APP-002", "アプリケーション終了", new LinkedHashMap<>());
    }
}
