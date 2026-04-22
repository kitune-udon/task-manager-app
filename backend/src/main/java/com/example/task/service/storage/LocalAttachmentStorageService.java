package com.example.task.service.storage;

import com.example.task.config.StorageProperties;
import com.example.task.exception.ErrorCode;
import com.example.task.exception.StorageException;
import org.springframework.http.MediaType;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * ローカルファイルシステムを利用する添付保存実装。
 */
public class LocalAttachmentStorageService implements AttachmentStorageService {

    private final Path basePath;

    /**
     * ローカル添付ストレージを生成する。
     *
     * @param properties ストレージ設定
     */
    public LocalAttachmentStorageService(StorageProperties properties) {
        this.basePath = Path.of(properties.getLocalBasePath());
    }

    /**
     * 添付ファイルをローカルファイルシステムへ保存する。
     *
     * <p>保存先の親ディレクトリが存在しない場合は作成する。</p>
     *
     * @param storageKey 保存先を表すストレージキー
     * @param file 保存する添付ファイル
     * @throws StorageException 保存に失敗した場合
     */
    @Override
    public void store(String storageKey, MultipartFile file) {
        Path target = resolvePath(storageKey);

        try {
            Files.createDirectories(target.getParent());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target);
            }
        } catch (IOException ex) {
            throw new StorageException(ErrorCode.FILE_009, "Failed to store file locally", ex);
        }
    }

    /**
     * ローカルファイルシステムから添付ファイルを読み込む。
     *
     * <p>コンテントタイプを判定できない場合は {@code application/octet-stream} を返す。</p>
     *
     * @param storageKey 読み込み対象を表すストレージキー
     * @return ファイル内容とコンテントタイプを含む保存済み添付ファイル
     * @throws StorageException 読み込みに失敗した場合
     */
    @Override
    public StoredAttachment load(String storageKey) {
        Path target = resolvePath(storageKey);

        try {
            byte[] content = FileCopyUtils.copyToByteArray(target.toFile());
            String contentType = Files.probeContentType(target);
            return new StoredAttachment(content, contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE);
        } catch (IOException ex) {
            throw new StorageException(ErrorCode.FILE_010, "Failed to load file locally", ex);
        }
    }

    /**
     * ローカルファイルシステムから添付ファイルを削除する。
     *
     * <p>対象ファイルが存在しない場合は何もしない。</p>
     *
     * @param storageKey 削除対象を表すストレージキー
     * @throws StorageException 削除に失敗した場合
     */
    @Override
    public void delete(String storageKey) {
        Path target = resolvePath(storageKey);

        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            throw new StorageException(ErrorCode.FILE_011, "Failed to delete local file", ex);
        }
    }

    /**
     * ストレージキーからローカルファイルパスを解決する。
     *
     * @param storageKey ストレージキー
     * @return ベースパス配下のファイルパス
     */
    private Path resolvePath(String storageKey) {
        return basePath.resolve(storageKey);
    }
}
