package com.example.task.controller;

import com.example.task.dto.auth.LoginRequest;
import com.example.task.dto.auth.LoginResponse;
import com.example.task.dto.auth.RegisterRequest;
import com.example.task.dto.auth.RegisterResponse;
import com.example.task.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ユーザー登録とログインを受け付ける認証 API。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 新規ユーザーを登録し、作成結果を返す。
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    /**
     * 認証に成功したユーザーへ JWT を払い出す。
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
