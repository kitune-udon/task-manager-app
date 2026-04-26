package com.example.task.exception;

import lombok.Getter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 業務ルール違反を API 用のエラーコード付きで表す基底例外。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String logEventId;
    private final Map<String, Object> logFields;
    private final boolean suppressGeneric4xxLog;

    /**
     * エラーコードに定義されたデフォルトメッセージで業務例外を生成する。
     *
     * @param errorCode API応答に利用するエラーコード
     */
    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.getDefaultMessage(), null, null, false);
    }

    /**
     * エラーコードと個別メッセージを指定して業務例外を生成する。
     *
     * @param errorCode API応答に利用するエラーコード
     * @param message クライアントに返す例外メッセージ
     */
    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode, message, null, null, false);
    }

    /**
     * 専用業務ログを持つ業務例外を生成する。
     *
     * @param errorCode API応答に利用するエラーコード
     * @param logEventId 専用ログイベントID
     * @param logFields 専用ログに追加するフィールド
     */
    public BusinessException(ErrorCode errorCode, String logEventId, Map<String, Object> logFields) {
        this(errorCode, errorCode.getDefaultMessage(), logEventId, logFields, true);
    }

    /**
     * エラーコード、個別メッセージ、専用業務ログを指定して業務例外を生成する。
     *
     * @param errorCode API応答に利用するエラーコード
     * @param message クライアントに返す例外メッセージ
     * @param logEventId 専用ログイベントID
     * @param logFields 専用ログに追加するフィールド
     * @param suppressGeneric4xxLog 汎用4xxログを抑止する場合はtrue
     */
    public BusinessException(
            ErrorCode errorCode,
            String message,
            String logEventId,
            Map<String, Object> logFields,
            boolean suppressGeneric4xxLog
    ) {
        super(message);
        this.errorCode = errorCode;
        this.logEventId = logEventId;
        this.logFields = logFields == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(logFields));
        this.suppressGeneric4xxLog = suppressGeneric4xxLog;
    }
}
