package com.example.task.service;

import com.example.task.dto.team.AddTeamMemberRequest;
import com.example.task.dto.team.TeamMemberResponse;
import com.example.task.dto.team.UpdateTeamMemberRoleRequest;
import com.example.task.entity.Team;
import com.example.task.entity.TeamMember;
import com.example.task.entity.TeamRole;
import com.example.task.entity.User;
import com.example.task.exception.BusinessException;
import com.example.task.exception.ErrorCode;
import com.example.task.exception.ResourceNotFoundException;
import com.example.task.repository.TaskRepository;
import com.example.task.repository.TeamMemberRepository;
import com.example.task.repository.UserRepository;
import com.example.task.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * チームメンバー追加、ロール変更、削除を扱うサービス。
 */
@Service
public class TeamMemberService {

    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final CurrentUserProvider currentUserProvider;
    private final TaskAuthorizationService taskAuthorizationService;
    private final TeamAuditLogService teamAuditLogService;

    public TeamMemberService(
            TeamMemberRepository teamMemberRepository,
            UserRepository userRepository,
            TaskRepository taskRepository,
            CurrentUserProvider currentUserProvider,
            TaskAuthorizationService taskAuthorizationService,
            TeamAuditLogService teamAuditLogService
    ) {
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.currentUserProvider = currentUserProvider;
        this.taskAuthorizationService = taskAuthorizationService;
        this.teamAuditLogService = teamAuditLogService;
    }

    /**
     * OWNER / ADMIN 権限でチームメンバーを追加する。
     */
    @Transactional
    public TeamMemberResponse addMember(Long teamId, AddTeamMemberRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        TeamMember actorMembership = taskAuthorizationService.authorizeTeamMemberAdd(currentUserId, teamId);
        TeamRole targetRole = requireAssignableRole(request.getRole());

        User targetUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USR_002, "ユーザーが存在しません"));

        if (teamMemberRepository.existsByTeamIdAndUserId(teamId, targetUser.getId())) {
            throw new BusinessException(
                    ErrorCode.TEAM_MEMBER_003,
                    "LOG-TEAM-106",
                    memberFields(teamId, targetUser.getId(), actorMembership.getRole(), targetRole)
            );
        }

        Team team = actorMembership.getTeam();
        TeamMember saved = teamMemberRepository.save(TeamMember.builder()
                .team(team)
                .user(targetUser)
                .role(targetRole)
                .build());

        teamAuditLogService.logMemberAdded(teamId, targetUser.getId(), actorMembership.getRole(), targetRole);
        return toMemberResponse(saved);
    }

    /**
     * OWNER 権限でADMIN / MEMBER間のロール変更を行う。
     */
    @Transactional
    public TeamMemberResponse updateMemberRole(Long teamId, Long memberId, UpdateTeamMemberRoleRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        TeamMember actorMembership = taskAuthorizationService.authorizeTeamRoleChange(currentUserId, teamId);
        TeamRole targetRole = requireAssignableRole(request.getRole());
        TeamMember targetMember = getMemberInTeam(teamId, memberId);

        if (targetMember.getRole() == TeamRole.OWNER) {
            throw new BusinessException(
                    ErrorCode.TEAM_MEMBER_006,
                    "LOG-TEAM-107",
                    constrainedMemberFields(
                            teamId,
                            targetMember.getUser().getId(),
                            actorMembership.getRole(),
                            targetMember.getRole(),
                            "OWNER_ROLE_CHANGE_PROHIBITED"
                    )
            );
        }

        TeamRole previousRole = targetMember.getRole();
        targetMember.setRole(targetRole);
        TeamMember saved = teamMemberRepository.save(targetMember);
        teamAuditLogService.logMemberRoleUpdated(
                teamId,
                targetMember.getUser().getId(),
                actorMembership.getRole(),
                previousRole,
                targetRole
        );
        return toMemberResponse(saved);
    }

    /**
     * OWNER / ADMIN 権限でチームメンバーを削除する。
     */
    @Transactional
    public void removeMember(Long teamId, Long memberId) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        TeamMember actorMembership = taskAuthorizationService.authorizeTeamMemberRemove(currentUserId, teamId);
        TeamMember targetMember = getMemberInTeam(teamId, memberId);

        if (targetMember.getRole() == TeamRole.OWNER) {
            throw new BusinessException(
                    ErrorCode.TEAM_MEMBER_009,
                    "LOG-TEAM-107",
                    constrainedMemberFields(
                            teamId,
                            targetMember.getUser().getId(),
                            actorMembership.getRole(),
                            targetMember.getRole(),
                            "OWNER_DELETE_PROHIBITED"
                    )
            );
        }

        Long targetUserId = targetMember.getUser().getId();
        long assignedTaskCount = taskRepository.countActiveAssignmentsByTeamIdAndUserId(teamId, targetUserId);
        if (assignedTaskCount > 0) {
            throw new BusinessException(
                    ErrorCode.TEAM_MEMBER_010,
                    "LOG-TEAM-108",
                    constrainedMemberFields(
                            teamId,
                            targetUserId,
                            actorMembership.getRole(),
                            targetMember.getRole(),
                            "ASSIGNED_TASK_EXISTS"
                    )
            );
        }

        teamMemberRepository.delete(targetMember);
        teamAuditLogService.logMemberRemoved(teamId, targetUserId, actorMembership.getRole());
    }

    private TeamMember getMemberInTeam(Long teamId, Long memberId) {
        TeamMember member = teamMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TEAM_MEMBER_007));

        Team team = member.getTeam();
        if (team == null || !teamId.equals(team.getId())) {
            throw new ResourceNotFoundException(ErrorCode.TEAM_MEMBER_007);
        }
        return member;
    }

    private TeamRole requireAssignableRole(TeamRole role) {
        if (role != TeamRole.ADMIN && role != TeamRole.MEMBER) {
            throw new BusinessException(ErrorCode.TEAM_MEMBER_004);
        }
        return role;
    }

    private TeamMemberResponse toMemberResponse(TeamMember member) {
        User user = member.getUser();
        return TeamMemberResponse.builder()
                .memberId(member.getId())
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(member.getRole())
                .joinedAt(member.getJoinedAt())
                .build();
    }

    private Map<String, Object> memberFields(Long teamId, Long memberUserId, TeamRole actorRole, TeamRole targetRole) {
        LinkedHashMap<String, Object> fields = new LinkedHashMap<>();
        fields.put("teamId", teamId);
        fields.put("memberUserId", memberUserId);
        fields.put("actorRole", actorRole);
        fields.put("targetRole", targetRole);
        return fields;
    }

    private Map<String, Object> constrainedMemberFields(
            Long teamId,
            Long memberUserId,
            TeamRole actorRole,
            TeamRole targetRole,
            String constraintType
    ) {
        LinkedHashMap<String, Object> fields = new LinkedHashMap<>(memberFields(teamId, memberUserId, actorRole, targetRole));
        fields.put("constraintType", constraintType);
        return fields;
    }
}
