package com.example.task.controller;

import com.example.task.dto.team.AddTeamMemberRequest;
import com.example.task.dto.team.TeamMemberResponse;
import com.example.task.dto.team.UpdateTeamMemberRoleRequest;
import com.example.task.service.TeamMemberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * チームメンバー追加、ロール変更、削除を公開する API。
 */
@RestController
@RequestMapping("/api/teams/{teamId}/members")
public class TeamMemberController {

    private final TeamMemberService teamMemberService;

    public TeamMemberController(TeamMemberService teamMemberService) {
        this.teamMemberService = teamMemberService;
    }

    /**
     * チームへメンバーを追加する。
     */
    @PostMapping
    public ResponseEntity<TeamMemberResponse> addMember(
            @PathVariable Long teamId,
            @Valid @RequestBody AddTeamMemberRequest request
    ) {
        TeamMemberResponse response = teamMemberService.addMember(teamId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * チームメンバーのロールを変更する。
     */
    @PatchMapping("/{memberId}")
    public ResponseEntity<TeamMemberResponse> updateMemberRole(
            @PathVariable Long teamId,
            @PathVariable Long memberId,
            @Valid @RequestBody UpdateTeamMemberRoleRequest request
    ) {
        return ResponseEntity.ok(teamMemberService.updateMemberRole(teamId, memberId, request));
    }

    /**
     * チームメンバーを削除する。
     */
    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long teamId,
            @PathVariable Long memberId
    ) {
        teamMemberService.removeMember(teamId, memberId);
        return ResponseEntity.noContent().build();
    }
}
