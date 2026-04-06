package com.example.task.controller;

import com.example.task.dto.ApiResponse;
import com.example.task.dto.TaskCreateRequest;
import com.example.task.dto.TaskResponse;
import com.example.task.dto.TaskUpdateRequest;
import com.example.task.entity.Priority;
import com.example.task.entity.TaskStatus;
import com.example.task.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(@Valid @RequestBody TaskCreateRequest request) {
        TaskResponse response = taskService.createTask(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<TaskResponse>builder()
                        .success(true)
                        .data(response)
                        .message("Task created successfully.")
                        .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getTasks(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Long assignedUserId,
            @RequestParam(required = false) String keyword
    ) {
        List<TaskResponse> response = taskService.getTasks(status, priority, assignedUserId, keyword);
        return ResponseEntity.ok(ApiResponse.<List<TaskResponse>>builder()
                .success(true)
                .data(response)
                .message("Tasks fetched successfully.")
                .build());
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<ApiResponse<TaskResponse>> getTask(@PathVariable Long taskId) {
        TaskResponse response = taskService.getTask(taskId);
        return ResponseEntity.ok(ApiResponse.<TaskResponse>builder()
                .success(true)
                .data(response)
                .message("Task fetched successfully.")
                .build());
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskUpdateRequest request
    ) {
        TaskResponse response = taskService.updateTask(taskId, request);
        return ResponseEntity.ok(ApiResponse.<TaskResponse>builder()
                .success(true)
                .data(response)
                .message("Task updated successfully.")
                .build());
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long taskId) {
        taskService.deleteTask(taskId);
        return ResponseEntity.noContent().build();
    }
}
