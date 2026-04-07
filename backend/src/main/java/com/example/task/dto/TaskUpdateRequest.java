package com.example.task.dto;

import com.example.task.entity.Priority;
import com.example.task.entity.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class TaskUpdateRequest {

    @NotBlank(message = "title is required")
    @Size(max = 200, message = "title must be 200 characters or less")
    private String title;

    @Size(max = 5000, message = "description must be 5000 characters or less")
    private String description;

    @NotNull(message = "status is required")
    private TaskStatus status;

    @NotNull(message = "priority is required")
    private Priority priority;

    private LocalDate dueDate;

    private Long assignedUserId;
}
