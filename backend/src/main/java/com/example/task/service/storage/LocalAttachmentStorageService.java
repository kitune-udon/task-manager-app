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

    public LocalAttachmentStorageService(StorageProperties properties) {
        this.basePath = Path.of(properties.getLocalBasePath());
    }

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

    @Override
    public void delete(String storageKey) {
        Path target = resolvePath(storageKey);

        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            throw new StorageException(ErrorCode.FILE_011, "Failed to delete local file", ex);
        }
    }

    private Path resolvePath(String storageKey) {
        return basePath.resolve(storageKey);
    }
}
