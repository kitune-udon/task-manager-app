package com.example.task.security;

import com.example.task.exception.ErrorCode;
import com.example.task.logging.StructuredLogService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authorization ヘッダーの JWT を検証し、認証済みユーザーを SecurityContext に設定する。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final StructuredLogService structuredLogService;

    /**
     * JWT認証フィルターを生成する。
     *
     * @param jwtUtil JWTユーティリティ
     * @param userDetailsService ユーザー詳細サービス
     * @param authenticationEntryPoint 認証エントリーポイント
     * @param structuredLogService 構造化ログサービス
     */
    public JwtAuthenticationFilter(JwtUtil jwtUtil,
                                   CustomUserDetailsService userDetailsService,
                                   CustomAuthenticationEntryPoint authenticationEntryPoint,
                                   StructuredLogService structuredLogService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.structuredLogService = structuredLogService;
    }

    /**
     * AuthorizationヘッダーのBearerトークンを検証し、認証済みユーザーをSecurityContextに設定する。
     *
     * <p>トークンがない場合は認証状態を変更せず後続フィルターへ委譲し、JWT検証に失敗した場合は
     * 認証エントリーポイントへ委譲して共通の401レスポンスを返す。</p>
     *
     * @param request HTTPリクエスト
     * @param response HTTPレスポンス
     * @param filterChain フィルターチェーン
     * @throws ServletException 後続フィルターの処理に失敗した場合
     * @throws IOException 後続フィルターまたはレスポンス書き込みに失敗した場合
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        // Bearer トークンがないリクエストは未認証のまま後続へ流す。
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);
        String username;

        try {
            username = jwtUtil.extractUsername(jwt);
        } catch (ExpiredJwtException ex) {
            // 期限切れは専用エラーコードを付与して認証エントリポイントへ委譲する。
            request.setAttribute("authErrorCode", ErrorCode.AUTH_004.getCode());
            logJwtValidationFailure(request, ErrorCode.AUTH_004);
            authenticationEntryPoint.commence(request, response,
                    new InsufficientAuthenticationException("Token expired", ex));
            return;
        } catch (JwtException | IllegalArgumentException ex) {
            // 署名不正や形式不備も同じく統一レスポンスで返す。
            request.setAttribute("authErrorCode", ErrorCode.AUTH_003.getCode());
            logJwtValidationFailure(request, ErrorCode.AUTH_003);
            authenticationEntryPoint.commence(request, response,
                    new InsufficientAuthenticationException("Invalid token", ex));
            return;
        }

        // すでに認証済みの場合は既存のSecurityContextを優先する。
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            CustomUserDetails userDetails =
                    (CustomUserDetails) userDetailsService.loadUserByUsername(username);

            if (jwtUtil.isTokenValid(jwt, userDetails)) {
                // トークンが有効な場合だけ認証済みユーザーとしてコンテキストに登録する。
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * JWT検証失敗をセキュリティログとして記録する。
     *
     * @param request HTTPリクエスト
     * @param errorCode 検証失敗のエラーコード
     */
    private void logJwtValidationFailure(HttpServletRequest request, ErrorCode errorCode) {
        var fields = structuredLogService.requestFields(
                request,
                errorCode.getHttpStatus().value(),
                false,
                true,
                false
        );
        fields.put("errorCode", errorCode.getCode());
        fields.put("safeMessage", errorCode.getDefaultMessage());
        structuredLogService.warnSecurity("LOG-AUTH-003", "JWT検証失敗", fields);
    }
}
