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

    /**
     * 指定ユーザー宛ての通知を新しい順に取得する。
     *
     * <p>{@code unreadOnly} がtrueの場合は未読通知のみを対象にする。</p>
     *
     * @param recipientUserId 受信者ユーザーID
     * @param unreadOnly 未読のみ取得する場合はtrue
     * @param pageable ページング条件
     * @return ページングされた通知
     */
    @EntityGraph(attributePaths = {"activityLog", "activityLog.actorUser", "activityLog.task"})
    // 一覧レスポンス変換で参照するアクティビティログ関連をまとめて取得する。
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

    /**
     * 指定ユーザー宛ての通知をIDで取得する。
     *
     * @param id 通知ID
     * @param recipientUserId 受信者ユーザーID
     * @return 条件に一致する通知
     */
    @EntityGraph(attributePaths = {"activityLog", "activityLog.actorUser", "activityLog.task"})
    Optional<Notification> findByIdAndRecipientUserId(Long id, Long recipientUserId);

    /**
     * 指定ユーザー宛ての未読通知件数を取得する。
     *
     * @param recipientUserId 受信者ユーザーID
     * @return 未読通知件数
     */
    long countByRecipientUserIdAndIsReadFalse(Long recipientUserId);

    /**
     * 指定ユーザー宛ての未読通知をまとめて既読にする。
     *
     * @param recipientUserId 受信者ユーザーID
     * @param readAt 既読日時
     * @return 更新された通知件数
     */
    @Modifying
    // 既読済み通知のreadAtを上書きしないよう、未読通知だけを更新する。
    @Query("""
            update Notification n
            set n.isRead = true,
                n.readAt = :readAt
            where n.recipientUser.id = :recipientUserId
              and n.isRead = false
            """)
    int markAllAsRead(@Param("recipientUserId") Long recipientUserId, @Param("readAt") LocalDateTime readAt);
}
