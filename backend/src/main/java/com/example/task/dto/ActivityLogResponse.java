package com.example.task.dto;

import com.example.task.entity.ActivityEventType;
import com.example.task.entity.ActivityTargetType;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * タスク別履歴一覧のレスポンス DTO。
 */
@Getter
@Builder
public class ActivityLogResponse {
    private Long id;
    private ActivityEventType eventType;
    private TaskUserResponse actor;
    private ActivityTargetType targetType;
    private Long targetId;
    private Long taskId;
    private String summary;
    private JsonNode detailJson;
    private LocalDateTime createdAt;
}
