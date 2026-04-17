package com.example.task.service;

import com.example.task.dto.ActivityLogResponse;
import com.example.task.dto.PageResponse;
import com.example.task.dto.TaskUserResponse;
import com.example.task.entity.ActivityEventType;
import com.example.task.entity.ActivityLog;
import com.example.task.repository.ActivityLogRepository;
import com.example.task.security.CurrentUserProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * タスク別履歴一覧取得を扱うサービス。
 */
@Service
public class ActivityQueryService {

    private final ActivityLogRepository activityLogRepository;
    private final CurrentUserProvider currentUserProvider;
    private final TaskAuthorizationService taskAuthorizationService;

    public ActivityQueryService(
            ActivityLogRepository activityLogRepository,
            CurrentUserProvider currentUserProvider,
            TaskAuthorizationService taskAuthorizationService
    ) {
        this.activityLogRepository = activityLogRepository;
        this.currentUserProvider = currentUserProvider;
        this.taskAuthorizationService = taskAuthorizationService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ActivityLogResponse> getTaskActivities(Long taskId, int page, int size, ActivityEventType eventType) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        taskAuthorizationService.getViewableTask(taskId, currentUserId);
        Page<ActivityLog> result = activityLogRepository.findByTaskId(taskId, eventType, PageRequest.of(page, size));
        return PageResponse.<ActivityLogResponse>builder()
                .content(result.getContent().stream().map(this::toResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    private ActivityLogResponse toResponse(ActivityLog activityLog) {
        return ActivityLogResponse.builder()
                .id(activityLog.getId())
                .eventType(activityLog.getEventType())
                .actor(TaskUserResponse.builder()
                        .id(activityLog.getActorUser().getId())
                        .name(activityLog.getActorUser().getName())
                        .build())
                .targetType(activityLog.getTargetType())
                .targetId(activityLog.getTargetId())
                .taskId(activityLog.getTask() != null ? activityLog.getTask().getId() : null)
                .summary(activityLog.getSummary())
                .detailJson(activityLog.getDetailJson())
                .createdAt(activityLog.getCreatedAt())
                .build();
    }
}
