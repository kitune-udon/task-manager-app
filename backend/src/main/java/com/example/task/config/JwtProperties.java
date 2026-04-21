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

    /**
     * JWT署名に使うBase64エンコード済み秘密鍵を取得する。
     *
     * @return Base64エンコード済み秘密鍵
     */
    public String getSecret() {
        return secret;
    }

    /**
     * JWT署名に使うBase64エンコード済み秘密鍵を設定する。
     *
     * @param secret Base64エンコード済み秘密鍵
     */
    public void setSecret(String secret) {
        this.secret = secret == null ? null : secret.trim();
    }

    /**
     * JWTの有効期限をミリ秒で取得する。
     *
     * @return JWT有効期限（ミリ秒）
     */
    public long getExpirationMillis() {
        return expirationMillis;
    }

    /**
     * JWTの有効期限をミリ秒で設定する。
     *
     * @param expirationMillis JWT有効期限（ミリ秒）
     */
    public void setExpirationMillis(long expirationMillis) {
        this.expirationMillis = expirationMillis;
    }

    /**
     * 起動時にJWT秘密鍵が署名に利用できる形式か検証する。
     */
    @PostConstruct
    void validateSecret() {
        // 設定不備は最初のJWT発行時ではなく、アプリケーション起動時に検出する。
        getDecodedSecretBytes();
    }

    /**
     * Base64エンコード済み秘密鍵をデコードし、JWT署名に利用可能な強度であることを検証する。
     *
     * @return デコード済み秘密鍵バイト列
     * @throws IllegalStateException 秘密鍵がBase64として不正、またはHS256署名に必要な長さを満たさない場合
     */
    public byte[] getDecodedSecretBytes() {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secret);
            // JJWTの検証を通して、短すぎる鍵を設定段階で弾く。
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
