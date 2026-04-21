package com.example.task.controller;

import com.example.task.security.CurrentUserProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 認証フィルタの動作確認に使う簡易テスト用 API。
 */
@RestController
@RequestMapping("/api/auth-test")
public class AuthTestController {

    private final CurrentUserProvider currentUserProvider;

    /**
     * 認証テスト用コントローラーを生成する。
     *
     * @param currentUserProvider 現在のユーザー提供者
     */
    public AuthTestController(CurrentUserProvider currentUserProvider) {
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * 現在の認証コンテキストから取得したユーザー情報をそのまま返す。
     *
     * @return 認証済みユーザーIDとメールアドレス
     */
    @GetMapping("/me")
    public Map<String, Object> me() {
        return Map.of(
                "userId", currentUserProvider.getCurrentUserId(),
                "email", currentUserProvider.getCurrentUserEmail()
        );
    }
}
