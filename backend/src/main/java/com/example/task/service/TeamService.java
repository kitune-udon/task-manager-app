package com.example.task.service;

import com.example.task.dto.team.CreateTeamRequest;
import com.example.task.dto.team.TeamDetailResponse;
import com.example.task.dto.team.TeamSummaryResponse;
import com.example.task.entity.Team;
import com.example.task.entity.TeamMember;
import com.example.task.entity.TeamRole;
import com.example.task.entity.User;
import com.example.task.exception.BusinessException;
import com.example.task.exception.ErrorCode;
import com.example.task.exception.ResourceNotFoundException;
import com.example.task.repository.TeamMemberRepository;
import com.example.task.repository.TeamRepository;
import com.example.task.repository.UserRepository;
import com.example.task.security.CurrentUserProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * チーム作成、所属チーム一覧、チーム詳細を扱うサービス。
 */
@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final TaskAuthorizationService taskAuthorizationService;
    private final TeamAuditLogService teamAuditLogService;

    public TeamService(
            TeamRepository teamRepository,
            TeamMemberRepository teamMemberRepository,
            UserRepository userRepository,
            CurrentUserProvider currentUserProvider,
            TaskAuthorizationService taskAuthorizationService,
            TeamAuditLogService teamAuditLogService
    ) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
        this.taskAuthorizationService = taskAuthorizationService;
        this.teamAuditLogService = teamAuditLogService;
    }

    /**
     * チームを作成し、作成者をOWNERとして登録する。
     */
    @Transactional
    public TeamDetailResponse createTeam(CreateTeamRequest request) {
        User currentUser = resolveCurrentUser();
        String name = request.getName().trim();
        String description = normalizeDescription(request.getDescription());

        Team duplicateTeam = teamRepository.findByCreatedByIdAndName(currentUser.getId(), name).orElse(null);
        if (duplicateTeam != null) {
            throw new BusinessException(
                    ErrorCode.TEAM_002,
                    "LOG-TEAM-105",
                    duplicateTeamFields(duplicateTeam.getId(), name)
            );
        }

        Team team;
        try {
            team = teamRepository.saveAndFlush(Team.builder()
                    .name(name)
                    .description(description)
                    .createdBy(currentUser)
                    .build());

            teamMemberRepository.saveAndFlush(TeamMember.builder()
                    .team(team)
                    .user(currentUser)
                    .role(TeamRole.OWNER)
                    .build());
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(
                    ErrorCode.TEAM_002,
                    "LOG-TEAM-105",
                    duplicateTeamFields(null, name)
            );
        }

        teamAuditLogService.logTeamCreated(team.getId(), team.getName());
        return toDetailResponse(team, TeamRole.OWNER, 1L);
    }

    /**
     * ログインユーザーが所属するチーム一覧を返す。
     */
    @Transactional(readOnly = true)
    public List<TeamSummaryResponse> getMyTeams() {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        return teamMemberRepository.findByUserIdOrderByTeamNameAscTeamIdAsc(currentUserId)
                .stream()
                .map(member -> toSummaryResponse(
                        member.getTeam(),
                        member.getRole(),
                        teamMemberRepository.countByTeamId(member.getTeam().getId())
                ))
                .toList();
    }

    /**
     * 所属しているチームの詳細を返す。
     */
    @Transactional(readOnly = true)
    public TeamDetailResponse getTeamDetail(Long teamId) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        TeamMember membership = taskAuthorizationService.authorizeTeamView(currentUserId, teamId);
        return toDetailResponse(
                membership.getTeam(),
                membership.getRole(),
                teamMemberRepository.countByTeamId(teamId)
        );
    }

    private TeamSummaryResponse toSummaryResponse(Team team, TeamRole myRole, long memberCount) {
        return TeamSummaryResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .description(team.getDescription())
                .myRole(myRole)
                .memberCount(memberCount)
                .updatedAt(team.getUpdatedAt())
                .build();
    }

    private TeamDetailResponse toDetailResponse(Team team, TeamRole myRole, long memberCount) {
        return TeamDetailResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .description(team.getDescription())
                .myRole(myRole)
                .memberCount(memberCount)
                .createdAt(team.getCreatedAt())
                .updatedAt(team.getUpdatedAt())
                .build();
    }

    private User resolveCurrentUser() {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        return userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USR_002, "ユーザーが存在しません"));
    }

    private String normalizeDescription(String description) {
        return description == null ? null : description.trim();
    }

    private Map<String, Object> duplicateTeamFields(Long teamId, String teamName) {
        LinkedHashMap<String, Object> fields = new LinkedHashMap<>();
        fields.put("teamId", teamId);
        fields.put("teamName", teamName);
        return fields;
    }
}
