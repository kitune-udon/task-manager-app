package com.example.task.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * ユーザー登録 API の入力値を受け取る DTO。
 */
@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "ユーザー名を入力してください")
    @Size(max = 100, message = "ユーザー名は100文字以内で入力してください")
    private String name;

    @NotBlank(message = "メールアドレスを入力してください")
    @Email(message = "メールアドレスの形式が不正です")
    @Size(max = 255, message = "メールアドレスは255文字以内で入力してください")
    private String email;

    @NotBlank(message = "パスワードを入力してください")
    @Size(min = 8, max = 100, message = "パスワードは8文字以上100文字以下で入力してください")
    private String password;
}
