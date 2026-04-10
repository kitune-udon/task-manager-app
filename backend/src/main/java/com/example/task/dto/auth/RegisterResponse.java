package com.example.task.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * ユーザー登録完了後に返すレスポンス DTO。
 */
@Getter
@Builder
@AllArgsConstructor
public class RegisterResponse {

    private Long id;
    private String name;
    private String email;
    private LocalDateTime createdAt;
}
