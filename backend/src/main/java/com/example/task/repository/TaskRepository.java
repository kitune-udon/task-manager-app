package com.example.task.repository;

import com.example.task.entity.Priority;
import com.example.task.entity.Task;
import com.example.task.entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Task エンティティの保存と参照条件付き検索を提供するリポジトリ。
 */
public interface TaskRepository extends JpaRepository<Task, Long> {

    /**
     * 作成者または担当者として参照可能なタスクだけを条件付きで取得する。
     */
    @EntityGraph(attributePaths = {"assignedUser", "createdBy"})
    @Query("""
            select t
            from Task t
            where (t.createdBy.id = :currentUserId
                   or (t.assignedUser is not null and t.assignedUser.id = :currentUserId))
              and t.deletedAt is null
              and (:status is null or t.status = :status)
              and (:priority is null or t.priority = :priority)
              and (:assignedUserId is null or (t.assignedUser is not null and t.assignedUser.id = :assignedUserId))
              and (:keywordPattern is null or lower(t.title) like :keywordPattern)
            order by t.createdAt desc, t.id desc
            """)
    List<Task> searchAccessible(
            @Param("currentUserId") Long currentUserId,
            @Param("status") TaskStatus status,
            @Param("priority") Priority priority,
            @Param("assignedUserId") Long assignedUserId,
            @Param("keywordPattern") String keywordPattern
    );

    /**
     * 詳細表示で必要な関連ユーザーをまとめて読み込む。
     */
    @EntityGraph(attributePaths = {"assignedUser", "createdBy", "deletedBy"})
    Optional<Task> findWithAssignedUserById(Long id);

    @EntityGraph(attributePaths = {"assignedUser", "createdBy", "deletedBy"})
    Optional<Task> findWithAssignedUserByIdAndDeletedAtIsNull(Long id);

    @EntityGraph(attributePaths = {"assignedUser", "createdBy", "deletedBy"})
    @Query("""
            select t
            from Task t
            where t.id = :taskId
            """)
    Optional<Task> findForUpdate(@Param("taskId") Long taskId);

    @EntityGraph(attributePaths = {"assignedUser", "createdBy"})
    @Query("""
            select t
            from Task t
            where (t.createdBy.id = :currentUserId
                   or (t.assignedUser is not null and t.assignedUser.id = :currentUserId))
              and t.deletedAt is null
            """)
    Page<Task> findAccessiblePage(@Param("currentUserId") Long currentUserId, Pageable pageable);
}
