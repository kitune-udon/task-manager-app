package com.example.task.exception;

import lombok.Getter;

/**
 * 添付ファイル保存・取得時のストレージ障害。
 */
@Getter
public class StorageException extends RuntimeException {

    private final ErrorCode errorCode;

    public StorageException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public StorageException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
