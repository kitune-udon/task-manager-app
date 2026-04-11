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

    public HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = getServletRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    public HttpServletResponse getCurrentResponse() {
        ServletRequestAttributes attributes = getServletRequestAttributes();
        return attributes != null ? attributes.getResponse() : null;
    }

    public void markRequestStarted(HttpServletRequest request) {
        request.setAttribute(REQUEST_START_NANOS_ATTRIBUTE, System.nanoTime());
    }

    public Long getDurationMs(HttpServletRequest request) {
        Object rawValue = request.getAttribute(REQUEST_START_NANOS_ATTRIBUTE);
        if (!(rawValue instanceof Long startedAtNanos)) {
            return null;
        }

        return (System.nanoTime() - startedAtNanos) / 1_000_000;
    }

    public void suppressSystem4xxLog() {
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            suppressSystem4xxLog(request);
        }
    }

    public void suppressSystem4xxLog(HttpServletRequest request) {
        request.setAttribute(SKIP_SYSTEM_4XX_LOG_ATTRIBUTE, Boolean.TRUE);
    }

    public boolean shouldSkipSystem4xxLog(HttpServletRequest request) {
        return Boolean.TRUE.equals(request.getAttribute(SKIP_SYSTEM_4XX_LOG_ATTRIBUTE));
    }

    public String getRequestId(HttpServletRequest request) {
        Object requestId = request.getAttribute("requestId");
        return requestId instanceof String value && StringUtils.hasText(value) ? value : null;
    }

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

    public String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public boolean shouldSkipAccessLog(HttpServletRequest request, HttpServletResponse response) {
        int status = response.getStatus();
        if (status < 200 || status >= 300) {
            return false;
        }

        String path = request.getRequestURI();
        return "/actuator/health".equals(path) || path.startsWith("/api/auth-test/");
    }

    public void putRequestIdIntoMdc(String requestId) {
        if (StringUtils.hasText(requestId)) {
            MDC.put("requestId", requestId);
        }
    }

    public void clearMdc() {
        MDC.remove("requestId");
    }

    private ServletRequestAttributes getServletRequestAttributes() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes servletRequestAttributes
                ? servletRequestAttributes
                : null;
    }
}
