package com.example.task.service;

import com.example.task.dto.team.AvailableUserResponse;
import com.example.task.dto.team.TeamMemberResponse;
import com.example.task.entity.TeamMember;
import com.example.task.entity.User;
import com.example.task.repository.TeamMemberRepository;
import com.example.task.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * チームメンバー一覧と追加候補ユーザー一覧を扱うサービス。
 */
@Service
public class TeamMemberQueryService {

    private final TeamMemberRepository teamMemberRepository;
    private final CurrentUserProvider currentUserProvider;
    private final TaskAuthorizationService taskAuthorizationService;

    public TeamMemberQueryService(
            TeamMemberRepository teamMemberRepository,
            CurrentUserProvider currentUserProvider,
            TaskAuthorizationService taskAuthorizationService
    ) {
        this.teamMemberRepository = teamMemberRepository;
        this.currentUserProvider = currentUserProvider;
        this.taskAuthorizationService = taskAuthorizationService;
    }

    /**
     * チーム所属メンバー一覧を返す。
     */
    @Transactional(readOnly = true)
    public List<TeamMemberResponse> getMembers(Long teamId) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        taskAuthorizationService.authorizeTeamView(currentUserId, teamId);
        return teamMemberRepository.findByTeamId(teamId)
                .stream()
                .map(this::toMemberResponse)
                .toList();
    }

    /**
     * チームへ追加可能なユーザー一覧を返す。
     */
    @Transactional(readOnly = true)
    public List<AvailableUserResponse> getAvailableUsers(Long teamId) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        taskAuthorizationService.authorizeTeamMemberAdd(currentUserId, teamId);
        return teamMemberRepository.findAvailableUsers(teamId)
                .stream()
                .map(this::toAvailableUserResponse)
                .toList();
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

    private AvailableUserResponse toAvailableUserResponse(User user) {
        return AvailableUserResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }
}
