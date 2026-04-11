package com.example.task.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.example.task.dto.TaskCreateRequest;
import com.example.task.dto.TaskUpdateRequest;
import com.example.task.dto.common.ErrorDetail;
import com.example.task.dto.common.ErrorResponse;
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

    public GlobalExceptionHandler(
            StructuredLogService structuredLogService,
            RequestLogContext requestLogContext
    ) {
        this.structuredLogService = structuredLogService;
        this.requestLogContext = requestLogContext;
    }

    /**
     * Bean Validation の項目エラーをフィールド単位の詳細付きで返す。
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

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        ErrorCode code = ex.getErrorCode();
        return build(code.getHttpStatus(), code.getCode(), ex.getMessage(), null, request, ex);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            ConflictException ex,
            HttpServletRequest request
    ) {
        ErrorCode code = ex.getErrorCode();
        return build(code.getHttpStatus(), code.getCode(), ex.getMessage(), null, request, ex);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(
            BusinessException ex,
            HttpServletRequest request
    ) {
        ErrorCode code = ex.getErrorCode();
        return build(code.getHttpStatus(), code.getCode(), ex.getMessage(), null, request, ex);
    }

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

    private void logResponse(
            HttpStatus status,
            String errorCode,
            String message,
            List<ErrorDetail> details,
            HttpServletRequest request,
            Exception ex
    ) {
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
            if (ex != null) {
                fields.put("stackTrace", StructuredLogJsonFormatter.stackTrace(ex));
            }
            structuredLogService.errorApplication("LOG-SYS-001", "アプリ例外", fields);
            return;
        }

        if (!status.is4xxClientError()) {
            return;
        }

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

        if (requestLogContext.shouldSkipSystem4xxLog(request)) {
            return;
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
     */
    private ErrorCode resolveValidationErrorCode(Object target, List<ErrorDetail> details) {
        if (target instanceof TaskCreateRequest || target instanceof TaskUpdateRequest) {
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

        return ErrorCode.VAL_INPUT_001;
    }

    /**
     * JSON の型不一致や enum 変換失敗を、画面で扱いやすい入力エラーへ補正する。
     */
    private ValidationErrorResolution resolveNotReadableValidation(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        if (!isTaskRequest(request)) {
            return new ValidationErrorResolution(ErrorCode.VAL_INPUT_001, null);
        }

        Throwable cause = ex.getMostSpecificCause();
        if (cause instanceof InvalidFormatException invalidFormatException) {
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

    private boolean isTaskRequest(HttpServletRequest request) {
        return request.getRequestURI() != null && request.getRequestURI().startsWith("/api/tasks");
    }

    private boolean hasField(List<ErrorDetail> details, String fieldName) {
        return details.stream().anyMatch(detail -> fieldName.equals(detail.getField()));
    }

    private boolean isRegisterValidationFailure(HttpServletRequest request, HttpStatus status) {
        return status == HttpStatus.BAD_REQUEST && "/api/auth/register".equals(request.getRequestURI());
    }

    private ErrorDetail detail(String field, String message) {
        return ErrorDetail.builder()
                .field(field)
                .message(message)
                .build();
    }

    private record ValidationErrorResolution(ErrorCode errorCode, List<ErrorDetail> details) {
    }
}
