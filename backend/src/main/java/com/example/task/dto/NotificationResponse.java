package com.example.task.dto;

import com.example.task.entity.ActivityEventType;
import com.example.task.entity.ActivityTargetType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 通知一覧・既読化で返すレスポンス DTO。
 */
@Getter
@Builder
public class NotificationResponse {
    private Long id;
    private Long activityLogId;
    private ActivityEventType eventType;
    private String message;
    private Long relatedTaskId;
    private String relatedTaskTitle;
    private ActivityTargetType targetType;
    private Long targetId;
    @JsonProperty("isRead")
    private boolean isRead;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
