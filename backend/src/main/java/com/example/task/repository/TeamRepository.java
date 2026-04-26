package com.example.task.repository;

import com.example.task.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Team エンティティの保存と参照を担当するリポジトリ。
 */
public interface TeamRepository extends JpaRepository<Team, Long> {

    boolean existsByCreatedByIdAndName(Long createdById, String name);

    Optional<Team> findByCreatedByIdAndName(Long createdById, String name);

    Optional<Team> findById(Long id);

    List<Team> findByCreatedById(Long createdById);
}
