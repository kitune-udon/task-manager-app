package com.example.task.security;

import com.example.task.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT の生成と検証に必要な共通処理をまとめるユーティリティ。
 */
@Component
public class JwtUtil {

    private final JwtProperties jwtProperties;

    /**
     * JWTユーティリティを生成する。
     *
     * @param jwtProperties JWT設定
     */
    public JwtUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * 認証済みユーザーを表す JWT を生成する。
     *
     * <p>subjectにはログイン識別子であるメールアドレスを設定し、
     * {@link CustomUserDetails} の場合はユーザーIDもクレームに含める。</p>
     *
     * @param userDetails 認証済みユーザー情報
     * @return 署名済みJWT
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        if (userDetails instanceof CustomUserDetails customUserDetails) {
            // API 側で再利用できるよう、必要なユーザー ID もクレームに含める。
            claims.put("userId", customUserDetails.getId());
        }

        Instant now = Instant.now();
        Instant expiry = now.plusMillis(jwtProperties.getExpirationMillis());

        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * トークンの subject に保存したメールアドレスを取り出す。
     *
     * @param token JWT
     * @return ログイン識別子として利用するメールアドレス
     * @throws io.jsonwebtoken.JwtException JWTの署名検証または解析に失敗した場合
     * @throws IllegalArgumentException JWTが空または不正な場合
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * トークン利用者と期限の両方をチェックして再利用可能か判定する。
     *
     * @param token JWT
     * @param userDetails 比較対象のユーザー情報
     * @return subjectがユーザー名と一致し、期限切れでない場合はtrue
     * @throws io.jsonwebtoken.JwtException JWTの署名検証または解析に失敗した場合
     * @throws IllegalArgumentException JWTが空または不正な場合
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /**
     * トークンの有効期限が現在時刻より前かどうかを判定する。
     *
     * @param token JWT
     * @return 期限切れの場合はtrue
     */
    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    /**
     * JWTを検証してすべてのクレームを取得する。
     *
     * @param token JWT
     * @return 検証済みクレーム
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 設定値の Base64 文字列を JWT 署名に使える秘密鍵へ変換する。
     *
     * @return HMAC署名用の秘密鍵
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getDecodedSecretBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
