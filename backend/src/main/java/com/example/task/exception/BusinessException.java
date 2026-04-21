package com.example.task.exception;

import lombok.Getter;

/**
 * 業務ルール違反を API 用のエラーコード付きで表す基底例外。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * エラーコードに定義されたデフォルトメッセージで業務例外を生成する。
     *
     * @param errorCode API応答に利用するエラーコード
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    /**
     * エラーコードと個別メッセージを指定して業務例外を生成する。
     *
     * @param errorCode API応答に利用するエラーコード
     * @param message クライアントに返す例外メッセージ
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
