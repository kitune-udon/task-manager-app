package com.example.task.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * API 全体で共通利用するエラーコードと HTTP ステータスの対応表。
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {
    AUTH_001("ERR-AUTH-001", HttpStatus.UNAUTHORIZED, "認証が必要です"),
    AUTH_002("ERR-AUTH-002", HttpStatus.UNAUTHORIZED, "認証に失敗しました"),
    AUTH_003("ERR-AUTH-003", HttpStatus.UNAUTHORIZED, "トークンが不正です"),
    AUTH_004("ERR-AUTH-004", HttpStatus.UNAUTHORIZED, "セッションの有効期限が切れています"),
    AUTH_005("ERR-AUTH-005", HttpStatus.FORBIDDEN, "操作権限がありません"),

    VAL_INPUT_001("ERR-INPUT-001", HttpStatus.BAD_REQUEST, "入力内容に誤りがあります"),
    VAL_TASK_001("ERR-TASK-001", HttpStatus.BAD_REQUEST, "入力内容に誤りがあります"),
    VAL_TASK_002("ERR-TASK-002", HttpStatus.BAD_REQUEST, "入力内容に誤りがあります"),
    VAL_TASK_003("ERR-TASK-003", HttpStatus.BAD_REQUEST, "入力内容に誤りがあります"),

    USR_001("ERR-USR-001", HttpStatus.CONFLICT, "メールアドレスは既に登録されています"),
    USR_002("ERR-USR-002", HttpStatus.NOT_FOUND, "ユーザーが存在しません"),

    RES_TASK_404("ERR-TASK-004", HttpStatus.NOT_FOUND, "タスクが存在しません"),
    PERM_TASK_403_UPD("ERR-TASK-005", HttpStatus.FORBIDDEN, "タスク更新権限がありません"),
    PERM_TASK_403_DEL("ERR-TASK-006", HttpStatus.FORBIDDEN, "タスク削除権限がありません"),

    SYS_999("ERR-SYS-999", HttpStatus.INTERNAL_SERVER_ERROR, "システムエラーが発生しました。しばらくしてから再度お試しください。"),
    SYS_DB_001("ERR-SYS-001", HttpStatus.INTERNAL_SERVER_ERROR, "データの取得または更新に失敗しました。しばらくしてから再度お試しください。");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
