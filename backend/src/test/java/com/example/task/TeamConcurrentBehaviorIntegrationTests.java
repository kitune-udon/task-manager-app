package com.example.task;

import com.example.task.entity.Priority;
import com.example.task.entity.Task;
import com.example.task.entity.TaskStatus;
import com.example.task.entity.Team;
import com.example.task.entity.TeamMember;
import com.example.task.entity.TeamRole;
import com.example.task.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * チーム管理で競合しやすい連続操作後の最終整合性を検証する。
 */
class TeamConcurrentBehaviorIntegrationTests extends ApiIntegrationTestBase {

    @Test
    @DisplayName("CONC-01: 同一メンバーへの連続ロール変更は最終状態が一貫する")
    void concurrentRoleChangesEndInConsistentFinalState() throws Exception {
        TeamConcurrentContext context = newConcurrentContext();

        updateMemberRole(context.ownerToken(), context.team().getId(), context.memberMembership().getId(), TeamRole.ADMIN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
        updateMemberRole(context.ownerToken(), context.team().getId(), context.memberMembership().getId(), TeamRole.MEMBER)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MEMBER"));

        assertEquals(TeamRole.MEMBER, teamMemberRepository.findById(context.memberMembership().getId()).orElseThrow().getRole());
        assertEquals(1L, countOwners(context.team()));
    }

    @Test
    @DisplayName("CONC-02: ロール変更競合後もOWNER保護ルールが壊れない")
    void concurrentRoleChangesDoNotBreakOwnerProtection() throws Exception {
        TeamConcurrentContext context = newConcurrentContext();

        updateMemberRole(context.ownerToken(), context.team().getId(), context.ownerMembership().getId(), TeamRole.MEMBER)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-TEAM-MEMBER-006"));
        updateMemberRole(context.ownerToken(), context.team().getId(), context.memberMembership().getId(), TeamRole.ADMIN)
                .andExpect(status().isOk());

        assertEquals(TeamRole.OWNER, teamMemberRepository.findById(context.ownerMembership().getId()).orElseThrow().getRole());
        assertEquals(TeamRole.ADMIN, teamMemberRepository.findById(context.memberMembership().getId()).orElseThrow().getRole());
        assertEquals(1L, countOwners(context.team()));
    }

    @Test
    @DisplayName("CONC-03: 同一ユーザーへの重複追加でmembership重複が発生しない")
    void concurrentDuplicateAddsDoNotCreateDuplicateMemberships() throws Exception {
        TeamConcurrentContext context = newConcurrentContext();
        User target = createUser("Target", "target@example.com", "password123");

        addMember(context.ownerToken(), context.team().getId(), target.getId(), TeamRole.MEMBER)
                .andExpect(status().isCreated());
        addMember(context.ownerToken(), context.team().getId(), target.getId(), TeamRole.MEMBER)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ERR-TEAM-MEMBER-003"));

        assertEquals(1L, queryLong("""
                select count(*)
                from %s
                where team_id = ?
                  and user_id = ?
                """.formatted(table("team_members")), context.team().getId(), target.getId()));
    }

    @Test
    @DisplayName("CONC-04: 削除直前後に再追加してもmembership整合が崩れない")
    void removeAndReaddAroundSameUserKeepsMembershipConsistent() throws Exception {
        TeamConcurrentContext context = newConcurrentContext();

        removeMember(context.ownerToken(), context.team().getId(), context.memberMembership().getId())
                .andExpect(status().isNoContent());
        addMember(context.ownerToken(), context.team().getId(), context.member().getId(), TeamRole.ADMIN)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        assertEquals(1L, queryLong("""
                select count(*)
                from %s
                where team_id = ?
                  and user_id = ?
                """.formatted(table("team_members")), context.team().getId(), context.member().getId()));
        assertEquals(TeamRole.ADMIN, teamMemberRepository.findByTeamIdAndUserId(context.team().getId(), context.member().getId())
                .orElseThrow()
                .getRole());
    }

    @Test
    @DisplayName("CONC-05: タスク更新とメンバー削除の前後でteam外担当者が残らない")
    void taskUpdateAndMemberRemovalDoNotLeaveInvalidAssigneeMembership() throws Exception {
        TeamConcurrentContext context = newConcurrentContext();
        Task task = createTask("Assigned task", context.owner(), context.member(), context.team(), TaskStatus.TODO, Priority.HIGH);

        removeMember(context.ownerToken(), context.team().getId(), context.memberMembership().getId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ERR-TEAM-MEMBER-010"));

        updateTaskClearingAssignee(context.ownerToken(), task.getId(), task.getVersion())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedUser").doesNotExist());
        removeMember(context.ownerToken(), context.team().getId(), context.memberMembership().getId())
                .andExpect(status().isNoContent());

        assertEquals(0L, queryLong("""
                select count(*)
                from %s ta
                left join %s tm
                  on tm.team_id = ta.team_id
                 and tm.user_id = ta.assigned_user_id
                where ta.assigned_user_id is not null
                  and ta.deleted_at is null
                  and tm.id is null
                """.formatted(table("tasks"), table("team_members"))));
    }

    private TeamConcurrentContext newConcurrentContext() throws Exception {
        User owner = createUser("Owner", "owner@example.com", "password123");
        User member = createUser("Member", "member@example.com", "password123");
        Team team = createTeamWithMember(owner, "Concurrent Team", TeamRole.OWNER);
        TeamMember ownerMembership = teamMemberRepository.findByTeamIdAndUserId(team.getId(), owner.getId()).orElseThrow();
        TeamMember memberMembership = addTeamMember(team, member, TeamRole.MEMBER);
        String ownerToken = loginAndGetToken(owner.getEmail(), "password123");
        return new TeamConcurrentContext(owner, member, team, ownerMembership, memberMembership, ownerToken);
    }

    private org.springframework.test.web.servlet.ResultActions addMember(
            String token,
            Long teamId,
            Long userId,
            TeamRole role
    ) throws Exception {
        return mockMvc.perform(post("/api/teams/{teamId}/members", teamId)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJson(Map.of(
                        "userId", userId,
                        "role", role.name()
                ))));
    }

    private org.springframework.test.web.servlet.ResultActions updateMemberRole(
            String token,
            Long teamId,
            Long memberId,
            TeamRole role
    ) throws Exception {
        return mockMvc.perform(patch("/api/teams/{teamId}/members/{memberId}", teamId, memberId)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJson(Map.of("role", role.name()))));
    }

    private org.springframework.test.web.servlet.ResultActions removeMember(
            String token,
            Long teamId,
            Long memberId
    ) throws Exception {
        return mockMvc.perform(delete("/api/teams/{teamId}/members/{memberId}", teamId, memberId)
                .header("Authorization", bearer(token)));
    }

    private org.springframework.test.web.servlet.ResultActions updateTaskClearingAssignee(
            String token,
            Long taskId,
            Long version
    ) throws Exception {
        return mockMvc.perform(put("/api/tasks/{taskId}", taskId)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJson(Map.of(
                        "title", "Cleared assignee",
                        "description", "No assignee",
                        "status", "DOING",
                        "priority", "MEDIUM",
                        "dueDate", "2026-04-26",
                        "version", version
                ))));
    }

    private long countOwners(Team team) {
        return queryLong("""
                select count(*)
                from %s
                where team_id = ?
                  and role = 'OWNER'
                """.formatted(table("team_members")), team.getId());
    }

    private long queryLong(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0L : value;
    }

    private record TeamConcurrentContext(
            User owner,
            User member,
            Team team,
            TeamMember ownerMembership,
            TeamMember memberMembership,
            String ownerToken
    ) {
    }
}
