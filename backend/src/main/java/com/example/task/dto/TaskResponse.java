package com.example.task.dto;

import com.example.task.entity.Priority;
import com.example.task.entity.TaskStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private Priority priority;
    private LocalDate dueDate;
    private Long assignedUserId;
    private String assignedUserName;
    private TaskUserResponse assignedUser;
    private Long createdById;
    private String createdByName;
    private TaskUserResponse createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
