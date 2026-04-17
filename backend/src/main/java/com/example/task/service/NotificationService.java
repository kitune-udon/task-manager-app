package com.example.task.service;

import com.example.task.dto.NotificationResponse;
import com.example.task.dto.PageResponse;
import com.example.task.dto.UnreadCountResponse;
import com.example.task.entity.Notification;
import com.example.task.entity.User;
import com.example.task.exception.BusinessException;
import com.example.task.exception.ErrorCode;
import com.example.task.exception.ResourceNotFoundException;
import com.example.task.repository.NotificationRepository;
import com.example.task.repository.UserRepository;
import com.example.task.security.CurrentUserProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 通知一覧、未読件数、既読化を扱うサービス。
 */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            CurrentUserProvider currentUserProvider
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotifications(int page, int size, boolean unreadOnly) {
        User currentUser = resolveCurrentUser();
        Page<Notification> result = notificationRepository.findByRecipientUserId(currentUser.getId(), unreadOnly, PageRequest.of(page, size));
        return PageResponse.<NotificationResponse>builder()
                .content(result.getContent().stream().map(this::toResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount() {
        User currentUser = resolveCurrentUser();
        return new UnreadCountResponse(notificationRepository.countByRecipientUserIdAndIsReadFalse(currentUser.getId()));
    }

    @Transactional
    public NotificationResponse markAsRead(Long notificationId) {
        User currentUser = resolveCurrentUser();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.NOTIFY_001, "通知が存在しません"));

        if (!notification.getRecipientUser().getId().equals(currentUser.getId())) {
            throw new BusinessException(ErrorCode.NOTIFY_002);
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
        }

        return toResponse(notification);
    }

    @Transactional
    public void markAllAsRead() {
        User currentUser = resolveCurrentUser();
        notificationRepository.markAllAsRead(currentUser.getId(), LocalDateTime.now());
    }

    private User resolveCurrentUser() {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        return userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USR_002, "ユーザーが存在しません"));
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .activityLogId(notification.getActivityLog().getId())
                .eventType(notification.getActivityLog().getEventType())
                .message(notification.getActivityLog().getSummary())
                .relatedTaskId(notification.getActivityLog().getTask() != null ? notification.getActivityLog().getTask().getId() : null)
                .relatedTaskTitle(notification.getActivityLog().getTask() != null ? notification.getActivityLog().getTask().getTitle() : null)
                .targetType(notification.getActivityLog().getTargetType())
                .targetId(notification.getActivityLog().getTargetId())
                .isRead(notification.isRead())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
