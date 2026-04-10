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
import com.example.task.exception.BusinessException;
import com.example.task.exception.ErrorCode;
import com.example.task.exception.ResourceNotFoundException;
import com.example.task.repository.TaskRepository;
import com.example.task.repository.UserRepository;
import com.example.task.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * タスクの作成、参照、更新、削除と権限制御をまとめるサービス。
 */
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

    /**
     * リクエストを Task エンティティへ変換し、作成者情報を補って保存する。
     */
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

    /**
     * ログインユーザーが参照可能なタスクを検索条件付きで一覧化する。
     */
    @Transactional(readOnly = true)
    public List<TaskSummaryResponse> getTasks(TaskStatus status, Priority priority, Long assignedUserId, String keyword) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        String keywordPattern = toKeywordPattern(keyword);

        return taskRepository.searchAccessible(currentUserId, status, priority, assignedUserId, keywordPattern)
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    /**
     * 単一タスクを取得し、閲覧権限がある場合だけ詳細レスポンスへ変換する。
     */
    @Transactional(readOnly = true)
    public TaskResponse getTask(Long taskId) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        Task task = getTaskEntity(taskId);
        authorizeTaskView(task, currentUserId);
        return toDetailResponse(task);
    }

    /**
     * 更新対象の存在確認と権限確認を行ってから内容を書き換える。
     */
    @Transactional
    public TaskResponse updateTask(Long taskId, TaskUpdateRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        Task task = getTaskEntity(taskId);
        authorizeTaskUpdate(task, currentUserId);
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());
        task.setPriority(request.getPriority());
        task.setDueDate(request.getDueDate());
        task.setAssignedUser(resolveAssignedUser(request.getAssignedUserId()));

        Task saved = taskRepository.save(task);
        return toDetailResponse(saved);
    }

    /**
     * 作成者権限を確認したうえでタスクを物理削除する。
     */
    @Transactional
    public void deleteTask(Long taskId) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        Task task = getTaskEntity(taskId);
        authorizeTaskDelete(task, currentUserId);
        taskRepository.delete(task);
    }

    /**
     * 詳細取得や更新で共通利用するタスク取得処理。
     */
    private Task getTaskEntity(Long taskId) {
        return taskRepository.findWithAssignedUserById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.RES_TASK_404,
                        "タスクが存在しません"
                ));
    }

    /**
     * 担当者 ID が指定された場合のみ関連ユーザーを解決する。
     */
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

    /**
     * 認証済みユーザーを Task.createdBy に設定するためにエンティティ化する。
     */
    private User resolveCurrentUser() {
        Long currentUserId = currentUserProvider.getCurrentUserId();

        return userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.USR_002,
                        "ユーザーが存在しません"
                ));
    }

    /**
     * 閲覧可能条件を満たさない場合は 403 エラーへ変換する。
     */
    private void authorizeTaskView(Task task, Long currentUserId) {
        if (canViewTask(task, currentUserId)) {
            return;
        }

        throw new BusinessException(ErrorCode.AUTH_005, "対象タスクの参照権限がありません");
    }

    /**
     * 更新権限は閲覧可能かどうかと同じ条件で判定する。
     */
    private void authorizeTaskUpdate(Task task, Long currentUserId) {
        if (canUpdateTask(task, currentUserId)) {
            return;
        }

        throw new BusinessException(ErrorCode.PERM_TASK_403_UPD);
    }

    /**
     * 削除は作成者本人のみ許可する。
     */
    private void authorizeTaskDelete(Task task, Long currentUserId) {
        if (canDeleteTask(task, currentUserId)) {
            return;
        }

        throw new BusinessException(ErrorCode.PERM_TASK_403_DEL);
    }

    private boolean canViewTask(Task task, Long currentUserId) {
        return isTaskCreator(task, currentUserId) || isTaskAssignee(task, currentUserId);
    }

    private boolean canUpdateTask(Task task, Long currentUserId) {
        return canViewTask(task, currentUserId);
    }

    private boolean canDeleteTask(Task task, Long currentUserId) {
        return isTaskCreator(task, currentUserId);
    }

    private boolean isTaskCreator(Task task, Long currentUserId) {
        User createdBy = task.getCreatedBy();
        return createdBy != null && currentUserId.equals(createdBy.getId());
    }

    private boolean isTaskAssignee(Task task, Long currentUserId) {
        User assignedUser = task.getAssignedUser();
        return assignedUser != null && currentUserId.equals(assignedUser.getId());
    }

    /**
     * 部分一致検索用に前後へワイルドカードを付与し、小文字で統一する。
     */
    private String toKeywordPattern(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
    }

    /**
     * 一覧表示向けに必要最小限の項目へ絞って変換する。
     */
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

    /**
     * 詳細表示用に関連ユーザー情報も含めた DTO へ変換する。
     */
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

    /**
     * タスク関連レスポンスで共通利用する簡易ユーザー表現へ変換する。
     */
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
