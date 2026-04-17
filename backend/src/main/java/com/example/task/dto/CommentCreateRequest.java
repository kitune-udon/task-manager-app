package com.example.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * コメント投稿の入力 DTO。
 */
@Getter
@Setter
public class CommentCreateRequest {

    @NotBlank(message = "コメント内容を入力してください")
    @Size(max = 1000, message = "コメント内容は1000文字以内で入力してください")
    private String content;
}
