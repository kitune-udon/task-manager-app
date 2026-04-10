package com.example.task.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * ページング結果をまとめて返すための共通 DTO。
 */
@Getter
@Builder
public class PageResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
