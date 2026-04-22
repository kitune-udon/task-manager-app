package com.example.task.service.storage;

/**
 * ダウンロード応答用のファイル実体。
 */
public record StoredAttachment(byte[] content, String contentType) {
}
