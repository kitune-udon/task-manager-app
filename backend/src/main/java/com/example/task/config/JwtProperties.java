package com.example.task.config;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import jakarta.annotation.PostConstruct;
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
        this.secret = secret == null ? null : secret.trim();
    }

    public long getExpirationMillis() {
        return expirationMillis;
    }

    public void setExpirationMillis(long expirationMillis) {
        this.expirationMillis = expirationMillis;
    }

    @PostConstruct
    void validateSecret() {
        getDecodedSecretBytes();
    }

    public byte[] getDecodedSecretBytes() {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secret);
            Keys.hmacShaKeyFor(keyBytes);
            return keyBytes;
        } catch (DecodingException ex) {
            throw new IllegalStateException(
                    "app.jwt.secret must be a valid Base64 string for JWT signing.",
                    ex
            );
        } catch (WeakKeyException ex) {
            throw new IllegalStateException(
                    "app.jwt.secret must decode to at least 32 bytes for HS256 JWT signing.",
                    ex
            );
        }
    }
}
