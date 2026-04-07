package com.example.task.service;

import com.example.task.dto.TaskCreateRequest;
import com.example.task.dto.TaskResponse;
import com.example.task.dto.TaskSummaryResponse;
import com.example.task.dto.TaskUserResponse;
import com.example.task.dto.TaskUpdateRequest;
import com.example.task.entity.Priority;
import com.example.task.entity.Task;
import com.example.task.entity.TaskStatus;
import com.example.task.entity.User;
import com.example.task.exception.ErrorCode;
import com.example.task.exception.ResourceNotFoundException;
import com.example.task.repository.TaskRepository;
import com.example.task.repository.UserRepository;
import com.example.task.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;

    public TaskService(
            TaskRepository taskRepository,
            UserRepository userRepository,
            CurrentUserProvider currentUserProvider
    ) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
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
                .createdBy(resolveCurrentUser())
                .build();

        Task saved = taskRepository.save(task);
        return toDetailResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TaskSummaryResponse> getTasks(TaskStatus status, Priority priority, Long assignedUserId, String keyword) {
        String keywordPattern = toKeywordPattern(keyword);

        return taskRepository.search(status, priority, assignedUserId, keywordPattern)
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(Long taskId) {
        return toDetailResponse(getTaskEntity(taskId));
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
        return toDetailResponse(saved);
    }

    @Transactional
    public void deleteTask(Long taskId) {
        Task task = getTaskEntity(taskId);
        taskRepository.delete(task);
    }

    private Task getTaskEntity(Long taskId) {
        return taskRepository.findWithAssignedUserById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.RES_TASK_404,
                        "タスクが存在しません"
                ));
    }

    private User resolveAssignedUser(Long assignedUserId) {
        if (assignedUserId == null) {
            return null;
        }

        return userRepository.findById(assignedUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.USR_002,
                        "ユーザーが存在しません"
                ));
    }

    private User resolveCurrentUser() {
        Long currentUserId = currentUserProvider.getCurrentUserId();

        return userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.USR_002,
                        "ユーザーが存在しません"
                ));
    }

    private String toKeywordPattern(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private TaskSummaryResponse toSummaryResponse(Task task) {
        User assignedUser = task.getAssignedUser();

        return TaskSummaryResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .assignedUser(toTaskUserResponse(assignedUser))
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    private TaskResponse toDetailResponse(Task task) {
        User assignedUser = task.getAssignedUser();
        User createdBy = task.getCreatedBy();

        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .assignedUser(toTaskUserResponse(assignedUser))
                .createdBy(toTaskUserResponse(createdBy))
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    private TaskUserResponse toTaskUserResponse(User user) {
        if (user == null) {
            return null;
        }

        return TaskUserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .build();
    }
}
