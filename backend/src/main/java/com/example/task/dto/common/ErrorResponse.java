package com.example.task.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * API の失敗レスポンスを統一形式で表す DTO。
 */
@Getter
@Builder
@AllArgsConstructor
public class ErrorResponse {

    private OffsetDateTime timestamp;
    private int status;
    private String errorCode;
    private String message;
    private List<ErrorDetail> details;
    private String path;
    private String requestId;
}
