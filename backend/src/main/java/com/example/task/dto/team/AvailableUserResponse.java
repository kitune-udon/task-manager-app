package com.example.task.dto.team;

import lombok.Builder;
import lombok.Getter;

/**
 * チーム追加候補ユーザー API が返すレスポンス DTO。
 */
@Getter
@Builder
public class AvailableUserResponse {
    private Long userId;
    private String name;
    private String email;
}
