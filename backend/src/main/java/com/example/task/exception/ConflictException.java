package com.example.task.exception;

/**
 * 一意制約や重複登録のような競合状態を表す例外。
 */
public class ConflictException extends BusinessException {

    public ConflictException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ConflictException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
