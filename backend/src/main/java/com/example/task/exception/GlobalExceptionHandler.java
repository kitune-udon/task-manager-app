package com.example.task.exception;

import com.example.task.dto.common.ErrorDetail;
import com.example.task.dto.common.ErrorResponse;
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
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

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

        return build(
                ErrorCode.VAL_INPUT_001.getHttpStatus(),
                ErrorCode.VAL_INPUT_001.getCode(),
                ErrorCode.VAL_INPUT_001.getDefaultMessage(),
                details,
                request
        );
    }

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
                request
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        ErrorCode code = ex.getErrorCode();
        return build(code.getHttpStatus(), code.getCode(), ex.getMessage(), null, request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            ConflictException ex,
            HttpServletRequest request
    ) {
        ErrorCode code = ex.getErrorCode();
        return build(code.getHttpStatus(), code.getCode(), ex.getMessage(), null, request);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(
            BusinessException ex,
            HttpServletRequest request
    ) {
        ErrorCode code = ex.getErrorCode();
        return build(code.getHttpStatus(), code.getCode(), ex.getMessage(), null, request);
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
                request
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
                request
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        return build(
                ErrorCode.VAL_TASK_003.getHttpStatus(),
                ErrorCode.VAL_TASK_003.getCode(),
                ErrorCode.VAL_INPUT_001.getDefaultMessage(),
                null,
                request
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
                request
        );
    }

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status,
            String errorCode,
            String message,
            List<ErrorDetail> details,
            HttpServletRequest request
    ) {
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
}
