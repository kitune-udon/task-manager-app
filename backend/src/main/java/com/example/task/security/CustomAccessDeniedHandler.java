package com.example.task.security;

import com.example.task.dto.common.ErrorResponse;
import com.example.task.exception.ErrorCode;
import com.example.task.logging.StructuredLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;

/**
 * 認証済みだが権限不足の場合の 403 レスポンスを組み立てる。
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final StructuredLogService structuredLogService;

    /**
     * アクセス拒否ハンドラーを生成する。
     *
     * @param objectMapper JSONシリアライザー
     * @param structuredLogService 構造化ログサービス
     */
    public CustomAccessDeniedHandler(
            ObjectMapper objectMapper,
            StructuredLogService structuredLogService
    ) {
        this.objectMapper = objectMapper;
        this.structuredLogService = structuredLogService;
    }

    /**
     * 認証済みユーザーの権限不足を処理し、403のJSONエラーレスポンスを返す。
     *
     * <p>アクセス拒否を業務エラーとして構造化ログに記録し、API共通のエラー形式へ整形する。</p>
     *
     * @param request HTTPリクエスト
     * @param response HTTPレスポンス
     * @param accessDeniedException アクセス拒否例外
     * @throws IOException レスポンス書き込みに失敗した場合
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        var fields = structuredLogService.requestFields(
                request,
                HttpStatus.FORBIDDEN.value(),
                false,
                false,
                true
        );
        fields.put("errorCode", ErrorCode.AUTH_005.getCode());
        fields.put("safeMessage", ErrorCode.AUTH_005.getDefaultMessage());
        structuredLogService.warnApplication("LOG-SYS-002", "業務エラー応答", fields);

        // 権限制御エラーも通常の業務エラーと同じ JSON 形式に揃える。
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(OffsetDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .errorCode(ErrorCode.AUTH_005.getCode())
                .message(ErrorCode.AUTH_005.getDefaultMessage())
                .details(null)
                .path(request.getRequestURI())
                .requestId((String) request.getAttribute("requestId"))
                .build();

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
