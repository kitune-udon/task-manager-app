package com.example.task.service;

import com.example.task.entity.Task;
import com.example.task.entity.Team;
import com.example.task.entity.TeamMember;
import com.example.task.entity.TeamRole;
import com.example.task.entity.User;
import com.example.task.exception.BusinessException;
import com.example.task.exception.ErrorCode;
import com.example.task.exception.ResourceNotFoundException;
import com.example.task.repository.TaskRepository;
import com.example.task.repository.TeamMemberRepository;
import com.example.task.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * タスクを親に持つ機能向けの存在確認と認可判定を共通化する。
 */
@Service
public class TaskAuthorizationService {

    private final TaskRepository taskRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;

    /**
     * タスク認可サービスを生成する。
     *
     * @param taskRepository タスクリポジトリ
     * @param teamRepository チームリポジトリ
     * @param teamMemberRepository チームメンバーリポジトリ
     */
    public TaskAuthorizationService(
            TaskRepository taskRepository,
            TeamRepository teamRepository,
            TeamMemberRepository teamMemberRepository
    ) {
        this.taskRepository = taskRepository;
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
    }

    /**
     * チーム参照権限を検証する。
     *
     * @param currentUserId 現在のユーザーID
     * @param teamId チームID
     * @return ログインユーザーのチームメンバー情報
     */
    @Transactional(readOnly = true)
    public TeamMember authorizeTeamView(Long currentUserId, Long teamId) {
        return requireMembership(currentUserId, teamId);
    }

    /**
     * チームメンバー追加権限を検証する。
     *
     * @param currentUserId 現在のユーザーID
     * @param teamId チームID
     * @return ログインユーザーのチームメンバー情報
     */
    @Transactional(readOnly = true)
    public TeamMember authorizeTeamMemberAdd(Long currentUserId, Long teamId) {
        TeamMember membership = requireMembership(currentUserId, teamId);
        requireAnyRole(membership, ErrorCode.TEAM_MEMBER_002, TeamRole.OWNER, TeamRole.ADMIN);
        return membership;
    }

    /**
     * チームメンバーのロール変更権限を検証する。
     *
     * @param currentUserId 現在のユーザーID
     * @param teamId チームID
     * @return ログインユーザーのチームメンバー情報
     */
    @Transactional(readOnly = true)
    public TeamMember authorizeTeamRoleChange(Long currentUserId, Long teamId) {
        TeamMember membership = requireMembership(currentUserId, teamId);
        requireAnyRole(membership, ErrorCode.TEAM_MEMBER_005, TeamRole.OWNER);
        return membership;
    }

    /**
     * チームメンバー削除権限を検証する。
     *
     * @param currentUserId 現在のユーザーID
     * @param teamId チームID
     * @return ログインユーザーのチームメンバー情報
     */
    @Transactional(readOnly = true)
    public TeamMember authorizeTeamMemberRemove(Long currentUserId, Long teamId) {
        TeamMember membership = requireMembership(currentUserId, teamId);
        requireAnyRole(membership, ErrorCode.TEAM_MEMBER_008, TeamRole.OWNER, TeamRole.ADMIN);
        return membership;
    }

    /**
     * チーム文脈のタスク一覧参照権限を検証する。
     *
     * @param currentUserId 現在のユーザーID
     * @param teamId チームID
     * @return ログインユーザーのチームメンバー情報
     */
    @Transactional(readOnly = true)
    public TeamMember authorizeTaskListInTeam(Long currentUserId, Long teamId) {
        return requireMembership(currentUserId, teamId, ErrorCode.TASK_009);
    }

    /**
     * チーム文脈のタスク作成権限を検証する。
     *
     * @param currentUserId 現在のユーザーID
     * @param teamId チームID
     * @return ログインユーザーのチームメンバー情報
     */
    @Transactional(readOnly = true)
    public TeamMember authorizeTaskCreateInTeam(Long currentUserId, Long teamId) {
        return requireMembership(currentUserId, teamId, ErrorCode.TASK_009);
    }

    /**
     * チーム所属前提のタスク参照権限を検証する。
     *
     * @param currentUserId 現在のユーザーID
     * @param taskId タスクID
     */
    @Transactional(readOnly = true)
    public void authorizeTaskViewInTeam(Long currentUserId, Long taskId) {
        authorizeTaskOperationInTeam(currentUserId, taskId);
    }

    /**
     * チーム所属前提のタスク更新権限を検証する。
     *
     * @param currentUserId 現在のユーザーID
     * @param taskId タスクID
     */
    @Transactional(readOnly = true)
    public void authorizeTaskUpdateInTeam(Long currentUserId, Long taskId) {
        Task task = getActiveTask(taskId);
        authorizeUpdate(task, currentUserId);
    }

    /**
     * チーム所属前提のタスク削除権限を検証する。
     *
     * @param currentUserId 現在のユーザーID
     * @param taskId タスクID
     */
    @Transactional(readOnly = true)
    public void authorizeTaskDeleteInTeam(Long currentUserId, Long taskId) {
        Task task = getActiveTask(taskId);
        authorizeDelete(task, currentUserId);
    }

