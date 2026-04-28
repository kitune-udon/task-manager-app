package com.example.task.controller;

import com.example.task.dto.team.AvailableUserResponse;
import com.example.task.dto.team.TeamMemberResponse;
import com.example.task.service.TeamMemberQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * チームメンバー一覧と追加候補ユーザー一覧を公開する API。
 */
@RestController
@RequestMapping("/api/teams/{teamId}")
public class TeamMemberQueryController {

    private final TeamMemberQueryService teamMemberQueryService;

    public TeamMemberQueryController(TeamMemberQueryService teamMemberQueryService) {
        this.teamMemberQueryService = teamMemberQueryService;
    }

    /**
     * チーム所属メンバー一覧を返す。
     */
    @GetMapping("/members")
    public ResponseEntity<List<TeamMemberResponse>> getMembers(@PathVariable Long teamId) {
        return ResponseEntity.ok(teamMemberQueryService.getMembers(teamId));
    }

    /**
     * チームへ追加可能なユーザー一覧を返す。
     */
    @GetMapping("/available-users")
    public ResponseEntity<List<AvailableUserResponse>> getAvailableUsers(@PathVariable Long teamId) {
        return ResponseEntity.ok(teamMemberQueryService.getAvailableUsers(teamId));
    }
}
