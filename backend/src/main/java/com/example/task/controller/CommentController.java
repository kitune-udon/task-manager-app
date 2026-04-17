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

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/api/tasks/{taskId}/comments")
    public ResponseEntity<PageResponse<CommentResponse>> getComments(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(commentService.getComments(taskId, page, size));
    }

    @PostMapping("/api/tasks/{taskId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable Long taskId,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.createComment(taskId, request));
    }

    @PutMapping("/api/comments/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request
    ) {
        return ResponseEntity.ok(commentService.updateComment(commentId, request));
    }

    @DeleteMapping("/api/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }
}
