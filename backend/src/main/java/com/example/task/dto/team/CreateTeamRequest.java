package com.example.task.dto.team;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * チーム作成 API の入力値を受け取る DTO。
 */
@Getter
@Setter
public class CreateTeamRequest {

    @NotBlank(message = "チーム名を入力してください")
    @Size(max = 100, message = "チーム名は100文字以内で入力してください")
    private String name;

    @Size(max = 1000, message = "チーム説明は1000文字以内で入力してください")
    private String description;
}
