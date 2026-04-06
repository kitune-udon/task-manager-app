package com.example.task.service;

import com.example.task.dto.TaskCreateRequest;
import com.example.task.dto.TaskResponse;
import com.example.task.dto.TaskUpdateRequest;
import com.example.task.entity.Priority;
import com.example.task.entity.Task;
import com.example.task.entity.TaskStatus;
import com.example.task.entity.User;
import com.example.task.exception.ResourceNotFoundException;
import com.example.task.repository.TaskRepository;
import com.example.task.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TaskResponse createTask(TaskCreateRequest request) {
        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(request.getStatus())
                .priority(request.getPriority())
                .dueDate(request.getDueDate())
                .assignedUser(resolveAssignedUser(request.getAssignedUserId()))
                .build();

        Task saved = taskRepository.save(task);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasks(TaskStatus status, Priority priority, Long assignedUserId, String keyword) {
        String keywordPattern = toKeywordPattern(keyword);

        return taskRepository.search(status, priority, assignedUserId, keywordPattern)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(Long taskId) {
        return toResponse(getTaskEntity(taskId));
    }

    @Transactional
    public TaskResponse updateTask(Long taskId, TaskUpdateRequest request) {
        Task task = getTaskEntity(taskId);
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());
        task.setPriority(request.getPriority());
        task.setDueDate(request.getDueDate());
        task.setAssignedUser(resolveAssignedUser(request.getAssignedUserId()));

        Task saved = taskRepository.save(task);
        return toResponse(saved);
    }

    @Transactional
    public void deleteTask(Long taskId) {
        Task task = getTaskEntity(taskId);
        taskRepository.delete(task);
    }

    private Task getTaskEntity(Long taskId) {
        return taskRepository.findWithAssignedUserById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found. taskId=" + taskId));
    }

    private User resolveAssignedUser(Long assignedUserId) {
        if (assignedUserId == null) {
            return null;
        }

        return userRepository.findById(assignedUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Assigned user not found. userId=" + assignedUserId));
    }

    private String toKeywordPattern(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private TaskResponse toResponse(Task task) {
        User assignedUser = task.getAssignedUser();
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .assignedUserId(assignedUser != null ? assignedUser.getId() : null)
                .assignedUserName(assignedUser != null ? assignedUser.getName() : null)
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}