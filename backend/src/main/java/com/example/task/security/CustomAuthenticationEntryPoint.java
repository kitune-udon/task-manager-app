package com.example.task.security;

import com.example.task.dto.common.ErrorResponse;
import com.example.task.exception.ErrorCode;
import com.example.task.logging.StructuredLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;

/**
 * 未認証アクセス時のエラーレスポンスを API 仕様の JSON 形式で返す。
 */
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;
    private final StructuredLogService structuredLogService;

    public CustomAuthenticationEntryPoint(
            ObjectMapper objectMapper,
            StructuredLogService structuredLogService
    ) {
        this.objectMapper = objectMapper;
        this.structuredLogService = structuredLogService;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        String errorCode = (String) request.getAttribute("authErrorCode");
        if (errorCode == null) {
            errorCode = ErrorCode.AUTH_001.getCode();
        }

        String message = ErrorCode.AUTH_001.getDefaultMessage();
        if (ErrorCode.AUTH_003.getCode().equals(errorCode)) {
            message = ErrorCode.AUTH_003.getDefaultMessage();
        } else if (ErrorCode.AUTH_004.getCode().equals(errorCode)) {
            message = ErrorCode.AUTH_004.getDefaultMessage();
        }

        if (ErrorCode.AUTH_001.getCode().equals(errorCode)) {
            var fields = structuredLogService.requestFields(
                    request,
                    HttpStatus.UNAUTHORIZED.value(),
                    false,
                    true,
                    false
            );
            fields.put("errorCode", errorCode);
            structuredLogService.warnSecurity("LOG-AUTH-006", "未認証アクセス", fields);
        }

        // 認証失敗理由を統一フォーマットに詰め替えて返却する。
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(OffsetDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .errorCode(errorCode)
                .message(message)
                .details(null)
                .path(request.getRequestURI())
                .requestId((String) request.getAttribute("requestId"))
                .build();

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
