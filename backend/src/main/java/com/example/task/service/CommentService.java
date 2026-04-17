package com.example.task.service;

import com.example.task.dto.CommentCreateRequest;
import com.example.task.dto.CommentResponse;
import com.example.task.dto.CommentUpdateRequest;
import com.example.task.dto.PageResponse;
import com.example.task.dto.TaskUserResponse;
import com.example.task.entity.Task;
import com.example.task.entity.TaskComment;
import com.example.task.entity.User;
import com.example.task.exception.BusinessException;
import com.example.task.exception.ConflictException;
import com.example.task.exception.ErrorCode;
import com.example.task.exception.ResourceNotFoundException;
import com.example.task.repository.TaskCommentRepository;
import com.example.task.repository.UserRepository;
import com.example.task.security.CurrentUserProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * コメント一覧・投稿・更新・削除を扱うサービス。
 */
@Service
public class CommentService {

    private final TaskCommentRepository taskCommentRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final TaskAuthorizationService taskAuthorizationService;
    private final ActivityNotificationService activityNotificationService;

    public CommentService(
            TaskCommentRepository taskCommentRepository,
            UserRepository userRepository,
            CurrentUserProvider currentUserProvider,
            TaskAuthorizationService taskAuthorizationService,
            ActivityNotificationService activityNotificationService
    ) {
        this.taskCommentRepository = taskCommentRepository;
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
        this.taskAuthorizationService = taskAuthorizationService;
        this.activityNotificationService = activityNotificationService;
    }

    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> getComments(Long taskId, int page, int size) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        Task task = taskAuthorizationService.getViewableTask(taskId, currentUserId);
        Page<TaskComment> result = taskCommentRepository.findActiveByTaskId(task.getId(), PageRequest.of(page, size));
        return PageResponse.<CommentResponse>builder()
                .content(result.getContent().stream().map(this::toResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Transactional
    public CommentResponse createComment(Long taskId, CommentCreateRequest request) {
        User currentUser = resolveCurrentUser();
        Task task = taskAuthorizationService.getViewableTask(taskId, currentUser.getId());

        TaskComment comment = TaskComment.builder()
                .task(task)
                .content(request.getContent().trim())
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();

        TaskComment saved = taskCommentRepository.save(comment);
        activityNotificationService.recordCommentCreated(currentUser, saved);
        return toResponse(saved);
    }

    @Transactional
    public CommentResponse updateComment(Long commentId, CommentUpdateRequest request) {
        User currentUser = resolveCurrentUser();
        TaskComment comment = getActiveComment(commentId);
        ensureParentTaskActive(comment);
        taskAuthorizationService.authorizeView(comment.getTask(), currentUser.getId());

        if (!currentUser.getId().equals(comment.getCreatedBy().getId())) {
            throw new BusinessException(ErrorCode.COMMENT_004);
        }
        if (!request.getVersion().equals(comment.getVersion())) {
            throw new ConflictException(ErrorCode.COMMENT_006);
        }

        comment.setContent(request.getContent().trim());
        comment.setUpdatedBy(currentUser);

        try {
            TaskComment saved = taskCommentRepository.saveAndFlush(comment);
            activityNotificationService.recordCommentUpdated(currentUser, saved);
            return toResponse(saved);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new ConflictException(ErrorCode.COMMENT_006);
        }
    }

    @Transactional
    public void deleteComment(Long commentId) {
        User currentUser = resolveCurrentUser();
        TaskComment comment = getActiveComment(commentId);
        ensureParentTaskActive(comment);
        taskAuthorizationService.authorizeView(comment.getTask(), currentUser.getId());

        if (!currentUser.getId().equals(comment.getCreatedBy().getId())) {
            throw new BusinessException(ErrorCode.COMMENT_005);
        }

        comment.setDeletedAt(LocalDateTime.now());
        comment.setDeletedBy(currentUser);
        taskCommentRepository.save(comment);
        activityNotificationService.recordCommentDeleted(currentUser, comment);
    }

    private TaskComment getActiveComment(Long commentId) {
        return taskCommentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.COMMENT_002, "コメントが存在しません"));
    }

    private void ensureParentTaskActive(TaskComment comment) {
        if (comment.getTask() == null || comment.getTask().getDeletedAt() != null) {
            throw new ResourceNotFoundException(ErrorCode.RES_TASK_404, "タスクが存在しません");
        }
    }

    private User resolveCurrentUser() {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        return userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USR_002, "ユーザーが存在しません"));
    }

    private CommentResponse toResponse(TaskComment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .taskId(comment.getTask().getId())
                .content(comment.getContent())
                .createdBy(toTaskUserResponse(comment.getCreatedBy()))
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .version(comment.getVersion())
                .build();
    }

    private TaskUserResponse toTaskUserResponse(User user) {
        return TaskUserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .build();
    }
}
