package com.example.task.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 設計書で定義した論理 logger 名を表す。
 */
public enum LogChannel {
    APPLICATION("application"),
    SECURITY("security"),
    AUDIT("audit");

    private final Logger logger;

    LogChannel(String loggerName) {
        this.logger = LoggerFactory.getLogger(loggerName);
    }

    public Logger getLogger() {
        return logger;
    }
}
