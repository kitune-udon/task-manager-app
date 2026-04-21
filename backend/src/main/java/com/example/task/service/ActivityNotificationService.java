package com.example.task.service;

import com.example.task.entity.ActivityEventType;
import com.example.task.entity.ActivityLog;
import com.example.task.entity.ActivityTargetType;
import com.example.task.entity.Notification;
import com.example.task.entity.Task;
import com.example.task.entity.TaskAttachment;
import com.example.task.entity.TaskComment;
import com.example.task.entity.User;
import com.example.task.logging.StructuredLogService;
import com.example.task.repository.ActivityLogRepository;
import com.example.task.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * アクティビティログと通知生成を一貫して扱うサービス。
 */
@Service
public class ActivityNotificationService {

    private final ActivityLogRepository activityLogRepository;
    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;
    private final StructuredLogService structuredLogService;

    /**
     * コンストラクタ。
     *
     * @param activityLogRepository アクティビティログリポジトリ
     * @param notificationRepository 通知リポジトリ
     * @param objectMapper JSONマッパー
     * @param structuredLogService 構造化ログサービス
     */
    public ActivityNotificationService(
            ActivityLogRepository activityLogRepository,
            NotificationRepository notificationRepository,
            ObjectMapper objectMapper,
            StructuredLogService structuredLogService
    ) {
        this.activityLogRepository = activityLogRepository;
        this.notificationRepository = notificationRepository;
        this.objectMapper = objectMapper;
        this.structuredLogService = structuredLogService;
    }

    /**
     * タスク作成イベントを記録します。
     *
     * @param actor 操作者ユーザー
     * @param task 作成されたタスク
     */
    @Transactional
    public void recordTaskCreated(User actor, Task task) {
        ActivityLog activityLog = saveActivityLog(
                ActivityEventType.TASK_CREATED,
                ActivityTargetType.TASK,
                task.getId(),
                task,
                actor,
                actor.getName() + "さんがタスクを作成しました。",
                null
        );
        logTaskAudit("LOG-TASK-001", HttpStatus.CREATED.value(), task.getId(), null);
    }

    /**
     * タスク更新イベントを記録します。
     * 割り当てられた次第が変更した場合、担当者を暫定します。
     *
     * @param actor 操作者ユーザー
     * @param task 更新されたタスク
     * @param changedFields 変更されたフィールド名の一覧
     * @param detailJson 詳細情報JSON
     * @param previousAssigneeId 変更前の担当者ID
     */
    @Transactional
    public void recordTaskUpdated(User actor, Task task, List<String> changedFields, JsonNode detailJson, Long previousAssigneeId) {
        ActivityLog activityLog = saveActivityLog(
                ActivityEventType.TASK_UPDATED,
                ActivityTargetType.TASK,
                task.getId(),
                task,
                actor,
                actor.getName() + "さんがタスクを更新しました。",
                detailJson
        );
        logTaskAudit("LOG-TASK-002", HttpStatus.OK.value(), task.getId(), changedFields);

        Long currentAssigneeId = task.getAssignedUser() != null ? task.getAssignedUser().getId() : null;
        if (currentAssigneeId != null && !currentAssigneeId.equals(previousAssigneeId)) {
            createNotifications(activityLog, List.of(task.getAssignedUser()), actor.getId());
        }
    }

    /**
     * タスク削除イベントを記録します。
     *
     * @param actor 操作者ユーザー
     * @param task 削除されたタスク
     */
    @Transactional
    public void recordTaskDeleted(User actor, Task task) {
        saveActivityLog(
                ActivityEventType.TASK_DELETED,
                ActivityTargetType.TASK,
                task.getId(),
                task,
                actor,
                actor.getName() + "さんがタスクを削除しました。",
                null
        );
        logTaskAudit("LOG-TASK-003", HttpStatus.NO_CONTENT.value(), task.getId(), null);
    }

