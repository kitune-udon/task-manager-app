package com.example.task.dto.auth;

import com.example.task.dto.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * ログイン成功時に返す JWT とユーザー情報。
 */
@Getter
@Builder
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private UserResponse user;
}
