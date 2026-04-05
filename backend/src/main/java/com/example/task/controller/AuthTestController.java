package com.example.task.controller;

import com.example.task.security.CurrentUserProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth-test")
public class AuthTestController {

    private final CurrentUserProvider currentUserProvider;

    public AuthTestController(CurrentUserProvider currentUserProvider) {
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/me")
    public Map<String, Object> me() {
        return Map.of(
                "userId", currentUserProvider.getCurrentUserId(),
                "email", currentUserProvider.getCurrentUserEmail()
        );
    }
}
