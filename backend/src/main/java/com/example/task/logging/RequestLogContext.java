package com.example.task.logging;

import com.example.task.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * リクエストスコープの相関情報やログ制御用フラグを管理する。
 */
@Component
public class RequestLogContext {

    public static final String REQUEST_START_NANOS_ATTRIBUTE =
            RequestLogContext.class.getName() + ".requestStartNanos";
    public static final String SKIP_SYSTEM_4XX_LOG_ATTRIBUTE =
            RequestLogContext.class.getName() + ".skipSystem4xxLog";

    /**
     * 現在のスレッドに紐づくHTTPリクエストを取得する。
     *
     * @return 現在のHTTPリクエスト。リクエストコンテキスト外の場合はnull
     */
    public HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = getServletRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * 現在のスレッドに紐づくHTTPレスポンスを取得する。
     *
     * @return 現在のHTTPレスポンス。リクエストコンテキスト外の場合はnull
     */
    public HttpServletResponse getCurrentResponse() {
        ServletRequestAttributes attributes = getServletRequestAttributes();
        return attributes != null ? attributes.getResponse() : null;
    }

    /**
     * リクエスト処理の開始時刻を記録する。
     *
     * @param request HTTPリクエスト
     */
    public void markRequestStarted(HttpServletRequest request) {
        request.setAttribute(REQUEST_START_NANOS_ATTRIBUTE, System.nanoTime());
    }

    /**
     * 記録済みの開始時刻から現在までの処理時間をミリ秒で取得する。
     *
     * @param request HTTPリクエスト
     * @return 処理時間。開始時刻が記録されていない場合はnull
     */
    public Long getDurationMs(HttpServletRequest request) {
        Object rawValue = request.getAttribute(REQUEST_START_NANOS_ATTRIBUTE);
        if (!(rawValue instanceof Long startedAtNanos)) {
            return null;
        }

        return (System.nanoTime() - startedAtNanos) / 1_000_000;
    }

    /**
     * 現在のリクエストでシステム4xxログの出力を抑制する。
     */
    public void suppressSystem4xxLog() {
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            suppressSystem4xxLog(request);
        }
    }

    /**
     * 指定されたリクエストでシステム4xxログの出力を抑制する。
     *
     * @param request HTTPリクエスト
     */
    public void suppressSystem4xxLog(HttpServletRequest request) {
        request.setAttribute(SKIP_SYSTEM_4XX_LOG_ATTRIBUTE, Boolean.TRUE);
    }

    /**
     * システム4xxログの出力を抑制するかどうかを判定する。
     *
     * @param request HTTPリクエスト
     * @return 抑制する場合はtrue
     */
    public boolean shouldSkipSystem4xxLog(HttpServletRequest request) {
        return Boolean.TRUE.equals(request.getAttribute(SKIP_SYSTEM_4XX_LOG_ATTRIBUTE));
    }

    /**
     * リクエスト属性からリクエストIDを取得する。
     *
     * @param request HTTPリクエスト
     * @return リクエストID。未設定または空文字の場合はnull
     */
    public String getRequestId(HttpServletRequest request) {
        Object requestId = request.getAttribute("requestId");
        return requestId instanceof String value && StringUtils.hasText(value) ? value : null;
    }

    /**
     * SecurityContextから認証済みユーザーIDを取得する。
     *
     * @return 認証済みユーザーID。未認証、匿名認証、または想定外のprincipalの場合はnull
     */
    public Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null ||
                !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken ||
                !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return null;
        }

        return userDetails.getId();
    }

    /**
     * クライアントIPアドレスを取得する。
     *
     * @param request HTTPリクエスト
     * @return クライアントIPアドレス
     */
    public String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            // プロキシ経由では先頭が元のクライアントIPとして扱われる。
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * アクセスログの出力を抑制するかどうかを判定する。
     *
     * @param request HTTPリクエスト
     * @param response HTTPレスポンス
     * @return 正常応答かつログ対象外パスの場合はtrue
     */
    public boolean shouldSkipAccessLog(HttpServletRequest request, HttpServletResponse response) {
        int status = response.getStatus();
        if (status < 200 || status >= 300) {
            return false;
        }

        String path = request.getRequestURI();
        // ヘルスチェックと認証テスト用APIは成功時のアクセスログを抑制する。
        return "/actuator/health".equals(path) || path.startsWith("/api/auth-test/");
    }

    /**
     * リクエストIDをMDCへ設定する。
     *
     * @param requestId リクエストID
     */
    public void putRequestIdIntoMdc(String requestId) {
        if (StringUtils.hasText(requestId)) {
            MDC.put("requestId", requestId);
        }
    }

    /**
     * MDCからリクエストIDを削除する。
     */
    public void clearMdc() {
        MDC.remove("requestId");
    }

    /**
     * 現在のリクエスト属性をServlet向けに取得する。
     *
     * @return Servletリクエスト属性。リクエストコンテキスト外の場合はnull
     */
    private ServletRequestAttributes getServletRequestAttributes() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes servletRequestAttributes
                ? servletRequestAttributes
                : null;
    }
}
