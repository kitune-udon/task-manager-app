package com.example.task.entity;

/**
 * アクティビティログと通知生成元で利用するイベント種別。
 */
public enum ActivityEventType {
    TASK_CREATED,
    TASK_UPDATED,
    TASK_DELETED,
    COMMENT_CREATED,
    COMMENT_UPDATED,
    COMMENT_DELETED,
    ATTACHMENT_UPLOADED,
    ATTACHMENT_DELETED
}
