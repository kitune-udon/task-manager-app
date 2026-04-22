package com.example.task.dto;

import com.example.task.entity.AttachmentStorageType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 添付ファイル一覧・アップロードで返すレスポンス DTO。
 */
@Getter
@Builder
public class AttachmentResponse {
    private Long id;
    private Long taskId;
    private String originalFileName;
    private String contentType;
    private Long fileSize;
    private AttachmentStorageType storageType;
    private TaskUserResponse uploadedBy;
    private LocalDateTime createdAt;
}
