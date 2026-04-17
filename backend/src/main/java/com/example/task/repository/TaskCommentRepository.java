package com.example.task.repository;

import com.example.task.entity.TaskComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * コメントの一覧取得と個別参照を扱うリポジトリ。
 */
public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {

    @EntityGraph(attributePaths = {"createdBy"})
    @Query("""
            select c
            from TaskComment c
            where c.task.id = :taskId
              and c.deletedAt is null
            order by c.createdAt asc, c.id asc
            """)
    Page<TaskComment> findActiveByTaskId(@Param("taskId") Long taskId, Pageable pageable);

    @EntityGraph(attributePaths = {"task", "createdBy", "updatedBy", "deletedBy"})
    Optional<TaskComment> findByIdAndDeletedAtIsNull(Long id);

    @EntityGraph(attributePaths = {"task", "createdBy", "updatedBy", "deletedBy"})
    Optional<TaskComment> findById(Long id);
}
