package com.example.task.repository;

import com.example.task.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 通知一覧、未読件数、既読化を扱うリポジトリ。
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @EntityGraph(attributePaths = {"activityLog", "activityLog.actorUser", "activityLog.task"})
    @Query("""
            select n
            from Notification n
            where n.recipientUser.id = :recipientUserId
              and (:unreadOnly = false or n.isRead = false)
            order by n.createdAt desc, n.id desc
            """)
    Page<Notification> findByRecipientUserId(
            @Param("recipientUserId") Long recipientUserId,
            @Param("unreadOnly") boolean unreadOnly,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"activityLog", "activityLog.actorUser", "activityLog.task"})
    Optional<Notification> findByIdAndRecipientUserId(Long id, Long recipientUserId);

    long countByRecipientUserIdAndIsReadFalse(Long recipientUserId);

    @Modifying
    @Query("""
            update Notification n
            set n.isRead = true,
                n.readAt = :readAt
            where n.recipientUser.id = :recipientUserId
              and n.isRead = false
            """)
    int markAllAsRead(@Param("recipientUserId") Long recipientUserId, @Param("readAt") LocalDateTime readAt);
}
