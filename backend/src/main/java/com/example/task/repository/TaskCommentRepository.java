package com.example.task.repository;

import com.example.task.entity.TaskComment;
import com.example.task.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * コメントの一覧取得と個別参照を扱うリポジトリ。
 */
public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {

    /**
     * 指定タスクに紐づく未削除のコメントを作成日時の古い順に取得する。
     *
     * @param taskId タスクID
     * @param pageable ページング条件
     * @return ページングされた未削除コメント
     */
    @EntityGraph(attributePaths = {"createdBy"})
    // 一覧表示で利用する作成者情報を同時に取得する。
    @Query("""
            select c
            from TaskComment c
            where c.task.id = :taskId
              and c.deletedAt is null
            order by c.createdAt asc, c.id asc
            """)
    Page<TaskComment> findActiveByTaskId(@Param("taskId") Long taskId, Pageable pageable);

    /**
     * 未削除のコメントをIDで取得する。
     *
     * @param id コメントID
     * @return 条件に一致するコメント
     */
    @EntityGraph(attributePaths = {"task", "createdBy", "updatedBy", "deletedBy"})
    // 詳細表示や更新・削除処理で必要な関連情報をまとめて取得する。
    Optional<TaskComment> findByIdAndDeletedAtIsNull(Long id);

    /**
     * 削除済みを含めてコメントをIDで取得する。
     *
     * @param id コメントID
     * @return 条件に一致するコメント
     */
    @EntityGraph(attributePaths = {"task", "createdBy", "updatedBy", "deletedBy"})
    Optional<TaskComment> findById(Long id);

    /**
     * 指定タスクに紐づく未削除コメントをまとめて論理削除する。
     *
     * @param taskId タスクID
     * @param deletedAt 削除日時
     * @param deletedBy 削除者
     * @return 更新件数
     */
    @Modifying
    @Query("""
            update TaskComment c
            set c.deletedAt = :deletedAt,
                c.deletedBy = :deletedBy
            where c.task.id = :taskId
              and c.deletedAt is null
            """)
    int softDeleteActiveByTaskId(
            @Param("taskId") Long taskId,
            @Param("deletedAt") LocalDateTime deletedAt,
            @Param("deletedBy") User deletedBy
    );
}
