package com.example.task.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.example.task.dto.CommentCreateRequest;
import com.example.task.dto.CommentUpdateRequest;
import com.example.task.dto.TaskCreateRequest;
import com.example.task.dto.TaskUpdateRequest;
import com.example.task.dto.common.ErrorDetail;
import com.example.task.dto.common.ErrorResponse;
import com.example.task.dto.team.AddTeamMemberRequest;
import com.example.task.dto.team.CreateTeamRequest;
import com.example.task.dto.team.UpdateTeamMemberRoleRequest;
import com.example.task.logging.LoggingProperties;
import com.example.task.logging.RequestLogContext;
import com.example.task.logging.StructuredLogJsonFormatter;
import com.example.task.logging.StructuredLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

/**
 * アプリケーション内の例外を API 仕様の ErrorResponse へ統一変換する。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final StructuredLogService structuredLogService;
    private final RequestLogContext requestLogContext;
    private final LoggingProperties loggingProperties;

    /**
     * グローバル例外ハンドラーを生成する。
     *
     * @param structuredLogService 構造化ログサービス
     * @param requestLogContext リクエストログコンテキスト
     * @param loggingProperties ログ出力設定
     */
    public GlobalExceptionHandler(
            StructuredLogService structuredLogService,
            RequestLogContext requestLogContext,
            LoggingProperties loggingProperties
    ) {
        this.structuredLogService = structuredLogService;
        this.requestLogContext = requestLogContext;
        this.loggingProperties = loggingProperties;
    }

    /**
     * Bean Validation の項目エラーをフィールド単位の詳細付きで返す。
     *
     * @param ex 入力値検証例外
     * @param request HTTPリクエスト
     * @return API共通形式の入力エラーレスポンス
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<ErrorDetail> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> ErrorDetail.builder()
                        .field(fieldError.getField())
                        .message(fieldError.getDefaultMessage())
                        .build())
                .toList();

        ErrorCode errorCode = resolveValidationErrorCode(ex.getBindingResult().getTarget(), details);

        return build(
                errorCode.getHttpStatus(),
                errorCode.getCode(),
                errorCode.getDefaultMessage(),
                details,
                request,
                ex
        );
    }

    /**
     * ログイン失敗は認証エラーコードへ寄せて返す。
     *
     * @param ex 認証失敗例外
     * @param request HTTPリクエスト
     * @return API共通形式の認証エラーレスポンス
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request
    ) {
        return build(
                ErrorCode.AUTH_002.getHttpStatus(),
                ErrorCode.AUTH_002.getCode(),
                ErrorCode.AUTH_002.getDefaultMessage(),
                null,
                request,
                ex
        );
    }

    /**
     * リソース未検出例外をエラーコードに対応するレスポンスへ変換する。
     *
     * @param ex リソース未検出例外
     * @param request HTTPリクエスト
     * @return API共通形式の未検出エラーレスポンス
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        ErrorCode code = ex.getErrorCode();
        return build(code.getHttpStatus(), code.getCode(), ex.getMessage(), null, request, ex);
    }

    /**
     * 競合例外をエラーコードに対応するレスポンスへ変換する。
     *
     * @param ex 競合例外
     * @param request HTTPリクエスト
     * @return API共通形式の競合エラーレスポンス
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            ConflictException ex,
            HttpServletRequest request
    ) {
        ErrorCode code = ex.getErrorCode();
        return build(code.getHttpStatus(), code.getCode(), ex.getMessage(), null, request, ex);
    }

    /**
     * 業務例外をエラーコードに対応するレスポンスへ変換する。
     *
     * @param ex 業務例外
     * @param request HTTPリクエスト
     * @return API共通形式の業務エラーレスポンス
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(
            BusinessException ex,
            HttpServletRequest request
    ) {
        ErrorCode code = ex.getErrorCode();
        return build(code.getHttpStatus(), code.getCode(), ex.getMessage(), null, request, ex);
    }

    /**
     * 添付ストレージ例外をファイル操作エラーのレスポンスへ変換する。
     *
     * @param ex ストレージ例外
     * @param request HTTPリクエスト
     * @return API共通形式のファイル操作エラーレスポンス
     */
    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ErrorResponse> handleStorage(
            StorageException ex,
            HttpServletRequest request
    ) {
        ErrorCode code = ex.getErrorCode();
        return build(code.getHttpStatus(), code.getCode(), code.getDefaultMessage(), null, request, ex);
    }

    /**
     * Spring Security の権限不足例外を認可エラーレスポンスへ変換する。
     *
     * @param ex アクセス拒否例外
     * @param request HTTPリクエスト
     * @return API共通形式の認可エラーレスポンス
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        return build(
                ErrorCode.AUTH_005.getHttpStatus(),
                ErrorCode.AUTH_005.getCode(),
                ErrorCode.AUTH_005.getDefaultMessage(),
                null,
                request,
                ex
        );
    }

    /**
     * データアクセス例外をDBシステムエラーのレスポンスへ変換する。
     *
     * @param ex データアクセス例外
     * @param request HTTPリクエスト
     * @return API共通形式のDBエラーレスポンス
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccess(
            DataAccessException ex,
            HttpServletRequest request
    ) {
        return build(
                ErrorCode.SYS_DB_001.getHttpStatus(),
                ErrorCode.SYS_DB_001.getCode(),
                ErrorCode.SYS_DB_001.getDefaultMessage(),
                null,
                request,
                ex
        );
    }

    /**
     * JSONとして読めないリクエストボディを入力エラーのレスポンスへ変換する。
     *
     * @param ex リクエストボディ読取例外
     * @param request HTTPリクエスト
     * @return API共通形式の入力エラーレスポンス
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        ValidationErrorResolution resolution = resolveNotReadableValidation(ex, request);

        return build(
                resolution.errorCode().getHttpStatus(),
                resolution.errorCode().getCode(),
                resolution.errorCode().getDefaultMessage(),
                resolution.details(),
                request,
                ex
        );
    }

    /**
     * multipartリクエスト処理中の例外をファイルアップロードエラーのレスポンスへ変換する。
     *
     * @param ex multipart処理例外
     * @param request HTTPリクエスト
     * @return API共通形式のファイルアップロードエラーレスポンス
     */
    @ExceptionHandler({MultipartException.class, MaxUploadSizeExceededException.class})
    public ResponseEntity<ErrorResponse> handleMultipart(
            Exception ex,
            HttpServletRequest request
    ) {
        return build(
                ErrorCode.FILE_005.getHttpStatus(),
                ErrorCode.FILE_005.getCode(),
                ErrorCode.FILE_005.getDefaultMessage(),
                null,
                request,
                ex
        );
    }

    /**
     * 個別に処理されなかった例外を汎用システムエラーのレスポンスへ変換する。
     *
     * @param ex 予期しない例外
     * @param request HTTPリクエスト
     * @return API共通形式のシステムエラーレスポンス
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception ex,
            HttpServletRequest request
    ) {
        return build(
                ErrorCode.SYS_999.getHttpStatus(),
                ErrorCode.SYS_999.getCode(),
                ErrorCode.SYS_999.getDefaultMessage(),
                null,
                request,
                ex
        );
    }

    /**
     * 各例外ハンドラから呼ばれる共通レスポンス生成処理。
     *
     * @param status HTTPステータス
     * @param errorCode エラーコード
     * @param message クライアントに返すメッセージ
     * @param details 入力エラーなどの詳細情報
     * @param request HTTPリクエスト
     * @param ex レスポンス生成元の例外
     * @return API共通形式のエラーレスポンス
     */
    private ResponseEntity<ErrorResponse> build(
            HttpStatus status,
            String errorCode,
            String message,
            List<ErrorDetail> details,
            HttpServletRequest request,
            Exception ex
    ) {
        logResponse(status, errorCode, message, details, request, ex);

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(OffsetDateTime.now())
                .status(status.value())
                .errorCode(errorCode)
                .message(message)
                .details(details)
                .path(request.getRequestURI())
                .requestId((String) request.getAttribute("requestId"))
                .build();
        return ResponseEntity.status(status).body(body);
    }

    /**
     * エラーレスポンスに対応する構造化ログを出力する。
     *
     * @param status HTTPステータス
     * @param errorCode エラーコード
     * @param message クライアントに返すメッセージ
     * @param details 入力エラーなどの詳細情報
     * @param request HTTPリクエスト
     * @param ex レスポンス生成元の例外
     */
    private void logResponse(
            HttpStatus status,
            String errorCode,
            String message,
            List<ErrorDetail> details,
            HttpServletRequest request,
            Exception ex
    ) {
        // 5xxはアプリケーション障害として扱い、設定に応じてスタックトレースも残す。
        if (status.is5xxServerError()) {
            LinkedHashMap<String, Object> fields = structuredLogService.requestFields(
                    request,
                    status.value(),
                    false,
                    false,
                    true
            );
            fields.put("errorCode", errorCode);
            fields.put("safeMessage", message);
            fields.put("exceptionClass", ex != null ? ex.getClass().getName() : Exception.class.getName());
            if (ex != null && loggingProperties.isIncludeStacktrace()) {
                fields.put("stackTrace", StructuredLogJsonFormatter.stackTrace(ex));
            }
            structuredLogService.errorApplication("LOG-SYS-001", "アプリ例外", fields);
            return;
        }

        // 2xx/3xxはこのハンドラーのログ対象外。4xxのみ業務エラーとして扱う。
        if (!status.is4xxClientError()) {
            return;
        }

        // ユーザー登録失敗は認証・セキュリティ系イベントとして別チャンネルへ記録する。
        if (isRegisterValidationFailure(request, status)) {
            LinkedHashMap<String, Object> fields = structuredLogService.requestFields(
                    request,
                    status.value(),
                    false,
                    true,
                    false
            );
            fields.put("errorCode", errorCode);
            fields.put("safeMessage", message);
            if (details != null && !details.isEmpty()) {
                fields.put("details", details);
            }
            requestLogContext.suppressSystem4xxLog(request);
            structuredLogService.warnSecurity("LOG-AUTH-005", "ユーザー登録失敗", fields);
            return;
        }

        // 認証/認可ハンドラーなどで個別にログ済みの4xxは重複出力しない。
        if (requestLogContext.shouldSkipSystem4xxLog(request)) {
            return;
        }

        if (ex instanceof BusinessException businessException) {
            if (businessException.getLogEventId() != null) {
                LinkedHashMap<String, Object> fields = structuredLogService.requestFields(
                        request,
                        status.value(),
                        true,
                        false,
                        false
                );
                fields.put("errorCode", errorCode);
                fields.put("safeMessage", message);
                fields.putAll(businessException.getLogFields());
                structuredLogService.warnApplication(businessException.getLogEventId(), "チーム業務エラー応答", fields);
                return;
            }

            if (businessException.isSuppressGeneric4xxLog()) {
                return;
            }
        }

        LinkedHashMap<String, Object> fields = structuredLogService.requestFields(
                request,
                status.value(),
                false,
                false,
                true
        );
        fields.put("errorCode", errorCode);
        fields.put("safeMessage", message);
        if (details != null && !details.isEmpty()) {
            fields.put("details", details);
        }
        structuredLogService.warnApplication("LOG-SYS-002", "業務エラー応答", fields);
    }

    /**
     * リクエスト DTO とエラー項目から、返すべき業務エラーコードを決める。
     *
     * @param target バリデーション対象DTO
     * @param details フィールド単位の入力エラー詳細
     * @return 入力内容に対応するエラーコード
     */
    private ErrorCode resolveValidationErrorCode(Object target, List<ErrorDetail> details) {
        if (target instanceof TaskCreateRequest || target instanceof TaskUpdateRequest) {
            if (target instanceof TaskCreateRequest && hasField(details, "teamId")) {
                return ErrorCode.TASK_008;
            }
            if (hasField(details, "title")) {
                return ErrorCode.VAL_TASK_001;
            }
            if (hasField(details, "status")) {
                return ErrorCode.VAL_TASK_002;
            }
            if (hasField(details, "dueDate")) {
                return ErrorCode.VAL_TASK_003;
            }
        }

        if (target instanceof CommentCreateRequest || target instanceof CommentUpdateRequest) {
            if (hasField(details, "content")) {
                return containsMessage(details, "1000") ? ErrorCode.COMMENT_003 : ErrorCode.COMMENT_001;
            }
        }

        if (target instanceof CreateTeamRequest) {
            return ErrorCode.TEAM_001;
        }

        if (target instanceof AddTeamMemberRequest || target instanceof UpdateTeamMemberRoleRequest) {
            return ErrorCode.TEAM_MEMBER_001;
        }

        return ErrorCode.VAL_INPUT_001;
    }

    /**
     * JSON の型不一致や enum 変換失敗を、画面で扱いやすい入力エラーへ補正する。
     *
     * @param ex リクエストボディ読取例外
     * @param request HTTPリクエスト
     * @return 入力エラーとして返すエラーコードと詳細情報
     */
    private ValidationErrorResolution resolveNotReadableValidation(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        if (isTeamRequest(request)) {
            Throwable cause = ex.getMostSpecificCause();
            if (cause instanceof InvalidFormatException invalidFormatException) {
                String fieldName = invalidFormatException.getPath()
                        .stream()
                        .map(reference -> reference.getFieldName())
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null);

                if ("role".equals(fieldName)) {
                    return new ValidationErrorResolution(
                            ErrorCode.TEAM_MEMBER_004,
                            List.of(detail("role", "ロールはADMIN、MEMBERのいずれかを指定してください"))
                    );
                }
            }

            return new ValidationErrorResolution(ErrorCode.TEAM_001, null);
        }

        if (!isTaskRequest(request)) {
            return new ValidationErrorResolution(ErrorCode.VAL_INPUT_001, null);
        }

        Throwable cause = ex.getMostSpecificCause();
        if (cause instanceof InvalidFormatException invalidFormatException) {
            // Jacksonの参照パスから、型変換に失敗したフィールド名を取り出す。
            String fieldName = invalidFormatException.getPath()
                    .stream()
                    .map(reference -> reference.getFieldName())
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);

            if ("status".equals(fieldName)) {
                return new ValidationErrorResolution(
                        ErrorCode.VAL_TASK_002,
                        List.of(detail("status", "ステータスはTODO、DOING、DONEのいずれかを指定してください"))
                );
            }

            if ("dueDate".equals(fieldName)) {
                return new ValidationErrorResolution(
                        ErrorCode.VAL_TASK_003,
                        List.of(detail("dueDate", "期限はyyyy-MM-dd形式で入力してください"))
                );
            }

            if ("priority".equals(fieldName)) {
                return new ValidationErrorResolution(
                        ErrorCode.VAL_INPUT_001,
                        List.of(detail("priority", "優先度はLOW、MEDIUM、HIGHのいずれかを指定してください"))
                );
            }
        }

        return new ValidationErrorResolution(ErrorCode.VAL_INPUT_001, null);
    }

    /**
     * リクエストがタスクAPI向けかどうかを判定する。
     *
     * @param request HTTPリクエスト
     * @return タスクAPI向けの場合はtrue
     */
    private boolean isTaskRequest(HttpServletRequest request) {
        return request.getRequestURI() != null && request.getRequestURI().startsWith("/api/tasks");
    }

    /**
     * リクエストがチームAPI向けかどうかを判定する。
     *
     * @param request HTTPリクエスト
     * @return チームAPI向けの場合はtrue
     */
    private boolean isTeamRequest(HttpServletRequest request) {
        return request.getRequestURI() != null && request.getRequestURI().startsWith("/api/teams");
    }

    /**
     * 入力エラー詳細に指定フィールドのエラーが含まれるかを判定する。
     *
     * @param details 入力エラー詳細
     * @param fieldName フィールド名
     * @return 指定フィールドのエラーが含まれる場合はtrue
     */
    private boolean hasField(List<ErrorDetail> details, String fieldName) {
        return details.stream().anyMatch(detail -> fieldName.equals(detail.getField()));
    }

    /**
     * 入力エラー詳細のメッセージに指定文字列が含まれるかを判定する。
     *
     * @param details 入力エラー詳細
     * @param token 検索する文字列
     * @return 指定文字列が含まれる場合はtrue
     */
    private boolean containsMessage(List<ErrorDetail> details, String token) {
        return details.stream()
                .map(ErrorDetail::getMessage)
                .filter(Objects::nonNull)
                .anyMatch(message -> message.contains(token));
    }

    /**
     * ユーザー登録APIの入力エラーかどうかを判定する。
     *
     * @param request HTTPリクエスト
     * @param status HTTPステータス
     * @return ユーザー登録APIの400エラーの場合はtrue
     */
    private boolean isRegisterValidationFailure(HttpServletRequest request, HttpStatus status) {
        return status == HttpStatus.BAD_REQUEST && "/api/auth/register".equals(request.getRequestURI());
    }

    /**
     * フィールド単位の入力エラー詳細を生成する。
     *
     * @param field フィールド名
     * @param message エラーメッセージ
     * @return 入力エラー詳細
     */
    private ErrorDetail detail(String field, String message) {
        return ErrorDetail.builder()
                .field(field)
                .message(message)
                .build();
    }

    /**
     * JSON読取エラーを入力エラーへ補正した結果。
     *
     * @param errorCode 返却するエラーコード
     * @param details フィールド単位の入力エラー詳細
     */
    private record ValidationErrorResolution(ErrorCode errorCode, List<ErrorDetail> details) {
    }
}
