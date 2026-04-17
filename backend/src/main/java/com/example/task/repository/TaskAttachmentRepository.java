package com.example.task.repository;

import com.example.task.entity.TaskAttachment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 添付ファイルの一覧取得と容量集計を扱うリポジトリ。
 */
public interface TaskAttachmentRepository extends JpaRepository<TaskAttachment, Long> {

    @EntityGraph(attributePaths = {"createdBy"})
    @Query("""
            select a
            from TaskAttachment a
            where a.task.id = :taskId
              and a.deletedAt is null
            order by a.createdAt desc, a.id desc
            """)
    List<TaskAttachment> findActiveByTaskId(@Param("taskId") Long taskId);

    @EntityGraph(attributePaths = {"task", "createdBy", "deletedBy", "task.createdBy", "task.assignedUser"})
    Optional<TaskAttachment> findByIdAndDeletedAtIsNull(Long id);

    @Query("""
            select count(a)
            from TaskAttachment a
            where a.task.id = :taskId
              and a.deletedAt is null
            """)
    long countActiveByTaskId(@Param("taskId") Long taskId);

    @Query("""
            select coalesce(sum(a.fileSize), 0)
            from TaskAttachment a
            where a.task.id = :taskId
              and a.deletedAt is null
            """)
    long sumActiveFileSizeByTaskId(@Param("taskId") Long taskId);
}
