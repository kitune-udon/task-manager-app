package com.example.task.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * コメント一覧・投稿・更新で返すレスポンス DTO。
 */
@Getter
@Builder
public class CommentResponse {
    private Long id;
    private Long taskId;
    private String content;
    private TaskUserResponse createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
}
