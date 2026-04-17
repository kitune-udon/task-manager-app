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

    public JwtUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * 認証済みユーザーを表す JWT を生成する。
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
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * トークン利用者と期限の両方をチェックして再利用可能か判定する。
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 設定値の Base64 文字列を JWT 署名に使える秘密鍵へ変換する。
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getDecodedSecretBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
