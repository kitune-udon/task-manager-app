package com.example.task.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

        filterChain.doFilter(request, response);
    }
}
