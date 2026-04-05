package com.example.task.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "name is required")
    @Size(max = 100, message = "name must be 100 characters or less")
    private String name;

    @NotBlank(message = "email is required")
    @Email(message = "email format is invalid")
    @Size(max = 255, message = "email must be 255 characters or less")
    private String email;

    @NotBlank(message = "password is required")
    @Size(min = 8, max = 100, message = "password must be between 8 and 100 characters")
    private String password;
}
