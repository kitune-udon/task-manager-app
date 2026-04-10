package com.example.task.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * タスク関連レスポンスで共通利用する簡易ユーザー DTO。
 */
@Getter
@Builder
public class TaskUserResponse {
    private Long id;
    private String name;
}
