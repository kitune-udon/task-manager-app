package com.example.task.dto;

import com.example.task.entity.Priority;
import com.example.task.entity.TaskStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class TaskSummaryResponse {
    private Long id;
    private String title;
    private TaskStatus status;
    private Priority priority;
    private LocalDate dueDate;
    private TaskUserResponse assignedUser;
    private LocalDateTime updatedAt;
}
