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

    /**
     * 認証エントリーポイントを生成する。
     *
     * @param objectMapper JSONシリアライザー
     * @param structuredLogService 構造化ログサービス
     */
    public CustomAuthenticationEntryPoint(
            ObjectMapper objectMapper,
            StructuredLogService structuredLogService
    ) {
        this.objectMapper = objectMapper;
        this.structuredLogService = structuredLogService;
    }

    /**
     * 未認証アクセスを処理し、401のJSONエラーレスポンスを返す。
     *
     * <p>JWT検証などの前段処理が設定した認証エラーコードを参照し、API共通のエラー形式へ整形する。</p>
     *
     * @param request HTTPリクエスト
     * @param response HTTPレスポンス
     * @param authException 認証例外
     * @throws IOException レスポンス書き込みに失敗した場合
     */
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        // JwtAuthenticationFilter が詳細な理由を設定していない場合は、汎用の未認証エラーとして扱う。
        String errorCode = (String) request.getAttribute("authErrorCode");
        if (errorCode == null) {
            errorCode = ErrorCode.AUTH_001.getCode();
        }

        // クライアントに返すメッセージは公開してよい定型文に限定する。
        String message = ErrorCode.AUTH_001.getDefaultMessage();
        if (ErrorCode.AUTH_003.getCode().equals(errorCode)) {
            message = ErrorCode.AUTH_003.getDefaultMessage();
        } else if (ErrorCode.AUTH_004.getCode().equals(errorCode)) {
            message = ErrorCode.AUTH_004.getDefaultMessage();
        }

        // 単純な未認証アクセスのみセキュリティイベントとして記録し、期限切れなどの既知エラーは応答に専念する。
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
