package com.example.task;

import com.example.task.entity.ActivityEventType;
import com.example.task.entity.ActivityTargetType;
import com.example.task.entity.Priority;
import com.example.task.entity.Task;
import com.example.task.entity.TaskStatus;
import com.example.task.entity.Team;
import com.example.task.entity.TeamMember;
import com.example.task.entity.TeamRole;
import com.example.task.entity.User;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Map;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * チーム所属前提になったタスク系 API の認可と関連機能への影響を検証する。
 */
class TeamTaskAuthorizationApiIntegrationTests extends ApiIntegrationTestBase {

    @Test
    @DisplayName("TSK-L-11 TTEAM-X-02: /api/tasks は所属全チームのタスクのみ返す")
    void crossTeamTaskListReturnsAccessibleTasksOnly() throws Exception {
        User requester = createUser("Requester", "requester@example.com", "password123");
        User otherOwner = createUser("Other Owner", "other-owner@example.com", "password123");
        User hiddenOwner = createUser("Hidden Owner", "hidden-owner@example.com", "password123");
        User hiddenAssignee = createUser("Hidden Assignee", "hidden-assignee@example.com", "password123");
        Team ownTeam = createTeamWithMember(requester, "Alpha Team", TeamRole.OWNER);
        Team joinedTeam = createTeamWithMember(otherOwner, "Beta Team", TeamRole.OWNER);
        Team hiddenTeam = createTeamWithMember(hiddenOwner, "Hidden Team", TeamRole.OWNER);
        addTeamMember(joinedTeam, requester, TeamRole.MEMBER);
        addTeamMember(hiddenTeam, hiddenAssignee, TeamRole.MEMBER);
        createTask("Own team task", requester, null, ownTeam, TaskStatus.TODO, Priority.HIGH);
        createTask("Joined team task", otherOwner, requester, joinedTeam, TaskStatus.DOING, Priority.MEDIUM);
        createTask("Hidden team task", hiddenOwner, hiddenAssignee, hiddenTeam, TaskStatus.DONE, Priority.LOW);
        String token = loginAndGetToken(requester.getEmail(), "password123");

        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].title", hasItems("Own team task", "Joined team task")))
                .andExpect(jsonPath("$[*].teamName", hasItems("Alpha Team", "Beta Team")));
    }

    @Test
    @DisplayName("TSK-L-14 TTEAM-L-02: /api/tasks?teamId= は指定teamのタスクのみ返す")
    void teamTaskListReturnsOnlySpecifiedTeamTasks() throws Exception {
        User requester = createUser("Requester", "requester@example.com", "password123");
        Team alpha = createTeamWithMember(requester, "Alpha Team", TeamRole.OWNER);
        Team beta = createTeamWithMember(requester, "Beta Team", TeamRole.OWNER);
        createTask("Alpha task", requester, null, alpha, TaskStatus.TODO, Priority.HIGH);
        createTask("Beta task", requester, null, beta, TaskStatus.TODO, Priority.HIGH);
        String token = loginAndGetToken(requester.getEmail(), "password123");

        mockMvc.perform(get("/api/tasks")
                        .queryParam("teamId", alpha.getId().toString())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Alpha task"))
                .andExpect(jsonPath("$[0].teamId").value(alpha.getId()));
    }

    @Test
    @DisplayName("TTEAM-L-04 AUTH-TEAM-07: 非所属teamId指定は ERR-TASK-009 を返す")
    void nonMemberCannotListTasksOfTeam() throws Exception {
        TeamTaskContext context = newTeamTaskContext();

        mockMvc.perform(get("/api/tasks")
                        .queryParam("teamId", context.team().getId().toString())
                        .header("Authorization", bearer(context.outsiderToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-009"));
    }

    @Test
    @DisplayName("TSK-C-10: teamIdなし作成は ERR-TASK-008 を返す")
    void createTaskRejectsMissingTeamId() throws Exception {
        User creator = createUser("Creator", "creator@example.com", "password123");
        String token = loginAndGetToken(creator.getEmail(), "password123");

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(taskPayload(null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-008"))
                .andExpect(jsonPath("$.details[*].field", hasItems("teamId")));
    }

    @Test
    @DisplayName("TTEAM-C-01: team文脈でタスク作成できる")
    void createTaskInTeamContextSucceeds() throws Exception {
        TeamTaskContext context = newTeamTaskContext();

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", bearer(context.ownerToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(taskPayload(context.team().getId(), context.member().getId()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Team task"))
                .andExpect(jsonPath("$.teamId").value(context.team().getId()))
                .andExpect(jsonPath("$.teamName").value(context.team().getName()))
                .andExpect(jsonPath("$.assignedUser.id").value(context.member().getId()));
    }

    @Test
    @DisplayName("TSK-C-13 TTEAM-C-06: 非所属team作成は ERR-TASK-009 を返す")
    void createTaskRejectsNonMemberTeamWith403() throws Exception {
        TeamTaskContext context = newTeamTaskContext();

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", bearer(context.outsiderToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(taskPayload(context.team().getId(), null))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-009"));
    }

    @Test
    @DisplayName("TSK-C-14 TTEAM-C-07: 未存在team作成は404を返す")
    void createTaskRejectsMissingTeamWith404() throws Exception {
        TeamTaskContext context = newTeamTaskContext();

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", bearer(context.ownerToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(taskPayload(999999L, null))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ERR-TEAM-004"));
    }

    @Test
    @DisplayName("TTEAM-C-08 AUTH-TEAM-19: 非所属teamと未存在teamのエラーを分離する")
    void createTaskDistinguishesForbiddenAndMissingTeam() throws Exception {
        TeamTaskContext context = newTeamTaskContext();

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", bearer(context.outsiderToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(taskPayload(context.team().getId(), null))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-009"));

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", bearer(context.ownerToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(taskPayload(999999L, null))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ERR-TEAM-004"));
    }

    @Test
    @DisplayName("TSK-C-15 TTEAM-C-09: チーム外担当者指定は ERR-TASK-010 を返す")
    void createTaskRejectsAssigneeOutsideTeam() throws Exception {
        TeamTaskContext context = newTeamTaskContext();

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", bearer(context.ownerToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(taskPayload(context.team().getId(), context.outsider().getId()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-010"));
    }

    @Test
    @DisplayName("TSK-G-08 AUTH-TEAM-09: タスク詳細は現在のteam所属者のみ参照できる")
    void taskDetailAccessRequiresCurrentTeamMembership() throws Exception {
        TeamTaskContext context = newTeamTaskContext();

        mockMvc.perform(get("/api/tasks/{taskId}", context.task().getId())
                        .header("Authorization", bearer(context.memberToken())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tasks/{taskId}", context.task().getId())
                        .header("Authorization", bearer(context.outsiderToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-009"));
    }

    @Test
    @DisplayName("TSK-G-09 TTEAM-O-01: 作成者でもteam非所属なら参照できない")
    void taskCreatorCannotViewTaskAfterLosingTeamMembership() throws Exception {
        TeamTaskContext context = newTeamTaskContext();
        removeMembership(context.team(), context.owner());

        mockMvc.perform(get("/api/tasks/{taskId}", context.task().getId())
                        .header("Authorization", bearer(context.ownerToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-009"));
    }

    @Test
    @DisplayName("TSK-U-05: タスク更新でteamは変更されない")
    void taskUpdateDoesNotChangeTeam() throws Exception {
        TeamTaskContext context = newTeamTaskContext();
        Team otherTeam = createTeamWithMember(context.owner(), "Other Team", TeamRole.OWNER);

        mockMvc.perform(put("/api/tasks/{taskId}", context.task().getId())
                        .header("Authorization", bearer(context.ownerToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(taskUpdatePayload("Updated task", context.task().getVersion(), null, otherTeam.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated task"))
                .andExpect(jsonPath("$.teamId").value(context.team().getId()))
                .andExpect(jsonPath("$.teamName").value(context.team().getName()));

        assertEquals(context.team().getId(), taskRepository.findById(context.task().getId()).orElseThrow().getTeam().getId());
    }

    @Test
    @DisplayName("TSK-U-07 TTEAM-U-04: 更新時のチーム外担当者指定は ERR-TASK-010 を返す")
    void updateTaskRejectsAssigneeOutsideTeam() throws Exception {
        TeamTaskContext context = newTeamTaskContext();

        mockMvc.perform(put("/api/tasks/{taskId}", context.task().getId())
                        .header("Authorization", bearer(context.ownerToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(taskUpdatePayload("Updated task", context.task().getVersion(), context.outsider().getId(), null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-010"));
    }

    @Test
    @DisplayName("TTEAM-U-05 AUTH-TEAM-10: 作成者/担当者/OWNER/ADMINの更新権限が維持される")
    void taskUpdateAuthorityRemainsWithinTeam() throws Exception {
        TeamTaskContext context = newTeamTaskContext();

        updateTask(context.ownerToken(), context.task().getId(), "Owner update", context.task().getVersion(), context.member().getId())
                .andExpect(status().isOk());
        long adminVersion = getTaskVersion(context.adminToken(), context.task().getId());
        updateTask(context.adminToken(), context.task().getId(), "Admin update", adminVersion, context.member().getId())
                .andExpect(status().isOk());
        long assigneeVersion = getTaskVersion(context.memberToken(), context.task().getId());
        updateTask(context.memberToken(), context.task().getId(), "Assignee update", assigneeVersion, context.member().getId())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TSK-U-08 TTEAM-O-02: 作成者でもteam非所属なら更新できない")
    void taskCreatorCannotUpdateTaskAfterLosingTeamMembership() throws Exception {
        TeamTaskContext context = newTeamTaskContext();
        removeMembership(context.team(), context.owner());

        mockMvc.perform(put("/api/tasks/{taskId}", context.task().getId())
                        .header("Authorization", bearer(context.ownerToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(taskUpdatePayload("Blocked update", context.task().getVersion(), null, null))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-009"));
    }

    @Test
    @DisplayName("TSK-D-11 AUTH-TEAM-11: 非所属タスク削除は ERR-TASK-009 を返す")
    void taskDeleteAccessRequiresCurrentTeamMembership() throws Exception {
        TeamTaskContext context = newTeamTaskContext();

        mockMvc.perform(delete("/api/tasks/{taskId}", context.task().getId())
                        .header("Authorization", bearer(context.outsiderToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-009"));
    }

    @Test
    @DisplayName("TSK-D-12 TTEAM-O-03: 作成者でもteam非所属なら削除できない")
    void taskCreatorCannotDeleteTaskAfterLosingTeamMembership() throws Exception {
        TeamTaskContext context = newTeamTaskContext();
        removeMembership(context.team(), context.owner());

        mockMvc.perform(delete("/api/tasks/{taskId}", context.task().getId())
                        .header("Authorization", bearer(context.ownerToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-009"));
    }

    @Test
    @DisplayName("USR-06: 担当者候補は対象team所属メンバーに限定される")
    void taskAssigneeCandidatesExcludeUsersOutsideTeam() throws Exception {
        TeamTaskContext context = newTeamTaskContext();

        mockMvc.perform(get("/api/teams/{teamId}/members", context.team().getId())
                        .header("Authorization", bearer(context.ownerToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].userId", hasItems(
                        context.owner().getId().intValue(),
                        context.admin().getId().intValue(),
                        context.member().getId().intValue()
                )));

        JsonNode members = objectMapper.readTree(mockMvc.perform(get("/api/teams/{teamId}/members", context.team().getId())
                        .header("Authorization", bearer(context.ownerToken())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
        for (JsonNode member : members) {
            assertFalse(member.path("userId").asLong() == context.outsider().getId());
        }
    }

    @Test
    @DisplayName("CMT-L-09 AUTH-TEAM-16: 非所属teamタスクのコメント一覧は取得できない")
    void nonMemberCannotListCommentsOfOtherTeamTask() throws Exception {
        TeamTaskContext context = newTeamTaskContext();
        postComment(context.ownerToken(), context.task().getId(), "team comment");

        mockMvc.perform(get("/api/tasks/{taskId}/comments", context.task().getId())
                        .header("Authorization", bearer(context.outsiderToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-009"));
    }

    @Test
    @DisplayName("CMT-C-11: 非所属teamタスクにはコメント投稿できない")
    void nonMemberCannotCreateCommentOnOtherTeamTask() throws Exception {
        TeamTaskContext context = newTeamTaskContext();

        mockMvc.perform(post("/api/tasks/{taskId}/comments", context.task().getId())
                        .header("Authorization", bearer(context.outsiderToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of("content", "blocked"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-009"));
    }

    @Test
    @DisplayName("CMT-U-10 CMT-D-09: 非所属teamタスクのコメント更新/削除はできない")
    void nonMemberCannotOperateCommentsOfOtherTeamTask() throws Exception {
        TeamTaskContext context = newTeamTaskContext();
        JsonNode comment = postComment(context.ownerToken(), context.task().getId(), "team comment");

        mockMvc.perform(put("/api/comments/{commentId}", comment.path("id").asLong())
                        .header("Authorization", bearer(context.outsiderToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of(
                                "content", "blocked",
                                "version", comment.path("version").asLong()
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-009"));

        mockMvc.perform(delete("/api/comments/{commentId}", comment.path("id").asLong())
                        .header("Authorization", bearer(context.outsiderToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-009"));
    }

    @Test
    @DisplayName("ATT-L-06 AUTH-TEAM-17: 非所属teamタスクの添付一覧は取得できない")
    void nonMemberCannotListAttachmentsOfOtherTeamTask() throws Exception {
        TeamTaskContext context = newTeamTaskContext();
        uploadTextAttachment(context.ownerToken(), context.task().getId(), "evidence.txt", "hello");

        mockMvc.perform(get("/api/tasks/{taskId}/attachments", context.task().getId())
                        .header("Authorization", bearer(context.outsiderToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-009"));
    }

    @Test
    @DisplayName("ATT-C-16: 非所属teamタスクには添付アップロードできない")
    void nonMemberCannotUploadAttachmentToOtherTeamTask() throws Exception {
        TeamTaskContext context = newTeamTaskContext();

        mockMvc.perform(multipart("/api/tasks/{taskId}/attachments", context.task().getId())
                        .file(new MockMultipartFile("file", "blocked.txt", "text/plain", "blocked".getBytes()))
                        .header("Authorization", bearer(context.outsiderToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-009"));
    }

    @Test
    @DisplayName("ATT-G-06 ATT-D-08: 非所属teamタスクの添付ダウンロード/削除はできない")
    void nonMemberCannotOperateAttachmentsOfOtherTeamTask() throws Exception {
        TeamTaskContext context = newTeamTaskContext();
        JsonNode attachment = uploadTextAttachment(context.ownerToken(), context.task().getId(), "evidence.txt", "hello");

        mockMvc.perform(get("/api/attachments/{attachmentId}/download", attachment.path("id").asLong())
                        .header("Authorization", bearer(context.outsiderToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-009"));

        mockMvc.perform(delete("/api/attachments/{attachmentId}", attachment.path("id").asLong())
                        .header("Authorization", bearer(context.outsiderToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-009"));
    }

    @Test
    @DisplayName("NTF-N-07 TTEAM-N-05: 権限喪失後は通知関連タスクへアクセスできない")
    void notificationTaskAccessReturns403WhenMembershipLost() throws Exception {
        TeamTaskContext context = newTeamTaskContext();
        createNotification(
                context.member(),
                createActivity(context.owner(), context.task(), ActivityEventType.TASK_UPDATED, ActivityTargetType.TASK, context.task().getId()),
                false
        );

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", bearer(context.memberToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].relatedTaskId").value(context.task().getId()));

        removeMembership(context.team(), context.member());

        mockMvc.perform(get("/api/tasks/{taskId}", context.task().getId())
                        .header("Authorization", bearer(context.memberToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-009"));
    }

    @Test
    @DisplayName("AUTH-TEAM-19: task系APIは403と404を分離する")
    void taskApisDistinguishForbiddenAndNotFound() throws Exception {
        TeamTaskContext context = newTeamTaskContext();

        mockMvc.perform(get("/api/tasks/{taskId}", context.task().getId())
                        .header("Authorization", bearer(context.outsiderToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-009"));

        mockMvc.perform(get("/api/tasks/{taskId}", 999999L)
                        .header("Authorization", bearer(context.ownerToken())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-004"));
    }

    private TeamTaskContext newTeamTaskContext() throws Exception {
        User owner = createUser("Owner", "owner@example.com", "password123");
        User admin = createUser("Admin", "admin@example.com", "password123");
        User member = createUser("Member", "member@example.com", "password123");
        User outsider = createUser("Outsider", "outsider@example.com", "password123");
        Team team = createTeamWithMember(owner, "Product Team", TeamRole.OWNER);
        TeamMember adminMembership = addTeamMember(team, admin, TeamRole.ADMIN);
        TeamMember memberMembership = addTeamMember(team, member, TeamRole.MEMBER);
        Task task = createTask("Initial team task", owner, member, team, TaskStatus.TODO, Priority.HIGH);
        String ownerToken = loginAndGetToken(owner.getEmail(), "password123");
        String adminToken = loginAndGetToken(admin.getEmail(), "password123");
        String memberToken = loginAndGetToken(member.getEmail(), "password123");
        String outsiderToken = loginAndGetToken(outsider.getEmail(), "password123");
        return new TeamTaskContext(
                owner,
                admin,
                member,
                outsider,
                team,
                adminMembership,
                memberMembership,
                task,
                ownerToken,
                adminToken,
                memberToken,
                outsiderToken
        );
    }

    private Map<String, Object> taskPayload(Long teamId, Long assignedUserId) {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("title", "Team task");
        payload.put("description", "Created in team context");
        payload.put("status", "TODO");
        payload.put("priority", "HIGH");
        payload.put("dueDate", "2026-04-25");
        if (teamId != null) {
            payload.put("teamId", teamId);
        }
        if (assignedUserId != null) {
            payload.put("assignedUserId", assignedUserId);
        }
        return payload;
    }

    private Map<String, Object> taskUpdatePayload(String title, Long version, Long assignedUserId, Long teamId) {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("title", title);
        payload.put("description", title + " description");
        payload.put("status", "DOING");
        payload.put("priority", "MEDIUM");
        payload.put("dueDate", "2026-04-26");
        payload.put("version", version);
        if (assignedUserId != null) {
            payload.put("assignedUserId", assignedUserId);
        }
        if (teamId != null) {
            payload.put("teamId", teamId);
        }
        return payload;
    }

    private org.springframework.test.web.servlet.ResultActions updateTask(
            String token,
            Long taskId,
            String title,
            Long version,
            Long assignedUserId
    ) throws Exception {
        return mockMvc.perform(put("/api/tasks/{taskId}", taskId)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJson(taskUpdatePayload(title, version, assignedUserId, null))));
    }

    private void removeMembership(Team team, User user) {
        teamMemberRepository.findByTeamIdAndUserId(team.getId(), user.getId())
                .ifPresent(teamMemberRepository::delete);
        teamMemberRepository.flush();
    }

    private record TeamTaskContext(
            User owner,
            User admin,
            User member,
            User outsider,
            Team team,
            TeamMember adminMembership,
            TeamMember memberMembership,
            Task task,
            String ownerToken,
            String adminToken,
            String memberToken,
            String outsiderToken
    ) {
    }
}
