package com.example.task.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * フロントエンドからのクロスオリジンアクセス設定をまとめる。
 */
@Configuration
public class CorsConfig {

    private final CorsProperties corsProperties;

    /**
     * CORS設定を生成する。
     *
     * @param corsProperties CORS設定プロパティ
     */
    public CorsConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    /**
     * 設定ファイルの許可オリジンを読み取り、API 全体に CORS を適用する。
     *
     * @return CORSマッピングを登録するWeb MVC設定
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        String[] allowedOrigins = corsProperties.getAllowedOrigins().stream()
                // 設定値の余分な空白や空要素を除外し、Springへ渡す許可オリジンだけを残す。
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toArray(String[]::new);

        return new WebMvcConfigurer() {
            /**
             * すべてのパスに対して、フロントエンドから利用するHTTPメソッドとヘッダーを許可する。
             *
             * @param registry CORSマッピング登録先
             */
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(allowedOrigins)
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        // CookieやAuthorizationヘッダーを含むリクエストを受け付ける。
                        .allowCredentials(true);
            }
        };
    }
}
