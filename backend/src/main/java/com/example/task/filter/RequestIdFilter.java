package com.example.task.filter;

import com.example.task.logging.RequestLogContext;
import com.example.task.logging.StructuredLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * リクエスト単位の識別子を採番し、ログやレスポンスで追跡しやすくするフィルタ。
 */
@Component
public class RequestIdFilter extends OncePerRequestFilter {

    private final StructuredLogService structuredLogService;
    private final RequestLogContext requestLogContext;

    /**
     * リクエストIDフィルターを生成する。
     *
     * @param structuredLogService 構造化ログサービス
     * @param requestLogContext リクエストログコンテキスト
     */
    public RequestIdFilter(StructuredLogService structuredLogService, RequestLogContext requestLogContext) {
        this.structuredLogService = structuredLogService;
        this.requestLogContext = requestLogContext;
    }

    /**
     * リクエストIDを設定し、後続処理の完了後にアクセスログを出力する。
     *
     * @param request HTTPリクエスト
     * @param response HTTPレスポンス
     * @param filterChain フィルターチェーン
     * @throws ServletException 後続フィルターの処理に失敗した場合
     * @throws IOException 後続フィルターまたはレスポンス書き込みに失敗した場合
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // クライアントが指定した ID を優先し、なければサーバー側で採番する。
        String requestId = Optional.ofNullable(request.getHeader("X-Request-Id"))
                .filter(StringUtils::hasText)
                .orElse(UUID.randomUUID().toString());

        // 以降の処理とレスポンスの両方から同じ requestId を参照できるようにする。
        request.setAttribute("requestId", requestId);
        response.setHeader("X-Request-Id", requestId);
        requestLogContext.markRequestStarted(request);
        requestLogContext.putRequestIdIntoMdc(requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // 例外発生時もアクセスログ出力とMDCの後始末を必ず行う。
            emitAccessLogIfNeeded(request, response);
            requestLogContext.clearMdc();
        }
    }

    /**
     * ログ対象のリクエストであればアクセスログを出力する。
     *
     * @param request HTTPリクエスト
     * @param response HTTPレスポンス
     */
    private void emitAccessLogIfNeeded(HttpServletRequest request, HttpServletResponse response) {
        if (requestLogContext.shouldSkipAccessLog(request, response)) {
            return;
        }

        structuredLogService.infoApplication(
                "LOG-REQ-001",
                "リクエスト終了",
                structuredLogService.requestFields(
                        request,
                        response.getStatus(),
                        response.getStatus() != HttpStatus.UNAUTHORIZED.value(),
                        true,
                        true
                )
        );
    }
}
