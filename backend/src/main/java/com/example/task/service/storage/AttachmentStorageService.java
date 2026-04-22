package com.example.task.service.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 添付ファイル実体の保存・取得・削除を抽象化する。
 */
public interface AttachmentStorageService {

    /**
     * 添付ファイルを指定されたストレージキーで保存する。
     *
     * @param storageKey 保存先を表すストレージキー
     * @param file 保存する添付ファイル
     * @throws com.example.task.exception.StorageException 保存に失敗した場合
     */
    void store(String storageKey, MultipartFile file);

    /**
     * 指定されたストレージキーの添付ファイルを取得する。
     *
     * @param storageKey 取得対象を表すストレージキー
     * @return ファイル内容とコンテントタイプを含む保存済み添付ファイル
     * @throws com.example.task.exception.StorageException 取得に失敗した場合
     */
    StoredAttachment load(String storageKey);

    /**
     * 指定されたストレージキーの添付ファイルを削除する。
     *
     * @param storageKey 削除対象を表すストレージキー
     * @throws com.example.task.exception.StorageException 削除に失敗した場合
     */
    void delete(String storageKey);
}
