package com.example.task.logging;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 構造化ログの運用向けスイッチを束ねる設定値。
 */
@Component
@ConfigurationProperties(prefix = "app.logging")
public class LoggingProperties {

    private boolean includeStacktrace;

    public boolean isIncludeStacktrace() {
        return includeStacktrace;
    }

    public void setIncludeStacktrace(boolean includeStacktrace) {
        this.includeStacktrace = includeStacktrace;
    }
}
