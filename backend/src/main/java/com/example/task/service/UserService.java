package com.example.task.service;

import com.example.task.dto.UserResponse;
import com.example.task.entity.User;
import com.example.task.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ユーザー一覧取得などの参照系処理を担当するサービス。
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 名前順で安定したユーザー一覧を返し、画面側で扱いやすい DTO に変換する。
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getUsers() {
        return userRepository.findAll(Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"))).stream()
                .map(this::toUserResponse)
                .toList();
    }

    /**
     * Entity を API 応答用 DTO に写像する。
     */
    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }
}
