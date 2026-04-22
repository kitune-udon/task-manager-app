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

    /**
     * 指定タスクに紐づく未削除の添付ファイルを新しい順に取得する。
     *
     * @param taskId タスクID
     * @return 未削除の添付ファイルリスト
     */
    @EntityGraph(attributePaths = {"createdBy"})
    // 一覧表示で利用する作成者情報を同時に取得する。
    @Query("""
            select a
            from TaskAttachment a
            where a.task.id = :taskId
              and a.deletedAt is null
            order by a.createdAt desc, a.id desc
            """)
    List<TaskAttachment> findActiveByTaskId(@Param("taskId") Long taskId);

    /**
     * 未削除の添付ファイルをIDで取得する。
     *
     * @param id 添付ファイルID
     * @return 条件に一致する添付ファイル
     */
    @EntityGraph(attributePaths = {"task", "createdBy", "deletedBy", "task.createdBy", "task.assignedUser"})
    // 権限確認とレスポンス変換に必要なタスク・ユーザー情報をまとめて取得する。
    Optional<TaskAttachment> findByIdAndDeletedAtIsNull(Long id);

    /**
     * 指定タスクに紐づく未削除の添付ファイル数を取得する。
     *
     * @param taskId タスクID
     * @return 未削除の添付ファイル数
     */
    @Query("""
            select count(a)
            from TaskAttachment a
            where a.task.id = :taskId
              and a.deletedAt is null
            """)
    long countActiveByTaskId(@Param("taskId") Long taskId);

    /**
     * 指定タスクに紐づく未削除の添付ファイルサイズ合計を取得する。
     *
     * @param taskId タスクID
     * @return 未削除の添付ファイルサイズ合計。添付がない場合は0
     */
    @Query("""
            select coalesce(sum(a.fileSize), 0)
            from TaskAttachment a
            where a.task.id = :taskId
              and a.deletedAt is null
            """)
    long sumActiveFileSizeByTaskId(@Param("taskId") Long taskId);
}