    /**
     * コメント作成イベントを記録し、論者任に通知を送信します。
     *
     * @param actor 操作者ユーザー
     * @param comment 作成されたコメント
     */
    @Transactional
    public void recordCommentCreated(User actor, TaskComment comment) {
        ActivityLog activityLog = saveActivityLog(
                ActivityEventType.COMMENT_CREATED,
                ActivityTargetType.COMMENT,
                comment.getId(),
                comment.getTask(),
                actor,
                actor.getName() + "さんがコメントを投稿しました。",
                objectMapper.valueToTree(java.util.Map.of("commentId", comment.getId()))
        );
        logCommentAudit("LOG-COMM-001", HttpStatus.CREATED.value(), comment.getTask().getId(), comment.getId(), null);
        createNotifications(activityLog, commentRecipients(comment.getTask()), actor.getId());
    }

    /**
     * コメント更新イベントを記録します。
     *
     * @param actor 操作者ユーザー
     * @param comment 更新されたコメント
     */
    @Transactional
    public void recordCommentUpdated(User actor, TaskComment comment) {
        saveActivityLog(
                ActivityEventType.COMMENT_UPDATED,
                ActivityTargetType.COMMENT,
                comment.getId(),
                comment.getTask(),
                actor,
                actor.getName() + "さんがコメントを更新しました。",
                objectMapper.valueToTree(java.util.Map.of("commentId", comment.getId()))
        );
        logCommentAudit("LOG-COMM-002", HttpStatus.OK.value(), comment.getTask().getId(), comment.getId(), List.of("content"));
    }

    /**
     * コメント削除イベントを記録します。
     *
     * @param actor 操作者ユーザー
     * @param comment 削除されたコメント
     */
    @Transactional
    public void recordCommentDeleted(User actor, TaskComment comment) {
        saveActivityLog(
                ActivityEventType.COMMENT_DELETED,
                ActivityTargetType.COMMENT,
                comment.getId(),
                comment.getTask(),
                actor,
                actor.getName() + "さんがコメントを削除しました。",
                objectMapper.valueToTree(java.util.Map.of("commentId", comment.getId()))
        );
        logCommentAudit("LOG-COMM-003", HttpStatus.NO_CONTENT.value(), comment.getTask().getId(), comment.getId(), null);
    }

    /**
     * 添付ファイルアップロードイベントを記録し、論者任に通知を送信します。
     *
     * @param actor 操作者ユーザー
     * @param attachment アップロードされた添付ファイル
     */
    @Transactional
    public void recordAttachmentUploaded(User actor, TaskAttachment attachment) {
        ActivityLog activityLog = saveActivityLog(
                ActivityEventType.ATTACHMENT_UPLOADED,
                ActivityTargetType.ATTACHMENT,
                attachment.getId(),
                attachment.getTask(),
                actor,
                actor.getName() + "さんがファイルを添付しました。",
                objectMapper.valueToTree(java.util.Map.of(
                        "attachmentId", attachment.getId(),
                        "fileName", attachment.getOriginalFileName()
                ))
        );
        logAttachmentAudit("LOG-FILE-002", HttpStatus.CREATED.value(), attachment);
        createNotifications(activityLog, commentRecipients(attachment.getTask()), actor.getId());
    }

    /**
     * 添付ファイル削除イベントを記録します。
     *
     * @param actor 操作者ユーザー
     * @param attachment 削除された添付ファイル
     */
    @Transactional
    public void recordAttachmentDeleted(User actor, TaskAttachment attachment) {
        saveActivityLog(
                ActivityEventType.ATTACHMENT_DELETED,
                ActivityTargetType.ATTACHMENT,
                attachment.getId(),
                attachment.getTask(),
                actor,
                actor.getName() + "さんが添付ファイルを削除しました。",
                objectMapper.valueToTree(java.util.Map.of(
                        "attachmentId", attachment.getId(),
                        "fileName", attachment.getOriginalFileName()
                ))
        );
        logAttachmentAudit("LOG-FILE-004", HttpStatus.NO_CONTENT.value(), attachment);
    }

    /**
     * 添付ファイルダウンロードイベントを記録します。
     *
     * @param actor 操作者ユーザー
     * @param attachment ダウンロードされた添付ファイル
     */
    public void logAttachmentDownloaded(User actor, TaskAttachment attachment) {
        logAttachmentAudit("LOG-FILE-003", HttpStatus.OK.value(), attachment);
    }

