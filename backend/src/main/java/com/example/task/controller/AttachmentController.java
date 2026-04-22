package com.example.task.controller;

import com.example.task.dto.AttachmentResponse;
import com.example.task.service.AttachmentService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 添付ファイル一覧・アップロード・ダウンロード・削除 API。
 */
@RestController
public class AttachmentController {

    private final AttachmentService attachmentService;

    /**
     * コンストラクタ。
     *
     * @param attachmentService 添付ファイルサービス
     */
    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    /**
     * タスクの添付ファイル一覧を取得します。
     *
     * @param taskId タスクID
     * @return 添付ファイルレスポンスリスト
     */
    @GetMapping("/api/tasks/{taskId}/attachments")
    public ResponseEntity<List<AttachmentResponse>> getAttachments(@PathVariable Long taskId) {
        return ResponseEntity.ok(attachmentService.getAttachments(taskId));
    }

    /**
     * タスクに添付ファイルをアップロードします。
     *
     * @param taskId タスクID
     * @param file アップロードするファイル
     * @return アップロードされた添付ファイルレスポンス
     */
    @PostMapping("/api/tasks/{taskId}/attachments")
    public ResponseEntity<AttachmentResponse> uploadAttachment(
            @PathVariable Long taskId,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        return ResponseEntity.status(201).body(attachmentService.uploadAttachment(taskId, file));
    }

    /**
     * 添付ファイルをダウンロードします。
     *
     * @param attachmentId 添付ファイルID
     * @return ファイルコンテンツとメタデータを含むレスポンス
     */
    @GetMapping("/api/attachments/{attachmentId}/download")
    public ResponseEntity<ByteArrayResource> downloadAttachment(@PathVariable Long attachmentId) {
        AttachmentService.DownloadResult result = attachmentService.downloadAttachment(attachmentId);
        ByteArrayResource resource = new ByteArrayResource(result.content());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(result.fileName()).build().toString())
                .body(resource);
    }

    /**
     * 添付ファイルを削除します。
     *
     * @param attachmentId 添付ファイルID
     * @return ステータスコード 204 No Content
     */
    @DeleteMapping("/api/attachments/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(@PathVariable Long attachmentId) {
        attachmentService.deleteAttachment(attachmentId);
        return ResponseEntity.noContent().build();
    }
}
