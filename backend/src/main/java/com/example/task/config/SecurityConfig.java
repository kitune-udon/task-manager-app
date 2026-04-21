package com.example.task.config;

import com.example.task.filter.RequestIdFilter;
import com.example.task.security.CustomAccessDeniedHandler;
import com.example.task.security.CustomAuthenticationEntryPoint;
import com.example.task.security.JwtAuthenticationFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 認証方式、公開エンドポイント、フィルタ順序を定義するセキュリティ設定。
 */
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final RequestIdFilter requestIdFilter;

    /**
     * セキュリティ設定を生成する。
     *
     * @param jwtAuthenticationFilter JWT認証フィルター
     * @param authenticationEntryPoint 未認証時のエントリーポイント
     * @param accessDeniedHandler 権限不足時のハンドラー
     * @param requestIdFilter リクエストIDフィルター
     */
    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CustomAuthenticationEntryPoint authenticationEntryPoint,
            CustomAccessDeniedHandler accessDeniedHandler,
            RequestIdFilter requestIdFilter
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.requestIdFilter = requestIdFilter;
    }

    /**
     * JWT ベースのステートレス認証を有効化し、保護対象 API を定義する。
     *
     * @param http HTTPセキュリティ設定
     * @return 構築済みのセキュリティフィルターチェーン
     * @throws Exception セキュリティ設定の構築に失敗した場合
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // JWTで認証するため、CSRFトークンとHTTPセッションは利用しない。
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        // ブラウザのプリフライト、認証API、ヘルスチェック、エラー応答は認証なしで通す。
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated()
                )
                // リクエストIDは認証失敗レスポンスやログにも載せるため、JWT認証より前に設定する。
                .addFilterBefore(requestIdFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * ユーザー登録時やログイン判定で使うパスワードハッシュ化器。
     *
     * @return BCryptを利用するパスワードエンコーダー
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Spring Security が保持する認証マネージャーを他コンポーネントから参照できるようにする。
     *
     * @param config Spring Securityの認証設定
     * @return 認証マネージャー
     * @throws Exception 認証マネージャーの取得に失敗した場合
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