    /**
     * アクティビティログを保存します。
     *
     * @param eventType イベントタイプ
     * @param targetType 対象タイプ
     * @param targetId 対象ID
     * @param task タスク
     * @param actor 操作者ユーザー
     * @param summary サマリー
     * @param detailJson 詳細情報JSON
     * @return 保存されたアクティビティログ
     */
    private ActivityLog saveActivityLog(
            ActivityEventType eventType,
            ActivityTargetType targetType,
            Long targetId,
            Task task,
            User actor,
            String summary,
            com.fasterxml.jackson.databind.JsonNode detailJson
    ) {
        ActivityLog activityLog = ActivityLog.builder()
                .eventType(eventType)
                .actorUser(actor)
                .targetType(targetType)
                .targetId(targetId)
                .task(task)
                .summary(summary)
                .detailJson(detailJson)
                .build();
        return activityLogRepository.save(activityLog);
    }

    /**
     * 通知を作成して保存します。
     * 操作者自身を除外し、重複する対象を削除します。
     *
     * @param activityLog アクティビティログ
     * @param recipients 会接者ユーザーリスト
     * @param actorUserId 操作者ユーザーID
     */
    private void createNotifications(ActivityLog activityLog, List<User> recipients, Long actorUserId) {
        Set<Long> uniqueRecipientIds = new LinkedHashSet<>();
        List<Notification> notifications = new ArrayList<>();

        for (User candidate : recipients) {
            if (candidate == null || candidate.getId() == null) {
                continue;
            }
            if (candidate.getId().equals(actorUserId)) {
                continue;
            }
            if (!uniqueRecipientIds.add(candidate.getId())) {
                continue;
            }
            notifications.add(Notification.builder()
                    .recipientUser(candidate)
                    .activityLog(activityLog)
                    .isRead(false)
                    .build());
        }

        if (!notifications.isEmpty()) {
            notificationRepository.saveAll(notifications);
        }
    }

    /**
     * コメント受信者を取得します。論者任と割り当てられた者を返します。
     *
     * @param task タスク
     * @return コメント受信者ユーザーリスト
     */
    private List<User> commentRecipients(Task task) {
        return List.of(task.getCreatedBy(), task.getAssignedUser());
    }

    /**
     * タスク監査ログを記録します。
     *
     * @param eventId 詳細事象ID
     * @param status HTTPステータスコード
     * @param taskId タスクID
     * @param changedFields 変更されたフィールド名の一覧
     */
    private void logTaskAudit(String eventId, int status, Long taskId, List<String> changedFields) {
        LinkedHashMap<String, Object> fields = structuredLogService.currentRequestFields(status, true, false, false);
        fields.put("taskId", taskId);
        if (changedFields != null && !changedFields.isEmpty()) {
            fields.put("changedFields", changedFields);
        }
        structuredLogService.infoAudit(eventId, "タスク監査ログ", fields);
    }

    /**
     * コメント監査ログを記録します。
     *
     * @param eventId 詳細事象ID
     * @param status HTTPステータスコード
     * @param taskId タスクID
     * @param commentId コメントID
     * @param changedFields 変更されたフィールド名の一覧
     */
    private void logCommentAudit(String eventId, int status, Long taskId, Long commentId, List<String> changedFields) {
        LinkedHashMap<String, Object> fields = structuredLogService.currentRequestFields(status, true, false, false);
        fields.put("taskId", taskId);
        fields.put("commentId", commentId);
        if (changedFields != null && !changedFields.isEmpty()) {
            fields.put("changedFields", changedFields);
        }
        structuredLogService.infoAudit(eventId, "コメント監査ログ", fields);
    }

    /**
     * 添付ファイル監査ログを記録します。
     *
     * @param eventId 詳細事象ID
     * @param status HTTPステータスコード
     * @param attachment 添付ファイル
     */
    private void logAttachmentAudit(String eventId, int status, TaskAttachment attachment) {
        LinkedHashMap<String, Object> fields = structuredLogService.currentRequestFields(status, true, false, false);
        fields.put("taskId", attachment.getTask().getId());
        fields.put("attachmentId", attachment.getId());
        fields.put("fileName", attachment.getOriginalFileName());
        fields.put("size", attachment.getFileSize());
        structuredLogService.infoAudit(eventId, "添付監査ログ", fields);
    }
}
