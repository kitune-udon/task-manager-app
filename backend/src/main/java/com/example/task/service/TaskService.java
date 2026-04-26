package com.example.task.service;

import com.example.task.dto.TaskCreateRequest;
import com.example.task.dto.TaskResponse;
import com.example.task.dto.TaskSummaryResponse;
import com.example.task.dto.TaskUserResponse;
import com.example.task.dto.TaskUpdateRequest;
import com.example.task.entity.Priority;
import com.example.task.entity.Task;
import com.example.task.entity.TaskStatus;
import com.example.task.entity.Team;
import com.example.task.entity.TeamMember;
import com.example.task.entity.User;
import com.example.task.exception.BusinessException;
import com.example.task.exception.ConflictException;
import com.example.task.exception.ErrorCode;
import com.example.task.exception.ResourceNotFoundException;
import com.example.task.repository.TaskAttachmentRepository;
import com.example.task.repository.TaskCommentRepository;
import com.example.task.repository.TaskRepository;
import com.example.task.repository.TeamMemberRepository;
import com.example.task.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.task.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;

/**
 * タスクの作成、参照、更新、削除と権限制御をまとめるサービス。
 */
@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final TaskAttachmentRepository taskAttachmentRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final TaskAuthorizationService taskAuthorizationService;
    private final ActivityNotificationService activityNotificationService;
    private final ObjectMapper objectMapper;

    public TaskService(
            TaskRepository taskRepository,
            TaskCommentRepository taskCommentRepository,
            TaskAttachmentRepository taskAttachmentRepository,
            TeamMemberRepository teamMemberRepository,
            UserRepository userRepository,
            CurrentUserProvider currentUserProvider,
            TaskAuthorizationService taskAuthorizationService,
            ActivityNotificationService activityNotificationService,
            ObjectMapper objectMapper
    ) {
        this.taskRepository = taskRepository;
        this.taskCommentRepository = taskCommentRepository;
        this.taskAttachmentRepository = taskAttachmentRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
        this.taskAuthorizationService = taskAuthorizationService;
        this.activityNotificationService = activityNotificationService;
        this.objectMapper = objectMapper;
    }

    /**
     * リクエストを Task エンティティへ変換し、作成者情報を補って保存する。
     */
    @Transactional
    public TaskResponse createTask(TaskCreateRequest request) {
        User currentUser = resolveCurrentUser();
        TeamMember membership = resolveTaskCreateMembership(currentUser.getId(), request.getTeamId());
        Team team = membership.getTeam();
        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(request.getStatus())
                .priority(request.getPriority())
                .dueDate(request.getDueDate())
                .assignedUser(resolveAssignedUserInTeam(request.getAssignedUserId(), team.getId()))
                .createdBy(currentUser)
                .team(team)
                .build();

        Task saved = taskRepository.save(task);
        activityNotificationService.recordTaskCreated(currentUser, saved);
        return toDetailResponse(saved);
    }

    /**
     * ログインユーザーが参照可能なタスクを検索条件付きで一覧化する。
     */
    @Transactional(readOnly = true)
    public List<TaskSummaryResponse> getTasks(
            TaskStatus status,
            Priority priority,
            Long assignedUserId,
            Long teamId,
            String keyword
    ) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        if (teamId != null) {
            taskAuthorizationService.authorizeTaskListInTeam(currentUserId, teamId);
        }
        String keywordPattern = toKeywordPattern(keyword);

        return taskRepository.searchAccessible(currentUserId, teamId, status, priority, assignedUserId, keywordPattern)
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
        Task task = taskAuthorizationService.getViewableTask(taskId, currentUserId);
        return toDetailResponse(task);
    }

    /**
     * 更新対象の存在確認と権限確認を行ってから内容を書き換える。
     */
    @Transactional
    public TaskResponse updateTask(Long taskId, TaskUpdateRequest request) {
        User currentUser = resolveCurrentUser();
        Long currentUserId = currentUserProvider.getCurrentUserId();
        Task task = taskAuthorizationService.getUpdatableTask(taskId, currentUserId);

        if (!Objects.equals(task.getVersion(), request.getVersion())) {
            throw new ConflictException(ErrorCode.TASK_007);
        }

        Long previousAssigneeId = extractUserId(task.getAssignedUser());
        List<String> changedFields = resolveChangedFields(task, request);
        JsonNode detailJson = resolveTaskDetailJson(task, request);
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());
        task.setPriority(request.getPriority());
        task.setDueDate(request.getDueDate());
        task.setAssignedUser(resolveAssignedUserInTeam(request.getAssignedUserId(), task.getTeam().getId()));

        try {
            Task saved = taskRepository.saveAndFlush(task);
            activityNotificationService.recordTaskUpdated(currentUser, saved, changedFields, detailJson, previousAssigneeId);
            return toDetailResponse(saved);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new ConflictException(ErrorCode.TASK_007);
        }
    }

    /**
     * 削除権限を確認したうえでタスクと子データを論理削除する。
     */
    @Transactional
    public void deleteTask(Long taskId) {
        User currentUser = resolveCurrentUser();
        Long currentUserId = currentUserProvider.getCurrentUserId();
        Task task = taskAuthorizationService.getDeletableTask(taskId, currentUserId);
        LocalDateTime deletedAt = LocalDateTime.now();
        task.setDeletedAt(deletedAt);
        task.setDeletedBy(currentUser);
        taskRepository.save(task);
        taskCommentRepository.softDeleteActiveByTaskId(task.getId(), deletedAt, currentUser);
        taskAttachmentRepository.softDeleteActiveByTaskId(task.getId(), deletedAt, currentUser);
        activityNotificationService.recordTaskDeleted(currentUser, task);
    }

    /**
     * 詳細取得や更新で共通利用するタスク取得処理。
     */
    private Task getTaskEntity(Long taskId) {
        return taskRepository.findWithAssignedUserByIdAndDeletedAtIsNull(taskId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.RES_TASK_404,
                        "タスクが存在しません"
                ));
    }

    /**
     * 担当者 ID が指定された場合のみ、同一チーム所属ユーザーとして解決する。
     */
    private User resolveAssignedUserInTeam(Long assignedUserId, Long teamId) {
        if (assignedUserId == null) {
            return null;
        }

        User assignedUser = userRepository.findById(assignedUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.USR_002,
                        "ユーザーが存在しません"
                ));

        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, assignedUser.getId())) {
            throw new BusinessException(ErrorCode.TASK_010);
        }

        return assignedUser;
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
        Team team = task.getTeam();

        return TaskSummaryResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .assignedUser(toTaskUserResponse(assignedUser))
                .teamId(team.getId())
                .teamName(team.getName())
                .updatedAt(task.getUpdatedAt())
                .version(task.getVersion())
                .build();
    }

    /**
     * 詳細表示用に関連ユーザー情報も含めた DTO へ変換する。
     */
    private TaskResponse toDetailResponse(Task task) {
        User assignedUser = task.getAssignedUser();
        User createdBy = task.getCreatedBy();
        Team team = task.getTeam();

        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .assignedUser(toTaskUserResponse(assignedUser))
                .createdBy(toTaskUserResponse(createdBy))
                .teamId(team.getId())
                .teamName(team.getName())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .version(task.getVersion())
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

    private List<String> resolveChangedFields(Task task, TaskUpdateRequest request) {
        List<String> changedFields = new ArrayList<>();

        if (!Objects.equals(task.getTitle(), request.getTitle())) {
            changedFields.add("title");
        }
        if (!Objects.equals(task.getDescription(), request.getDescription())) {
            changedFields.add("description");
        }
        if (!Objects.equals(task.getStatus(), request.getStatus())) {
            changedFields.add("status");
        }
        if (!Objects.equals(task.getPriority(), request.getPriority())) {
            changedFields.add("priority");
        }
        if (!Objects.equals(task.getDueDate(), request.getDueDate())) {
            changedFields.add("dueDate");
        }
        if (!Objects.equals(extractUserId(task.getAssignedUser()), request.getAssignedUserId())) {
            changedFields.add("assignedUserId");
        }

        return changedFields;
    }

    private JsonNode resolveTaskDetailJson(Task task, TaskUpdateRequest request) {
        List<LinkedHashMap<String, Object>> changes = new ArrayList<>();

        appendTaskChange(changes, "title", task.getTitle(), request.getTitle());
        appendTaskChange(changes, "description", task.getDescription(), request.getDescription());
        appendTaskChange(changes, "status", task.getStatus(), request.getStatus());
        appendTaskChange(changes, "priority", task.getPriority(), request.getPriority());
        appendTaskChange(changes, "dueDate", task.getDueDate(), request.getDueDate());
        appendTaskChange(changes, "assignedUserId", extractUserId(task.getAssignedUser()), request.getAssignedUserId());

        if (changes.isEmpty()) {
            return null;
        }

        return objectMapper.valueToTree(java.util.Map.of("changes", changes));
    }

    private void appendTaskChange(List<LinkedHashMap<String, Object>> changes, String field, Object oldValue, Object newValue) {
        Object normalizedOldValue = normalizeActivityValue(oldValue);
        Object normalizedNewValue = normalizeActivityValue(newValue);

        if (Objects.equals(normalizedOldValue, normalizedNewValue)) {
            return;
        }

        LinkedHashMap<String, Object> change = new LinkedHashMap<>();
        change.put("field", field);
        change.put("oldValue", normalizedOldValue);
        change.put("newValue", normalizedNewValue);
        changes.add(change);
    }

    private Object normalizeActivityValue(Object value) {
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        if (value instanceof java.time.LocalDate dateValue) {
            return dateValue.toString();
        }
        return value;
    }

    private Long extractUserId(User user) {
        return user != null ? user.getId() : null;
    }

    private TeamMember resolveTaskCreateMembership(Long currentUserId, Long teamId) {
        if (teamId == null) {
            throw new BusinessException(ErrorCode.TASK_008);
        }
        return taskAuthorizationService.authorizeTaskCreateInTeam(currentUserId, teamId);
    }
}
