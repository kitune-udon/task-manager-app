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

    public void logAttachmentDownloaded(User actor, TaskAttachment attachment) {
        logAttachmentAudit("LOG-FILE-003", HttpStatus.OK.value(), attachment);
    }

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

    private List<User> commentRecipients(Task task) {
        return List.of(task.getCreatedBy(), task.getAssignedUser());
    }

    private void logTaskAudit(String eventId, int status, Long taskId, List<String> changedFields) {
        LinkedHashMap<String, Object> fields = structuredLogService.currentRequestFields(status, true, false, false);
        fields.put("taskId", taskId);
        if (changedFields != null && !changedFields.isEmpty()) {
            fields.put("changedFields", changedFields);
        }
        structuredLogService.infoAudit(eventId, "タスク監査ログ", fields);
    }

    private void logCommentAudit(String eventId, int status, Long taskId, Long commentId, List<String> changedFields) {
        LinkedHashMap<String, Object> fields = structuredLogService.currentRequestFields(status, true, false, false);
        fields.put("taskId", taskId);
        fields.put("commentId", commentId);
        if (changedFields != null && !changedFields.isEmpty()) {
            fields.put("changedFields", changedFields);
        }
        structuredLogService.infoAudit(eventId, "コメント監査ログ", fields);
    }

    private void logAttachmentAudit(String eventId, int status, TaskAttachment attachment) {
        LinkedHashMap<String, Object> fields = structuredLogService.currentRequestFields(status, true, false, false);
        fields.put("taskId", attachment.getTask().getId());
        fields.put("attachmentId", attachment.getId());
        fields.put("fileName", attachment.getOriginalFileName());
        fields.put("size", attachment.getFileSize());
        structuredLogService.infoAudit(eventId, "添付監査ログ", fields);
    }
}
