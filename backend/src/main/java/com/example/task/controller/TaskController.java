package com.example.task.controller;

import com.example.task.dto.TaskCreateRequest;
import com.example.task.dto.TaskResponse;
import com.example.task.dto.TaskSummaryResponse;
import com.example.task.dto.TaskUpdateRequest;
import com.example.task.entity.Priority;
import com.example.task.entity.TaskStatus;
import com.example.task.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * タスクの CRUD と一覧検索を公開する API。
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    /**
     * タスクAPIコントローラーを生成する。
     *
     * @param taskService タスクサービス
     */
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * 入力値を検証したうえで新規タスクを作成する。
     *
     * @param request タスク作成リクエスト
     * @return 作成されたタスク。作成成功時はHTTP 201を返す
     */
    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody TaskCreateRequest request) {
        TaskResponse response = taskService.createTask(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * ログインユーザーが参照できるタスクだけを条件付きで取得する。
     *
     * @param status 絞り込み対象のステータス（任意）
     * @param priority 絞り込み対象の優先度（任意）
     * @param assignedUserId 絞り込み対象の担当者ID（任意）
     * @param keyword タイトルや説明に対する検索キーワード（任意）
     * @return 条件に一致するタスク概要リスト
     */
    @GetMapping
    public ResponseEntity<List<TaskSummaryResponse>> getTasks(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Long assignedUserId,
            @RequestParam(required = false) String keyword
    ) {
        List<TaskSummaryResponse> response = taskService.getTasks(status, priority, assignedUserId, keyword);
        return ResponseEntity.ok(response);
    }

    /**
     * 単一タスクの詳細を返す。
     *
     * @param taskId タスクID
     * @return タスク詳細
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponse> getTask(@PathVariable Long taskId) {
        TaskResponse response = taskService.getTask(taskId);
        return ResponseEntity.ok(response);
    }

    /**
     * 権限を確認したうえで既存タスクを更新する。
     *
     * @param taskId 更新対象のタスクID
     * @param request タスク更新リクエスト
     * @return 更新後のタスク
     */
    @PutMapping("/{taskId}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskUpdateRequest request
    ) {
        TaskResponse response = taskService.updateTask(taskId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 作成者のみ削除できるルールでタスクを削除する。
     *
     * @param taskId 削除対象のタスクID
     * @return レスポンス本文なし。削除成功時はHTTP 204を返す
     */
    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long taskId) {
        taskService.deleteTask(taskId);
        return ResponseEntity.noContent().build();
    }
}
