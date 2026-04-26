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
     *
     * @param currentUserId 現在のユーザーID
     * @param status 絞り込み対象のステータス（任意）
     * @param priority 絞り込み対象の優先度（任意）
     * @param assignedUserId 絞り込み対象の担当者ID（任意）
     * @param keywordPattern 小文字化済みのLIKE検索パターン（任意）
     * @return 条件に一致する参照可能な未削除タスク
     */
    @EntityGraph(attributePaths = {"assignedUser", "createdBy", "team"})
    // 一覧表示に必要な担当者・作成者・チームを同時に取得する。
    @Query("""
            select t
            from Task t
            where exists (
                select tm.id
                from TeamMember tm
                where tm.team = t.team
                  and tm.user.id = :currentUserId
            )
              and t.deletedAt is null
              and (:teamId is null or t.team.id = :teamId)
              and (:status is null or t.status = :status)
              and (:priority is null or t.priority = :priority)
              and (:assignedUserId is null or (t.assignedUser is not null and t.assignedUser.id = :assignedUserId))
              and (:keywordPattern is null or lower(t.title) like :keywordPattern)
            order by t.createdAt desc, t.id desc
            """)
    List<Task> searchAccessible(
            @Param("currentUserId") Long currentUserId,
            @Param("teamId") Long teamId,
            @Param("status") TaskStatus status,
            @Param("priority") Priority priority,
            @Param("assignedUserId") Long assignedUserId,
            @Param("keywordPattern") String keywordPattern
    );

    /**
     * 詳細表示で必要な関連ユーザーをまとめて読み込む。
     *
     * @param id タスクID
     * @return 条件に一致するタスク
     */
    @EntityGraph(attributePaths = {"assignedUser", "createdBy", "deletedBy", "team"})
    Optional<Task> findWithAssignedUserById(Long id);

    /**
     * 未削除のタスクをIDで取得し、関連ユーザーをまとめて読み込む。
     *
     * @param id タスクID
     * @return 条件に一致する未削除タスク
     */
    @EntityGraph(attributePaths = {"assignedUser", "createdBy", "deletedBy", "team"})
    Optional<Task> findWithAssignedUserByIdAndDeletedAtIsNull(Long id);

    /**
     * 更新処理向けにタスクをIDで取得する。
     *
     * @param taskId タスクID
     * @return 条件に一致するタスク
     */
    @EntityGraph(attributePaths = {"assignedUser", "createdBy", "deletedBy", "team"})
    // 更新前後の差分判定や通知生成で必要な関連ユーザーを同時に取得する。
    @Query("""
            select t
            from Task t
            where t.id = :taskId
            """)
    Optional<Task> findForUpdate(@Param("taskId") Long taskId);

    /**
     * 作成者または担当者として参照可能な未削除タスクをページングして取得する。
     *
     * @param currentUserId 現在のユーザーID
     * @param pageable ページング条件
     * @return ページングされた参照可能な未削除タスク
     */
    @EntityGraph(attributePaths = {"assignedUser", "createdBy", "team"})
    @Query("""
            select t
            from Task t
            where exists (
                select tm.id
                from TeamMember tm
                where tm.team = t.team
                  and tm.user.id = :currentUserId
            )
              and t.deletedAt is null
            """)
    Page<Task> findAccessiblePage(@Param("currentUserId") Long currentUserId, Pageable pageable);

    /**
     * team_id マッピング追加前の認可補助として、未削除タスクの所属チームIDだけを取得する。
     *
     * @param taskId タスクID
     * @return タスクが存在し未削除の場合は所属チームID
     */
    @Query("""
            select t.team.id
            from Task t
            where t.id = :taskId
              and t.deletedAt is null
            """)
    Optional<Long> findActiveTeamIdByTaskId(@Param("taskId") Long taskId);

    /**
     * 指定ユーザーが同一チーム内の未削除タスクで担当者になっている件数を返す。
     *
     * @param teamId チームID
     * @param userId 担当者ユーザーID
     * @return 未削除タスクの担当件数
     */
    @Query("""
            select count(t)
            from Task t
            where t.team.id = :teamId
              and t.assignedUser.id = :userId
              and t.deletedAt is null
            """)
    long countActiveAssignmentsByTeamIdAndUserId(@Param("teamId") Long teamId, @Param("userId") Long userId);
}
