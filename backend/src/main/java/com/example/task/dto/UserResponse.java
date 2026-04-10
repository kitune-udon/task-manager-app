package com.example.task.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * ユーザー一覧 API が返す基本情報 DTO。
 */
@Getter
@Builder
public class UserResponse {
    private Long id;
    private String name;
    private String email;
}
