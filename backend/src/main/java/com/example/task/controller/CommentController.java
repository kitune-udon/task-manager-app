package com.example.task.controller;

import com.example.task.dto.CommentCreateRequest;
import com.example.task.dto.CommentResponse;
import com.example.task.dto.CommentUpdateRequest;
import com.example.task.dto.PageResponse;
import com.example.task.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * コメント一覧・投稿・更新・削除 API。
 */
@RestController
public class CommentController {

    private final CommentService commentService;

    /**
     * コンストラクタ。
     *
     * @param commentService コメントサービス
     */
    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * タスクのコメント一覧を取得します。ページネーション対応。
     *
     * @param taskId タスクID
     * @param page ページ番号（デフォルト: 0）
     * @param size 1ページあたりの件数（デフォルト: 20）
     * @return ページネーション付きのコメントレスポンスリスト
     */
    @GetMapping("/api/tasks/{taskId}/comments")
    public ResponseEntity<PageResponse<CommentResponse>> getComments(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(commentService.getComments(taskId, page, size));
    }

    /**
     * コメントを作成します。
     *
     * @param taskId タスクID
     * @param request コメント作成リクエスト
     * @return 作成されたコメントレスポンス
     */
    @PostMapping("/api/tasks/{taskId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable Long taskId,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.createComment(taskId, request));
    }

    /**
     * コメントを更新します。
     *
     * @param commentId コメントID
     * @param request コメント更新リクエスト
     * @return 更新されたコメントレスポンス
     */
    @PutMapping("/api/comments/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request
    ) {
        return ResponseEntity.ok(commentService.updateComment(commentId, request));
    }

    /**
     * コメントを削除します。
     *
     * @param commentId コメントID
     * @return ステータスコード 204 No Content
     */
    @DeleteMapping("/api/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }
}
