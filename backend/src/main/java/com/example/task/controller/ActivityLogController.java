package com.example.task.controller;

import com.example.task.dto.ActivityLogResponse;
import com.example.task.dto.PageResponse;
import com.example.task.entity.ActivityEventType;
import com.example.task.service.ActivityQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * タスク別履歴一覧取得 API。
 */
@RestController
public class ActivityLogController {

    private final ActivityQueryService activityQueryService;

    public ActivityLogController(ActivityQueryService activityQueryService) {
        this.activityQueryService = activityQueryService;
    }

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
