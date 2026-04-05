package com.example.task.dto;

import com.example.task.entity.Priority;
import com.example.task.entity.TaskStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class TaskCreateRequest {
    private String title;
    private String description;
    private TaskStatus status;
    private Priority priority;
    private LocalDateTime dueDate;
    private Long assignedUserId;
}