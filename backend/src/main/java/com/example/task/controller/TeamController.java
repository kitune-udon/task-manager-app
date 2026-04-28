package com.example.task.controller;

import com.example.task.dto.team.CreateTeamRequest;
import com.example.task.dto.team.TeamDetailResponse;
import com.example.task.dto.team.TeamSummaryResponse;
import com.example.task.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * チーム作成、所属チーム一覧、チーム詳細を公開する API。
 */
@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    /**
     * チームを作成し、作成者をOWNERとして所属させる。
     */
    @PostMapping
    public ResponseEntity<TeamDetailResponse> createTeam(@Valid @RequestBody CreateTeamRequest request) {
        TeamDetailResponse response = teamService.createTeam(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * ログインユーザーの所属チーム一覧を返す。
     */
    @GetMapping
    public ResponseEntity<List<TeamSummaryResponse>> getTeams() {
        return ResponseEntity.ok(teamService.getMyTeams());
    }

    /**
     * 所属しているチームの詳細を返す。
     */
    @GetMapping("/{teamId}")
    public ResponseEntity<TeamDetailResponse> getTeam(@PathVariable Long teamId) {
        return ResponseEntity.ok(teamService.getTeamDetail(teamId));
    }
}
