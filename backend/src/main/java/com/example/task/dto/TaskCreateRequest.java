package com.example.task.dto;

import com.example.task.entity.Priority;
import com.example.task.entity.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * タスク作成 API の入力値を受け取る DTO。
 */
@Getter
@Setter
public class TaskCreateRequest {

    @NotBlank(message = "タイトルを入力してください")
    @Size(max = 100, message = "タイトルは100文字以内で入力してください")
    private String title;

    @Size(max = 5000, message = "説明は5000文字以内で入力してください")
    private String description;

    @NotNull(message = "ステータスを選択してください")
    private TaskStatus status;

    @NotNull(message = "優先度を選択してください")
    private Priority priority;

    private LocalDate dueDate;

    private Long assignedUserId;

    @NotNull(message = "タスクを作成するチームを指定してください")
    private Long teamId;
}
