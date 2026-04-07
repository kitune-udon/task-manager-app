package com.example.task.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    AUTH_001("AUTH-001", HttpStatus.UNAUTHORIZED, "認証が必要です"),
    AUTH_002("AUTH-002", HttpStatus.UNAUTHORIZED, "認証に失敗しました"),
    AUTH_003("AUTH-003", HttpStatus.UNAUTHORIZED, "トークンが不正です"),
    AUTH_004("AUTH-004", HttpStatus.UNAUTHORIZED, "セッションの有効期限が切れています"),
    AUTH_005("AUTH-005", HttpStatus.FORBIDDEN, "操作権限がありません"),

    VAL_INPUT_001("VAL-INPUT-001", HttpStatus.BAD_REQUEST, "入力内容に誤りがあります"),
    VAL_TASK_003("VAL-TASK-003", HttpStatus.BAD_REQUEST, "期限の形式が不正です"),

    USR_001("USR-001", HttpStatus.CONFLICT, "メールアドレスは既に登録されています"),
    USR_002("USR-002", HttpStatus.NOT_FOUND, "ユーザーが存在しません"),

    RES_TASK_404("RES-TASK-404", HttpStatus.NOT_FOUND, "タスクが存在しません"),

    SYS_999("SYS-999", HttpStatus.INTERNAL_SERVER_ERROR, "システムエラーが発生しました。しばらくしてから再度お試しください。"),
    SYS_DB_001("SYS-DB-001", HttpStatus.INTERNAL_SERVER_ERROR, "データの取得または更新に失敗しました。しばらくしてから再度お試しください。");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
