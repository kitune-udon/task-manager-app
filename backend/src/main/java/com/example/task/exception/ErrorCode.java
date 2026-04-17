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
    COMMENT_001("ERR-COMMENT-001", HttpStatus.BAD_REQUEST, "コメント内容を入力してください"),
    COMMENT_002("ERR-COMMENT-002", HttpStatus.NOT_FOUND, "コメントが存在しません"),
    COMMENT_003("ERR-COMMENT-003", HttpStatus.BAD_REQUEST, "コメント内容が長すぎます"),
    COMMENT_004("ERR-COMMENT-004", HttpStatus.FORBIDDEN, "コメント更新権限がありません"),
    COMMENT_005("ERR-COMMENT-005", HttpStatus.FORBIDDEN, "コメント削除権限がありません"),
    COMMENT_006("ERR-COMMENT-006", HttpStatus.CONFLICT, "他のユーザーによりコメントが更新されました。最新状態を再読み込みしてください。"),

    USR_001("ERR-USR-001", HttpStatus.CONFLICT, "メールアドレスは既に登録されています"),
    USR_002("ERR-USR-002", HttpStatus.NOT_FOUND, "ユーザーが存在しません"),

    RES_TASK_404("ERR-TASK-004", HttpStatus.NOT_FOUND, "タスクが存在しません"),
    PERM_TASK_403_UPD("ERR-TASK-005", HttpStatus.FORBIDDEN, "タスク更新権限がありません"),
    PERM_TASK_403_DEL("ERR-TASK-006", HttpStatus.FORBIDDEN, "タスク削除権限がありません"),
    TASK_007("ERR-TASK-007", HttpStatus.CONFLICT, "他のユーザーによりタスクが更新されました。最新状態を再読み込みしてください。"),

    FILE_001("ERR-FILE-001", HttpStatus.BAD_REQUEST, "ファイルを選択してください"),
    FILE_002("ERR-FILE-002", HttpStatus.NOT_FOUND, "添付ファイルが存在しません"),
    FILE_003("ERR-FILE-003", HttpStatus.FORBIDDEN, "添付アップロード権限がありません"),
    FILE_004("ERR-FILE-004", HttpStatus.FORBIDDEN, "添付削除権限がありません"),
    FILE_005("ERR-FILE-005", HttpStatus.BAD_REQUEST, "ファイルサイズが上限を超えています"),
    FILE_006("ERR-FILE-006", HttpStatus.BAD_REQUEST, "許可されていないファイル形式です"),
    FILE_007("ERR-FILE-007", HttpStatus.BAD_REQUEST, "添付件数が上限を超えています"),
    FILE_008("ERR-FILE-008", HttpStatus.BAD_REQUEST, "添付ファイル合計サイズが上限を超えています"),
    FILE_009("ERR-FILE-009", HttpStatus.INTERNAL_SERVER_ERROR, "ファイルの保存に失敗しました。しばらくしてから再度お試しください。"),
    FILE_010("ERR-FILE-010", HttpStatus.INTERNAL_SERVER_ERROR, "ファイルの取得に失敗しました。しばらくしてから再度お試しください。"),
    FILE_011("ERR-FILE-011", HttpStatus.INTERNAL_SERVER_ERROR, "ファイル後処理に失敗しました。詳細はログを確認してください。"),

    NOTIFY_001("ERR-NOTIFY-001", HttpStatus.NOT_FOUND, "通知が存在しません"),
    NOTIFY_002("ERR-NOTIFY-002", HttpStatus.FORBIDDEN, "通知参照権限がありません"),

    ACTIVITY_001("ERR-ACTIVITY-001", HttpStatus.NOT_FOUND, "履歴が存在しません"),
    ACTIVITY_002("ERR-ACTIVITY-002", HttpStatus.FORBIDDEN, "履歴参照権限がありません"),

    SYS_999("ERR-SYS-999", HttpStatus.INTERNAL_SERVER_ERROR, "システムエラーが発生しました。しばらくしてから再度お試しください。"),
    SYS_DB_001("ERR-SYS-001", HttpStatus.INTERNAL_SERVER_ERROR, "データの取得または更新に失敗しました。しばらくしてから再度お試しください。");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