    /**
     * 削除されていないタスクを取得する。
     *
     * @param taskId タスクID
     * @return アクティブなタスク
     * @throws ResourceNotFoundException タスクが存在しない、または削除済みの場合
     */
    @Transactional(readOnly = true)
    public Task getActiveTask(Long taskId) {
        return taskRepository.findWithAssignedUserByIdAndDeletedAtIsNull(taskId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RES_TASK_404, "タスクが存在しません"));
    }

    /**
     * 現在のユーザーが参照可能なタスクを取得する。
     *
     * <p>タスクの所属チームメンバーの場合に参照を許可する。</p>
     *
     * @param taskId タスクID
     * @param currentUserId 現在のユーザーID
     * @return 参照可能なタスク
     * @throws ResourceNotFoundException タスクが存在しない、または削除済みの場合
     * @throws BusinessException 参照権限がない場合
     */
    @Transactional(readOnly = true)
    public Task getViewableTask(Long taskId, Long currentUserId) {
        Task task = getActiveTask(taskId);
        authorizeView(task, currentUserId);
        return task;
    }

    /**
     * 現在のユーザーが更新可能なタスクを取得する。
     *
     * <p>タスク作成者、担当者、または所属チームのOWNER/ADMINの場合に更新を許可する。</p>
     *
     * @param taskId タスクID
     * @param currentUserId 現在のユーザーID
     * @return 更新可能なタスク
     * @throws ResourceNotFoundException タスクが存在しない、または削除済みの場合
     * @throws BusinessException 更新権限がない場合
     */
    @Transactional(readOnly = true)
    public Task getUpdatableTask(Long taskId, Long currentUserId) {
        Task task = getActiveTask(taskId);
        authorizeUpdate(task, currentUserId);
        return task;
    }

    /**
     * 現在のユーザーが削除可能なタスクを取得する。
     *
     * <p>タスク作成者、または所属チームのOWNER/ADMINの場合に削除を許可する。</p>
     *
     * @param taskId タスクID
     * @param currentUserId 現在のユーザーID
     * @return 削除可能なタスク
     * @throws ResourceNotFoundException タスクが存在しない、または削除済みの場合
     * @throws BusinessException 削除権限がない場合
     */
    @Transactional(readOnly = true)
    public Task getDeletableTask(Long taskId, Long currentUserId) {
        Task task = getActiveTask(taskId);
        authorizeDelete(task, currentUserId);
        return task;
    }

    /**
     * タスクの参照権限を検証する。
     *
     * @param task 検証対象のタスク
     * @param currentUserId 現在のユーザーID
     * @throws BusinessException タスクの所属チームメンバーではない場合
     */
    public void authorizeView(Task task, Long currentUserId) {
        requireTaskTeamMembership(task, currentUserId, ErrorCode.TASK_009);
    }

    /**
     * タスクの更新権限を検証する。
     *
     * @param task 検証対象のタスク
     * @param currentUserId 現在のユーザーID
     * @throws BusinessException タスク更新権限がない場合
     */
    public void authorizeUpdate(Task task, Long currentUserId) {
        TeamMember membership = requireTaskTeamMembership(task, currentUserId, ErrorCode.TASK_009);
        if (isTaskCreator(task, currentUserId) || isTaskAssignee(task, currentUserId) || isTeamManager(membership)) {
            return;
        }
        throw new BusinessException(ErrorCode.PERM_TASK_403_UPD);
    }

    /**
     * タスクの削除権限を検証する。
     *
     * @param task 検証対象のタスク
     * @param currentUserId 現在のユーザーID
     * @throws BusinessException タスク削除権限がない場合
     */
    public void authorizeDelete(Task task, Long currentUserId) {
        TeamMember membership = requireTaskTeamMembership(task, currentUserId, ErrorCode.TASK_009);
        if (isTaskCreator(task, currentUserId) || isTeamManager(membership)) {
            return;
        }
        throw new BusinessException(ErrorCode.PERM_TASK_403_DEL);
    }

    /**
     * コメント更新・削除権限を検証する。
     *
     * @param task コメントの親タスク
     * @param currentUserId 現在のユーザーID
     * @param commentAuthorId コメント作成者ID
     * @param errorCode 権限不足時のエラーコード
     * @throws BusinessException コメント操作権限がない場合
     */
    public void authorizeCommentMutation(Task task, Long currentUserId, Long commentAuthorId, ErrorCode errorCode) {
        TeamMember membership = requireTaskTeamMembership(task, currentUserId, ErrorCode.TASK_009);
        if (currentUserId.equals(commentAuthorId) || isTeamManager(membership)) {
            return;
        }
        throw new BusinessException(errorCode);
    }

    /**
     * 添付ファイル削除権限を検証する。
     *
     * @param task 添付ファイルの親タスク
     * @param currentUserId 現在のユーザーID
     * @param uploaderId アップロード者ID
     * @throws BusinessException 添付削除権限がない場合
     */
    public void authorizeAttachmentDelete(Task task, Long currentUserId, Long uploaderId) {
        TeamMember membership = requireTaskTeamMembership(task, currentUserId, ErrorCode.TASK_009);
        if (currentUserId.equals(uploaderId) || isTeamManager(membership)) {
            return;
        }
        throw new BusinessException(ErrorCode.FILE_004);
    }

    /**
     * 指定されたユーザーがタスク作成者かどうかを判定する。
     *
     * @param task 判定対象のタスク
     * @param currentUserId 現在のユーザーID
     * @return タスク作成者の場合はtrue
     */
    public boolean isTaskCreator(Task task, Long currentUserId) {
        User createdBy = task.getCreatedBy();
        return createdBy != null && currentUserId.equals(createdBy.getId());
    }

    /**
     * 指定されたユーザーがタスク担当者かどうかを判定する。
     *
     * @param task 判定対象のタスク
     * @param currentUserId 現在のユーザーID
     * @return タスク担当者の場合はtrue
     */
    public boolean isTaskAssignee(Task task, Long currentUserId) {
        User assignedUser = task.getAssignedUser();
        return assignedUser != null && currentUserId.equals(assignedUser.getId());
    }

    /**
     * チーム存在と所属を検証する。
     */
    private TeamMember requireMembership(Long currentUserId, Long teamId) {
        return requireMembership(currentUserId, teamId, ErrorCode.TEAM_003);
    }

    /**
     * チーム存在と所属を検証する。
     */
    private TeamMember requireMembership(Long currentUserId, Long teamId, ErrorCode notMemberErrorCode) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TEAM_004));

        return teamMemberRepository.findByTeamIdAndUserId(team.getId(), currentUserId)
                .orElseThrow(() -> new BusinessException(
                        notMemberErrorCode,
                        resolveTeamAccessFailureEventId(notMemberErrorCode),
                        teamFields(team.getId())
                ));
    }

    /**
     * 指定されたいずれかのチームロールを持つことを検証する。
     */
    private void requireAnyRole(TeamMember membership, ErrorCode errorCode, TeamRole... allowedRoles) {
        EnumSet<TeamRole> allowed = EnumSet.noneOf(TeamRole.class);
        for (TeamRole role : allowedRoles) {
            allowed.add(role);
        }

        if (!allowed.contains(membership.getRole())) {
            throw new BusinessException(
                    errorCode,
                    resolveTeamMemberFailureEventId(errorCode),
                    teamMemberFields(membership.getTeam().getId(), membership.getRole())
            );
        }
    }

    /**
     * タスク存在と所属チームへのユーザー所属を検証する。
     */
    private void authorizeTaskOperationInTeam(Long currentUserId, Long taskId) {
        Long teamId = taskRepository.findActiveTeamIdByTaskId(taskId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RES_TASK_404, "タスクが存在しません"));

        requireMembership(currentUserId, teamId, ErrorCode.TASK_009);
    }

    /**
     * タスクの所属チームへユーザーが所属していることを検証する。
     */
    private TeamMember requireTaskTeamMembership(Task task, Long currentUserId, ErrorCode errorCode) {
        Team team = task.getTeam();
        if (team == null) {
            throw new ResourceNotFoundException(ErrorCode.RES_TASK_404, "タスクが存在しません");
        }

        return teamMemberRepository.findByTeamIdAndUserId(team.getId(), currentUserId)
                .orElseThrow(() -> new BusinessException(
                        errorCode,
                        resolveTeamAccessFailureEventId(errorCode),
                        teamFields(team.getId())
                ));
    }

    private boolean isTeamManager(TeamMember membership) {
        return membership.getRole() == TeamRole.OWNER || membership.getRole() == TeamRole.ADMIN;
    }

    private String resolveTeamAccessFailureEventId(ErrorCode errorCode) {
        if (errorCode == ErrorCode.TEAM_003 || errorCode == ErrorCode.TASK_009) {
            return "LOG-TEAM-101";
        }
        return null;
    }

    private String resolveTeamMemberFailureEventId(ErrorCode errorCode) {
        if (errorCode == ErrorCode.TEAM_MEMBER_002) {
            return "LOG-TEAM-102";
        }
        if (errorCode == ErrorCode.TEAM_MEMBER_005) {
            return "LOG-TEAM-103";
        }
        if (errorCode == ErrorCode.TEAM_MEMBER_008) {
            return "LOG-TEAM-104";
        }
        return null;
    }

    private Map<String, Object> teamFields(Long teamId) {
        LinkedHashMap<String, Object> fields = new LinkedHashMap<>();
        fields.put("teamId", teamId);
        return fields;
    }

    private Map<String, Object> teamMemberFields(Long teamId, TeamRole actorRole) {
        LinkedHashMap<String, Object> fields = new LinkedHashMap<>();
        fields.put("teamId", teamId);
        fields.put("actorRole", actorRole);
        return fields;
    }
}
