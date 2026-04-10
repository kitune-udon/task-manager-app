package com.example.task.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 入力エラー時にどの項目が問題かを表す詳細 DTO。
 */
@Getter
@Builder
@AllArgsConstructor
public class ErrorDetail {
    private String field;
    private String message;
}
