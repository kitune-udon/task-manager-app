package com.example.task.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 汎用的な成功レスポンスを表現するラッパー DTO。
 */
@Getter
@Builder
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String message;
}
