package com.example.task.dto;

import com.example.task.entity.Priority;
import com.example.task.entity.TaskStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * タスク詳細 API が返すレスポンス DTO。
 */
@Getter
@Builder
public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private Priority priority;
    private LocalDate dueDate;
    private TaskUserResponse assignedUser;
    private TaskUserResponse createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
