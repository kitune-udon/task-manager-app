package com.example.task.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 未読通知件数のレスポンス DTO。
 */
@Getter
@AllArgsConstructor
public class UnreadCountResponse {
    private long unreadCount;
}
