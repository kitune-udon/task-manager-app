package com.example.task;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.task.entity.Priority;
import com.example.task.entity.TaskStatus;
import com.example.task.entity.Team;
import com.example.task.entity.TeamMember;
import com.example.task.entity.TeamRole;
import com.example.task.entity.User;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * チーム管理 API とチーム管理監査ログの統合観点を検証する。
 */
class TeamManagementApiIntegrationTests extends ApiIntegrationTestBase {

    private Logger applicationLogger;
    private Logger auditLogger;
    private ListAppender<ILoggingEvent> applicationAppender;
    private ListAppender<ILoggingEvent> auditAppender;

    @BeforeEach
    void attachLogAppenders() {
        applicationLogger = (Logger) LoggerFactory.getLogger("application");
        auditLogger = (Logger) LoggerFactory.getLogger("audit");
        applicationAppender = createAppender("team-management-application");
        auditAppender = createAppender("team-management-audit");
        applicationLogger.addAppender(applicationAppender);
        auditLogger.addAppender(auditAppender);
    }

    @AfterEach
    void detachLogAppenders() {
        detachAppender(applicationLogger, applicationAppender);
        detachAppender(auditLogger, auditAppender);
    }

    @Test
    @DisplayName("TEAM-C-01: チームを作成できる")
    void createTeamSucceeds() throws Exception {
        User owner = createUser("Owner", "owner@example.com", "password123");
        String token = loginAndGetToken(owner.getEmail(), "password123");

        mockMvc.perform(post("/api/teams")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of(
                                "name", "Product Team",
                                "description", "Product development"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Product Team"))
                .andExpect(jsonPath("$.description").value("Product development"))
                .andExpect(jsonPath("$.myRole").value("OWNER"))
                .andExpect(jsonPath("$.memberCount").value(1));

        assertTrue(teamRepository.existsByCreatedByIdAndName(owner.getId(), "Product Team"));
    }

    @Test
    @DisplayName("TEAM-C-02: チーム作成者はOWNERとして所属する")
    void createTeamAddsCreatorAsOwner() throws Exception {
        User owner = createUser("Owner", "owner@example.com", "password123");
        String token = loginAndGetToken(owner.getEmail(), "password123");

        JsonNode created = createTeam(token, "Owner Team", "owned");
        TeamMember membership = teamMemberRepository.findByTeamIdAndUserId(created.path("id").asLong(), owner.getId())
                .orElseThrow();

        assertEquals(TeamRole.OWNER, membership.getRole());
    }

    @Test
    @DisplayName("TEAM-C-04: チーム名未入力は ERR-TEAM-001 を返す")
    void createTeamValidationFails() throws Exception {
        User owner = createUser("Owner", "owner@example.com", "password123");
        String token = loginAndGetToken(owner.getEmail(), "password123");

        mockMvc.perform(post("/api/teams")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of("name", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ERR-TEAM-001"))
                .andExpect(jsonPath("$.details[*].field", hasItems("name")));
    }

    @Test
    @DisplayName("TEAM-C-05: チーム名が101文字以上の場合は ERR-TEAM-001 を返す")
    void createTeamRejectsNameLongerThan100Chars() throws Exception {
        User owner = createUser("Owner", "owner@example.com", "password123");
        String token = loginAndGetToken(owner.getEmail(), "password123");

        mockMvc.perform(post("/api/teams")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of("name", "a".repeat(101)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ERR-TEAM-001"))
                .andExpect(jsonPath("$.details[*].field", hasItems("name")));
    }

    @Test
    @DisplayName("TEAM-C-06: 説明が1001文字以上の場合は ERR-TEAM-001 を返す")
    void createTeamRejectsDescriptionLongerThan1000Chars() throws Exception {
        User owner = createUser("Owner", "owner@example.com", "password123");
        String token = loginAndGetToken(owner.getEmail(), "password123");

        mockMvc.perform(post("/api/teams")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of(
                                "name", "Long Description Team",
                                "description", "a".repeat(1001)
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ERR-TEAM-001"))
                .andExpect(jsonPath("$.details[*].field", hasItems("description")));
    }

    @Test
    @DisplayName("TEAM-C-07: 同一作成者の同名チームは409を返す")
    void createTeamRejectsDuplicateNameForSameOwner() throws Exception {
        User owner = createUser("Owner", "owner@example.com", "password123");
        String token = loginAndGetToken(owner.getEmail(), "password123");
        createTeam(token, "Duplicate Team", null);

        mockMvc.perform(post("/api/teams")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of("name", "Duplicate Team"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ERR-TEAM-002"));
    }

    @Test
    @DisplayName("TEAM-C-10: 説明未指定でもチーム作成できる")
    void createTeamAllowsEmptyDescription() throws Exception {
        User owner = createUser("Owner", "owner@example.com", "password123");
        String token = loginAndGetToken(owner.getEmail(), "password123");

        mockMvc.perform(post("/api/teams")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of("name", "No Description Team"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").doesNotExist());
    }

    @Test
    @DisplayName("TEAM-L-01 TEAM-L-02: 所属チーム一覧は自分のチームのみ返す")
    void listTeamsReturnsOwnTeamsOnly() throws Exception {
        User requester = createUser("Requester", "requester@example.com", "password123");
        User otherOwner = createUser("Other Owner", "other-owner@example.com", "password123");
        createUser("Outsider", "outsider@example.com", "password123");
        Team ownTeam = createTeamWithMember(requester, "Alpha Team", TeamRole.OWNER);
        Team joinedTeam = createTeamWithMember(otherOwner, "Beta Team", TeamRole.OWNER);
        createTeamWithMember(userRepository.findByEmail("outsider@example.com").orElseThrow(), "Hidden Team", TeamRole.OWNER);
        addTeamMember(joinedTeam, requester, TeamRole.MEMBER);
        String token = loginAndGetToken(requester.getEmail(), "password123");

        mockMvc.perform(get("/api/teams")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", hasItems(ownTeam.getName(), joinedTeam.getName())));
    }

    @Test
    @DisplayName("TEAM-L-07: 所属チーム一覧は name ASC, id ASC の順で返す")
    void listTeamsOrdersByNameAscThenIdAsc() throws Exception {
        User owner = createUser("Owner", "owner@example.com", "password123");
        createTeamWithMember(owner, "Zulu Team", TeamRole.OWNER);
        Team alpha = createTeamWithMember(owner, "Alpha Team", TeamRole.OWNER);
        String token = loginAndGetToken(owner.getEmail(), "password123");

        mockMvc.perform(get("/api/teams")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(alpha.getId()))
                .andExpect(jsonPath("$[0].name").value("Alpha Team"))
                .andExpect(jsonPath("$[1].name").value("Zulu Team"));
    }

    @Test
    @DisplayName("TEAM-G-01: チーム詳細に基本情報を返す")
    void getTeamDetailReturnsBasicInfo() throws Exception {
        TeamContext context = newTeamContext();

        mockMvc.perform(get("/api/teams/{teamId}", context.team().getId())
                        .header("Authorization", bearer(context.ownerToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(context.team().getId()))
                .andExpect(jsonPath("$.name").value("Product Team"))
                .andExpect(jsonPath("$.description").value("Product work"))
                .andExpect(jsonPath("$.myRole").value("OWNER"))
                .andExpect(jsonPath("$.memberCount").value(3));
    }

    @Test
    @DisplayName("AUTH-TEAM-01: チーム詳細は所属者のみ参照できる")
    void teamDetailAccessibleForMembersOnly() throws Exception {
        TeamContext context = newTeamContext();

        mockMvc.perform(get("/api/teams/{teamId}", context.team().getId())
                        .header("Authorization", bearer(context.memberToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myRole").value("MEMBER"));

        mockMvc.perform(get("/api/teams/{teamId}", context.team().getId())
                        .header("Authorization", bearer(context.outsiderToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-TEAM-003"));
    }

    @Test
    @DisplayName("TEAM-G-03 TEAM-U-01: メンバー一覧をロール優先順で取得できる")
    void getTeamMembersReturnsMembers() throws Exception {
        TeamContext context = newTeamContext();

        mockMvc.perform(get("/api/teams/{teamId}/members", context.team().getId())
                        .header("Authorization", bearer(context.ownerToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].userId").value(context.owner().getId()))
                .andExpect(jsonPath("$[0].role").value("OWNER"))
                .andExpect(jsonPath("$[1].userId").value(context.admin().getId()))
                .andExpect(jsonPath("$[1].role").value("ADMIN"))
                .andExpect(jsonPath("$[2].userId").value(context.member().getId()))
                .andExpect(jsonPath("$[2].role").value("MEMBER"));
    }

    @Test
    @DisplayName("AUTH-TEAM-02: メンバー一覧は所属者のみ参照できる")
    void teamMembersAccessibleForMembersOnly() throws Exception {
        TeamContext context = newTeamContext();

        mockMvc.perform(get("/api/teams/{teamId}/members", context.team().getId())
                        .header("Authorization", bearer(context.memberToken())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/teams/{teamId}/members", context.team().getId())
                        .header("Authorization", bearer(context.outsiderToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-TEAM-003"));
    }

    @Test
    @DisplayName("TEAM-U-02: 追加候補ユーザーは未所属ユーザーのみ返す")
    void getAvailableUsersReturnsOnlyNonMembers() throws Exception {
        TeamContext context = newTeamContext();

        mockMvc.perform(get("/api/teams/{teamId}/available-users", context.team().getId())
                        .header("Authorization", bearer(context.ownerToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userId").value(context.outsider().getId()))
                .andExpect(jsonPath("$[0].email").value(context.outsider().getEmail()));
    }

    @Test
    @DisplayName("AUTH-TEAM-03: 追加候補ユーザー一覧はOWNER/ADMINのみ参照できる")
    void availableUsersAccessibleForOwnerAndAdminOnly() throws Exception {
        TeamContext context = newTeamContext();

        mockMvc.perform(get("/api/teams/{teamId}/available-users", context.team().getId())
                        .header("Authorization", bearer(context.ownerToken())))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/teams/{teamId}/available-users", context.team().getId())
                        .header("Authorization", bearer(context.adminToken())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/teams/{teamId}/available-users", context.team().getId())
                        .header("Authorization", bearer(context.memberToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-TEAM-MEMBER-002"));
    }

    @Test
    @DisplayName("AUTH-TEAM-04: OWNER/ADMIN はメンバー追加でき、MEMBER は追加できない")
    void addMemberAllowedForOwnerAndAdminOnly() throws Exception {
        TeamContext context = newTeamContext();
        User ownerAdded = createUser("Owner Added", "owner-added@example.com", "password123");
        User adminAdded = createUser("Admin Added", "admin-added@example.com", "password123");
        User memberAdded = createUser("Member Added", "member-added@example.com", "password123");

        addMember(context.ownerToken(), context.team().getId(), ownerAdded.getId(), TeamRole.MEMBER)
                .andExpect(status().isCreated());
        addMember(context.adminToken(), context.team().getId(), adminAdded.getId(), TeamRole.MEMBER)
                .andExpect(status().isCreated());
        addMember(context.memberToken(), context.team().getId(), memberAdded.getId(), TeamRole.MEMBER)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-TEAM-MEMBER-002"));
    }

    @Test
    @DisplayName("TEAM-M-05: 既存メンバー追加は409を返す")
    void addMemberRejectsDuplicateMembership() throws Exception {
        TeamContext context = newTeamContext();

        addMember(context.ownerToken(), context.team().getId(), context.member().getId(), TeamRole.MEMBER)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ERR-TEAM-MEMBER-003"));
    }

    @Test
    @DisplayName("AUTH-TEAM-05: ロール変更はOWNERのみ実行できる")
    void changeRoleAllowedForOwnerOnly() throws Exception {
        TeamContext context = newTeamContext();

        updateMemberRole(context.ownerToken(), context.team().getId(), context.memberMembership().getId(), TeamRole.ADMIN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        updateMemberRole(context.adminToken(), context.team().getId(), context.memberMembership().getId(), TeamRole.MEMBER)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-TEAM-MEMBER-005"));
    }

    @Test
    @DisplayName("TEAM-M-10: OWNERのロールは変更できない")
    void changeRoleRejectsOwnerMember() throws Exception {
        TeamContext context = newTeamContext();

        updateMemberRole(context.ownerToken(), context.team().getId(), context.ownerMembership().getId(), TeamRole.MEMBER)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-TEAM-MEMBER-006"));
    }

    @Test
    @DisplayName("AUTH-TEAM-06: OWNER/ADMIN はメンバー削除でき、MEMBER は削除できない")
    void removeMemberAllowedForOwnerAndAdminOnly() throws Exception {
        TeamContext context = newTeamContext();
        User removableByOwner = createUser("Owner Remove", "owner-remove@example.com", "password123");
        User removableByAdmin = createUser("Admin Remove", "admin-remove@example.com", "password123");
        User blocked = createUser("Blocked Remove", "blocked-remove@example.com", "password123");
        TeamMember ownerTarget = addTeamMember(context.team(), removableByOwner, TeamRole.MEMBER);
        TeamMember adminTarget = addTeamMember(context.team(), removableByAdmin, TeamRole.MEMBER);
        TeamMember blockedTarget = addTeamMember(context.team(), blocked, TeamRole.MEMBER);

        removeMember(context.ownerToken(), context.team().getId(), ownerTarget.getId())
                .andExpect(status().isNoContent());
        removeMember(context.adminToken(), context.team().getId(), adminTarget.getId())
                .andExpect(status().isNoContent());
        removeMember(context.memberToken(), context.team().getId(), blockedTarget.getId())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-TEAM-MEMBER-008"));
    }

    @Test
    @DisplayName("TEAM-M-13: OWNERは削除できない")
    void ownerDeletionProhibited() throws Exception {
        TeamContext context = newTeamContext();

        removeMember(context.ownerToken(), context.team().getId(), context.ownerMembership().getId())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-TEAM-MEMBER-009"));
    }

    @Test
    @DisplayName("TEAM-M-15: 担当中タスクがあるメンバーは削除できない")
    void memberWithActiveAssignedTaskCannotBeRemoved() throws Exception {
        TeamContext context = newTeamContext();
        createTask("Assigned done task", context.owner(), context.member(), context.team(), TaskStatus.DONE, Priority.HIGH);
        assertEquals(1L, taskRepository.countActiveAssignmentsByTeamIdAndUserId(context.team().getId(), context.member().getId()));

        removeMember(context.ownerToken(), context.team().getId(), context.memberMembership().getId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ERR-TEAM-MEMBER-010"));
    }

    @Test
    @DisplayName("AUTH-TEAM-12: ADMIN は自分自身をチームから削除できる")
    void adminCanRemoveSelfFromTeam() throws Exception {
        TeamContext context = newTeamContext();

        removeMember(context.adminToken(), context.team().getId(), context.adminMembership().getId())
                .andExpect(status().isNoContent());

        assertFalse(teamMemberRepository.existsByTeamIdAndUserId(context.team().getId(), context.admin().getId()));
    }

    @Test
    @DisplayName("AUTH-TEAM-19: team系APIは403と404を分離する")
    void teamApisDistinguishForbiddenAndNotFound() throws Exception {
        TeamContext context = newTeamContext();

        mockMvc.perform(get("/api/teams/{teamId}", context.team().getId())
                        .header("Authorization", bearer(context.outsiderToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-TEAM-003"));

        mockMvc.perform(get("/api/teams/{teamId}", 999999L)
                        .header("Authorization", bearer(context.ownerToken())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ERR-TEAM-004"));
    }

    @Test
    @DisplayName("LOG-TEAM-01: チーム作成成功時に LOG-TEAM-001 を出力する")
    void createTeamWritesLogTeam001() throws Exception {
        User owner = createUser("Owner", "owner@example.com", "password123");
        String token = loginAndGetToken(owner.getEmail(), "password123");
        clearCapturedLogs();

        JsonNode created = createTeam(token, "Logged Team", null);

        CapturedLog event = findRequiredEvent(auditAppender, "LOG-TEAM-001");
        assertEquals("audit", event.loggerName());
        assertEquals(owner.getId(), event.payload().path("userId").asLong());
        assertEquals(created.path("id").asLong(), event.payload().path("teamId").asLong());
        assertEquals("Logged Team", event.payload().path("teamName").asText());
        assertEquals(201, event.payload().path("status").asInt());
    }

    @Test
    @DisplayName("LOG-TEAM-02: メンバー追加成功時に LOG-TEAM-002 を出力する")
    void addMemberWritesLogTeam002() throws Exception {
        TeamContext context = newTeamContext();
        User target = createUser("Target", "target@example.com", "password123");
        clearCapturedLogs();

        addMember(context.ownerToken(), context.team().getId(), target.getId(), TeamRole.MEMBER)
                .andExpect(status().isCreated());

        CapturedLog event = findRequiredEvent(auditAppender, "LOG-TEAM-002");
        assertEquals(context.team().getId(), event.payload().path("teamId").asLong());
        assertEquals(target.getId(), event.payload().path("memberUserId").asLong());
        assertEquals("OWNER", event.payload().path("actorRole").asText());
        assertEquals("MEMBER", event.payload().path("newRole").asText());
    }

    @Test
    @DisplayName("LOG-TEAM-03: ロール変更成功時に LOG-TEAM-003 を出力する")
    void changeRoleWritesLogTeam003() throws Exception {
        TeamContext context = newTeamContext();
        clearCapturedLogs();

        updateMemberRole(context.ownerToken(), context.team().getId(), context.memberMembership().getId(), TeamRole.ADMIN)
                .andExpect(status().isOk());

        CapturedLog event = findRequiredEvent(auditAppender, "LOG-TEAM-003");
        assertEquals(context.member().getId(), event.payload().path("memberUserId").asLong());
        assertEquals("MEMBER", event.payload().path("previousRole").asText());
        assertEquals("ADMIN", event.payload().path("newRole").asText());
    }

    @Test
    @DisplayName("LOG-TEAM-04: メンバー削除成功時に LOG-TEAM-004 を出力する")
    void removeMemberWritesLogTeam004() throws Exception {
        TeamContext context = newTeamContext();
        clearCapturedLogs();

        removeMember(context.ownerToken(), context.team().getId(), context.memberMembership().getId())
                .andExpect(status().isNoContent());

        CapturedLog event = findRequiredEvent(auditAppender, "LOG-TEAM-004");
        assertEquals(context.team().getId(), event.payload().path("teamId").asLong());
        assertEquals(context.member().getId(), event.payload().path("memberUserId").asLong());
        assertEquals("OWNER", event.payload().path("actorRole").asText());
        assertEquals(204, event.payload().path("status").asInt());
    }

    @Test
    @DisplayName("LOG-TEAM-05: 認可失敗時に LOG-TEAM-10x を出力する")
    void forbiddenTeamOperationsWriteLogTeam10x() throws Exception {
        TeamContext context = newTeamContext();
        User target = createUser("Target", "target@example.com", "password123");
        clearCapturedLogs();

        addMember(context.memberToken(), context.team().getId(), target.getId(), TeamRole.MEMBER)
                .andExpect(status().isForbidden());

        CapturedLog event = findRequiredEvent(applicationAppender, "LOG-TEAM-102");
        assertEquals("application", event.loggerName());
        assertEquals(context.member().getId(), event.payload().path("userId").asLong());
        assertEquals(context.team().getId(), event.payload().path("teamId").asLong());
        assertEquals("ERR-TEAM-MEMBER-002", event.payload().path("errorCode").asText());
        assertEquals("MEMBER", event.payload().path("actorRole").asText());
    }

    @Test
    @DisplayName("LOG-TEAM-06: 業務制約違反時に LOG-TEAM-10x を出力する")
    void businessConstraintViolationsWriteLogTeam10x() throws Exception {
        TeamContext context = newTeamContext();
        clearCapturedLogs();

        addMember(context.ownerToken(), context.team().getId(), context.member().getId(), TeamRole.MEMBER)
                .andExpect(status().isConflict());

        CapturedLog event = findRequiredEvent(applicationAppender, "LOG-TEAM-106");
        assertEquals(context.team().getId(), event.payload().path("teamId").asLong());
        assertEquals(context.member().getId(), event.payload().path("memberUserId").asLong());
        assertEquals("MEMBER", event.payload().path("targetRole").asText());
        assertEquals("ERR-TEAM-MEMBER-003", event.payload().path("errorCode").asText());
    }

    @Test
    @DisplayName("LOG-TEAM-09: LOG-TEAM-* 出力時に LOG-SYS-002 を重複出力しない")
    void teamSpecific4xxDoesNotAlsoWriteLogSys002() throws Exception {
        TeamContext context = newTeamContext();
        User target = createUser("Target", "target@example.com", "password123");
        clearCapturedLogs();

        addMember(context.memberToken(), context.team().getId(), target.getId(), TeamRole.MEMBER)
                .andExpect(status().isForbidden());

        assertEquals(1L, countEvents(applicationAppender, "LOG-TEAM-102"));
        assertEquals(0L, countEvents(applicationAppender, "LOG-SYS-002"));
    }

    @Test
    @DisplayName("LOG-TEAM-10: 個別業務ログがない4xxでは LOG-SYS-002 を出力する")
    void generic4xxWithoutSpecificBusinessLogWritesLogSys002() throws Exception {
        User owner = createUser("Owner", "owner@example.com", "password123");
        String token = loginAndGetToken(owner.getEmail(), "password123");
        clearCapturedLogs();

        mockMvc.perform(post("/api/teams")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of("name", ""))))
                .andExpect(status().isBadRequest());

        CapturedLog event = findRequiredEvent(applicationAppender, "LOG-SYS-002");
        assertEquals("ERR-TEAM-001", event.payload().path("errorCode").asText());
        assertTrue(event.payload().path("durationMs").isNumber());
    }

    private TeamContext newTeamContext() throws Exception {
        User owner = createUser("Owner", "owner@example.com", "password123");
        User admin = createUser("Admin", "admin@example.com", "password123");
        User member = createUser("Member", "member@example.com", "password123");
        User outsider = createUser("Outsider", "outsider@example.com", "password123");
        Team team = createTeamWithMember(owner, "Product Team", TeamRole.OWNER);
        team.setDescription("Product work");
        team = teamRepository.save(team);
        TeamMember ownerMembership = teamMemberRepository.findByTeamIdAndUserId(team.getId(), owner.getId()).orElseThrow();
        TeamMember adminMembership = addTeamMember(team, admin, TeamRole.ADMIN);
        TeamMember memberMembership = addTeamMember(team, member, TeamRole.MEMBER);
        String ownerToken = loginAndGetToken(owner.getEmail(), "password123");
        String adminToken = loginAndGetToken(admin.getEmail(), "password123");
        String memberToken = loginAndGetToken(member.getEmail(), "password123");
        String outsiderToken = loginAndGetToken(outsider.getEmail(), "password123");
        return new TeamContext(
                owner,
                admin,
                member,
                outsider,
                team,
                ownerMembership,
                adminMembership,
                memberMembership,
                ownerToken,
                adminToken,
                memberToken,
                outsiderToken
        );
    }

    private JsonNode createTeam(String token, String name, String description) throws Exception {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("name", name);
        if (description != null) {
            payload.put("description", description);
        }
        MvcResult result = mockMvc.perform(post("/api/teams")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(payload)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
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

    private ListAppender<ILoggingEvent> createAppender(String name) {
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.setName(name);
        appender.start();
        return appender;
    }

    private void detachAppender(Logger logger, ListAppender<ILoggingEvent> appender) {
        if (logger != null && appender != null) {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    private void clearCapturedLogs() {
        applicationAppender.list.clear();
        auditAppender.list.clear();
    }

    private CapturedLog findRequiredEvent(ListAppender<ILoggingEvent> appender, String eventId) {
        return capturedEvents(appender)
                .stream()
                .filter(event -> eventId.equals(event.payload().path("eventId").asText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing log event: " + eventId));
    }

    private long countEvents(ListAppender<ILoggingEvent> appender, String eventId) {
        return capturedEvents(appender)
                .stream()
                .filter(event -> eventId.equals(event.payload().path("eventId").asText()))
                .count();
    }

    private List<CapturedLog> capturedEvents(ListAppender<ILoggingEvent> appender) {
        return appender.list.stream()
                .map(this::toCapturedLog)
                .toList();
    }

    private CapturedLog toCapturedLog(ILoggingEvent event) {
        try {
            return new CapturedLog(event.getLoggerName(), objectMapper.readTree(event.getFormattedMessage()));
        } catch (java.io.IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private record CapturedLog(String loggerName, JsonNode payload) {
    }

    private record TeamContext(
            User owner,
            User admin,
            User member,
            User outsider,
            Team team,
            TeamMember ownerMembership,
            TeamMember adminMembership,
            TeamMember memberMembership,
            String ownerToken,
            String adminToken,
            String memberToken,
            String outsiderToken
    ) {
    }
}
