package com.example.task.repository;

import com.example.task.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * User エンティティの永続化とメールアドレス検索を担当するリポジトリ。
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * ログイン処理で使うメールアドレス検索。
     */
    Optional<User> findByEmail(String email);

    /**
     * 新規登録時の重複チェック。
     */
    boolean existsByEmail(String email);
}
