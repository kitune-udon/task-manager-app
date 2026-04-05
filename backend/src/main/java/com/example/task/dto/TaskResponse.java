package com.example.task.dto;

import com.example.task.entity.Priority;
import com.example.task.entity.TaskStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private Priority priority;
    private LocalDateTime dueDate;
    private Long assignedUserId;
    private String assignedUserName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}