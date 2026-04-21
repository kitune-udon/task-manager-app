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

    /**
     * コンストラクタ。
     *
     * @param taskCommentRepository コメントリポジトリ
     * @param userRepository ユーザーリポジトリ
     * @param currentUserProvider 現在のユーザー提供者
     * @param taskAuthorizationService タスク認可サービス
     * @param activityNotificationService アクティビティ通知サービス
     */
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

    /**
     * 指定されたタスクのコメント一覧を取得します。ページネーション対応。
     * 現在のユーザーがタスクを閲覧可能であることを確認してから取得します。
     *
     * @param taskId タスクID
     * @param page ページ番号
     * @param size 1ページあたりの件数
     * @return ページネーション付きのコメントレスポンスリスト
     */
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

    /**
     * タスクにコメントを作成します。
     * 現在のユーザーがタスクを閲覧可能であることを確認してから作成します。
     *
     * @param taskId タスクID
     * @param request コメント作成リクエスト
     * @return 作成されたコメントレスポンス
     */
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

    /**
     * コメントを更新します。
     * 所有者を確認し、断蔵不一致検証を処理します。
     *
     * @param commentId コメントID
     * @param request コメント更新リクエスト
     * @return 更新されたコメントレスポンス
     * @throws BusinessException 所有者でない場合
     * @throws ConflictException バージョン不一致時
     */
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

    /**
     * コメントを削除します。
     * 所有者のみ削除可能で、論理削除（論理削除）を処理します。
     *
     * @param commentId コメントID
     * @throws BusinessException 所有者でない場合
     * @throws ResourceNotFoundException コメントが見つからない場合
     */
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

    /**
     * 删除されていないコメントを取得します。
     *
     * @param commentId コメントID
     * @return アクティブなコメント
     * @throws ResourceNotFoundException コメントが見つからない場合
     */
    private TaskComment getActiveComment(Long commentId) {
        return taskCommentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.COMMENT_002, "コメントが存在しません"));
    }

    /**
     * 覚正事武がアクティブであることを確認します。
     *
     * @param comment 検証するコメント
     * @throws ResourceNotFoundException タスクが存在しない場合
     */
    private void ensureParentTaskActive(TaskComment comment) {
        if (comment.getTask() == null || comment.getTask().getDeletedAt() != null) {
            throw new ResourceNotFoundException(ErrorCode.RES_TASK_404, "タスクが存在しません");
        }
    }

    /**
     * 現在ログイン中のユーザーを取得します。
     *
     * @return 現在のユーザー
     * @throws ResourceNotFoundException ユーザーが見つからない場合
     */
    private User resolveCurrentUser() {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        return userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USR_002, "ユーザーが存在しません"));
    }

    /**
     * コメントをレスポンスDTOに変換します。
     *
     * @param comment コメントエンティティ
     * @return コメントレスポンスDTO
     */
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

    /**
     * ユーザーをタスクユーザーレスポンスに変換します。
     *
     * @param user ユーザー
     * @return タスクユーザーレスポンスDTO
     */
    private TaskUserResponse toTaskUserResponse(User user) {
        return TaskUserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .build();
    }
}
