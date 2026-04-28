package com.example.task.dto;

import com.example.task.entity.Priority;
import com.example.task.entity.TaskStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * タスク一覧表示向けの軽量レスポンス DTO。
 */
@Getter
@Builder
public class TaskSummaryResponse {
    private Long id;
    private String title;
    private TaskStatus status;
    private Priority priority;
    private LocalDate dueDate;
    private TaskUserResponse assignedUser;
    private Long teamId;
    private String teamName;
    private LocalDateTime updatedAt;
    private Long version;
}
