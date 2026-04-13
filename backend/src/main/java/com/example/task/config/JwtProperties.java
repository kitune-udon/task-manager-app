package com.example.task.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT の署名鍵と有効期限を設定ファイルから受け取る。
 */
@ConfigurationProperties(prefix = "app.jwt")
@Validated
public class JwtProperties {

    @NotBlank
    private String secret;

    @Positive
    private long expirationMillis;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpirationMillis() {
        return expirationMillis;
    }

    public void setExpirationMillis(long expirationMillis) {
        this.expirationMillis = expirationMillis;
    }
}
