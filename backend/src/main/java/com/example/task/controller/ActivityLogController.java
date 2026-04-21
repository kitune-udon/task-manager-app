package com.example.task.controller;

import com.example.task.dto.ActivityLogResponse;
import com.example.task.dto.PageResponse;
import com.example.task.entity.ActivityEventType;
import com.example.task.service.ActivityQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * アクティビティログ一覧取得 API。
 */
@RestController
public class ActivityLogController {

    private final ActivityQueryService activityQueryService;

    /**
     * コンストラクタ。
     *
     * @param activityQueryService アクティビティログクエリサービス
     */
    public ActivityLogController(ActivityQueryService activityQueryService) {
        this.activityQueryService = activityQueryService;
    }

    /**
     * タスクのアクティビティログ一覧を取得します。ページネーション対応、イベントタイプでのフィルタリング可能。
     *
     * @param taskId タスクID
     * @param page ページ番号（デフォルト: 0）
     * @param size 1ページあたりの件数（デフォルト: 20）
     * @param eventType イベントタイプ（オプション）
     * @return ページネーション付きのアクティビティログレスポンスリスト
     */
    @GetMapping("/api/tasks/{taskId}/activities")
    public ResponseEntity<PageResponse<ActivityLogResponse>> getTaskActivities(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) ActivityEventType eventType
    ) {
        return ResponseEntity.ok(activityQueryService.getTaskActivities(taskId, page, size, eventType));
    }
}
