package com.example.task.controller;

import com.example.task.dto.NotificationResponse;
import com.example.task.dto.PageResponse;
import com.example.task.dto.UnreadCountResponse;
import com.example.task.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 通知一覧、未読件数、既読化 API。
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * コンストラクタ。
     *
     * @param notificationService 通知サービス
     */
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * 通知一覧を取得します。ページネーション対応。
     *
     * @param page ページ番号（デフォルト: 0）
     * @param size 1ページあたりの件数（デフォルト: 20）
     * @param unreadOnly true の場合は未読のみ取得（デフォルト: false）
     * @return ページネーション付きの通知レスポンスリスト
     */
    @GetMapping
    public ResponseEntity<PageResponse<NotificationResponse>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean unreadOnly
    ) {
        return ResponseEntity.ok(notificationService.getNotifications(page, size, unreadOnly));
    }

    /**
     * 未読の通知件数を取得します。
     *
     * @return 未読件数レスポンス
     */
    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount() {
        return ResponseEntity.ok(notificationService.getUnreadCount());
    }

    /**
     * 指定された通知を既読にします。
     *
     * @param notificationId 通知ID
     * @return 既読化後の通知レスポンス
     */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Long notificationId) {
        return ResponseEntity.ok(notificationService.markAsRead(notificationId));
    }

    /**
     * すべての通知を既読にします。
     *
     * @return ステータスコード 204 No Content
     */
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.noContent().build();
    }
}
