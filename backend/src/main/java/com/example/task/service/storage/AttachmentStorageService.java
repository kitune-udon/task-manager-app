package com.example.task.service.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 添付ファイル実体の保存・取得・削除を抽象化する。
 */
public interface AttachmentStorageService {

    void store(String storageKey, MultipartFile file);

    StoredAttachment load(String storageKey);

    void delete(String storageKey);
}
