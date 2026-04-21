package com.example.task;

import com.example.task.entity.Priority;
import com.example.task.entity.Task;
import com.example.task.entity.TaskStatus;
import com.example.task.entity.User;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 4. タスク管理のAPI観点を検証する。
 */
class TaskManagementApiIntegrationTests extends ApiIntegrationTestBase {

    @Test
    @DisplayName("TSK-L-01: タスク一覧は参照可能な未削除タスクだけを返す")
    void getTasksReturnsOnlyAccessibleTasks() throws Exception {
        User requester = createUser("Requester", "requester@example.com", "password123");
        User creator = createUser("Creator", "creator@example.com", "password123");
        User outsider = createUser("Outsider", "outsider@example.com", "password123");

        createTask("Owned Task", requester, null, TaskStatus.TODO, Priority.HIGH);
        createTask("Assigned Task", creator, requester, TaskStatus.DOING, Priority.MEDIUM);
        createTask("Hidden Task", creator, outsider, TaskStatus.DONE, Priority.LOW);

        String token = loginAndGetToken("requester@example.com", "password123");

        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].title", hasItems("Owned Task", "Assigned Task")));
    }

    @Test
    @DisplayName("TSK-L-02: タスク一覧は createdAt DESC, id DESC の順で返す")
    void taskListOrdersByCreatedAtDescThenIdDesc() throws Exception {
        User requester = createUser("Requester", "requester@example.com", "password123");
        String token = loginAndGetToken("requester@example.com", "password123");
        Task older = createTask("Same time low id", requester, null, TaskStatus.TODO, Priority.MEDIUM);
        Task newer = createTask("Same time high id", requester, null, TaskStatus.TODO, Priority.MEDIUM);
        setCreatedAt("tasks", LocalDateTime.of(2026, 4, 21, 10, 0), older.getId(), newer.getId());

        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(newer.getId()))
                .andExpect(jsonPath("$[1].id").value(older.getId()));
    }

    @Test
    @DisplayName("TSK-L-03: 論理削除済みタスクは一覧に表示されない")
    void taskListExcludesDeletedTasks() throws Exception {
        User creator = createUser("Creator", "creator@example.com", "password123");
        String token = loginAndGetToken("creator@example.com", "password123");
        Task deletedTask = createTask("Deleted task", creator, null, TaskStatus.TODO, Priority.MEDIUM);
        Task activeTask = createTask("Active task", creator, null, TaskStatus.TODO, Priority.MEDIUM);

        deleteTask(token, deletedTask.getId());

        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(activeTask.getId()));
    }

    @Test
    @DisplayName("TSK-D-03: 削除済みタスクは一覧に表示されない")
    void deletedTaskIsHiddenFromList() throws Exception {
        User creator = createUser("Creator", "creator@example.com", "password123");
        String token = loginAndGetToken("creator@example.com", "password123");
        Task deletedTask = createTask("Deleted task", creator, null, TaskStatus.TODO, Priority.MEDIUM);
        Task activeTask = createTask("Active task", creator, null, TaskStatus.TODO, Priority.MEDIUM);

        deleteTask(token, deletedTask.getId());

        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(activeTask.getId()));
    }

    @Test
    @DisplayName("TSK-L-06: 条件一致0件時は空配列を返す")
    void taskListReturnsEmptyWhenNoTaskMatches() throws Exception {
        User requester = createUser("Requester", "requester@example.com", "password123");
        String token = loginAndGetToken("requester@example.com", "password123");

        mockMvc.perform(get("/api/tasks")
                        .queryParam("status", "DONE")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("TSK-C-01: 必須項目を満たすとタスク作成できる")
    void createTaskSucceeds() throws Exception {
        User assignee = createUser("Assignee", "assignee@example.com", "password123");
        createUser("Creator", "creator@example.com", "password123");
        String token = loginAndGetToken("creator@example.com", "password123");

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(taskPayload(
                                "Initial Task",
                                "Initial Description",
                                "TODO",
                                "HIGH",
                                assignee.getId(),
                                null
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Initial Task"))
                .andExpect(jsonPath("$.description").value("Initial Description"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.assignedUser.id").value(assignee.getId()));
    }

    @Test
    @DisplayName("TSK-C-03: タスク作成のタイトル必須エラーで ERR-TASK-001 を返す")
    void createTaskValidationFails() throws Exception {
        createUser("Creator", "creator@example.com", "password123");
        String token = loginAndGetToken("creator@example.com", "password123");

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of(
                                "title", "",
                                "status", "TODO",
                                "priority", "HIGH"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-001"))
                .andExpect(jsonPath("$.details[*].field", hasItems("title")));
    }

    @Test
    @DisplayName("TSK-C-04: タイトルが101文字以上の作成は ERR-TASK-001 を返す")
    void createTaskRejectsTitleLongerThan100Chars() throws Exception {
        createUser("Creator", "creator@example.com", "password123");
        String token = loginAndGetToken("creator@example.com", "password123");

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of(
                                "title", "a".repeat(101),
                                "description", "Too long",
                                "status", "TODO",
                                "priority", "HIGH"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-001"))
                .andExpect(jsonPath("$.details[*].field", hasItems("title")));
    }

    @Test
    @DisplayName("TSK-C-05: 説明が5001文字以上の作成は ERR-INPUT-001 を返す")
    void createTaskRejectsDescriptionLongerThan5000Chars() throws Exception {
        createUser("Creator", "creator@example.com", "password123");
        String token = loginAndGetToken("creator@example.com", "password123");

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of(
                                "title", "Description too long",
                                "description", "a".repeat(5001),
                                "status", "TODO",
                                "priority", "HIGH"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ERR-INPUT-001"))
                .andExpect(jsonPath("$.details[*].field", hasItems("description")));
    }

    @Test
    @DisplayName("TSK-C-06: 不正なステータス値では ERR-TASK-002 を返す")
    void createTaskRejectsInvalidStatus() throws Exception {
        createUser("Creator", "creator@example.com", "password123");
        String token = loginAndGetToken("creator@example.com", "password123");

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Invalid status",
                                  "status": "BLOCKED",
                                  "priority": "HIGH"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-002"))
                .andExpect(jsonPath("$.details[*].field", hasItems("status")));
    }

    @Test
    @DisplayName("TSK-C-07: 不正な優先度値では ERR-INPUT-001 を返す")
    void createTaskRejectsInvalidPriority() throws Exception {
        createUser("Creator", "creator@example.com", "password123");
        String token = loginAndGetToken("creator@example.com", "password123");

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Invalid priority",
                                  "status": "TODO",
                                  "priority": "URGENT"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ERR-INPUT-001"))
                .andExpect(jsonPath("$.details[*].field", hasItems("priority")));
    }

    @Test
    @DisplayName("TSK-C-08: 不存在の assignedUserId では ERR-USR-002 を返す")
    void createTaskRejectsMissingAssignee() throws Exception {
        createUser("Creator", "creator@example.com", "password123");
        String token = loginAndGetToken("creator@example.com", "password123");

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of(
                                "title", "Missing assignee",
                                "status", "TODO",
                                "priority", "HIGH",
                                "assignedUserId", 999999L
                        ))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ERR-USR-002"));
    }

    @Test
    @DisplayName("TSK-C-09: 作成成功時に TASK_CREATED が記録される")
    void createTaskRecordsActivityLog() throws Exception {
        createUser("Creator", "creator@example.com", "password123");
        String token = loginAndGetToken("creator@example.com", "password123");

        Long taskId = createTaskAndGetId(token, taskPayload(
                "Activity created",
                "Created activity detail",
                "TODO",
                "HIGH",
                null,
                null
        ));

        mockMvc.perform(get("/api/tasks/{taskId}/activities", taskId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].eventType", hasItems("TASK_CREATED")));
    }

    @Test
    @DisplayName("ACT-01: タスク作成時に TASK_CREATED が記録される")
    void taskCreateActivityLogIsRecorded() throws Exception {
        createUser("Creator", "creator@example.com", "password123");
        String token = loginAndGetToken("creator@example.com", "password123");

        Long taskId = createTaskAndGetId(token, taskPayload(
                "Activity created",
                "Created activity detail",
                "TODO",
                "HIGH",
                null,
                null
        ));

        mockMvc.perform(get("/api/tasks/{taskId}/activities", taskId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].eventType", hasItems("TASK_CREATED")));
    }

    @Test
    @DisplayName("TSK-G-01: 参照可能なタスク詳細を取得できる")
    void taskDetailReturnsRequestedTask() throws Exception {
        User creator = createUser("Creator", "creator@example.com", "password123");
        User assignee = createUser("Assignee", "assignee@example.com", "password123");
        Task task = createTask("Detail Task", creator, assignee, TaskStatus.TODO, Priority.HIGH);
        String token = loginAndGetToken("creator@example.com", "password123");

        mockMvc.perform(get("/api/tasks/{taskId}", task.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(task.getId()))
                .andExpect(jsonPath("$.title").value("Detail Task"))
                .andExpect(jsonPath("$.assignedUser.id").value(assignee.getId()))
                .andExpect(jsonPath("$.createdBy.id").value(creator.getId()));
    }

    @Test
    @DisplayName("TSK-G-05: 削除済みタスク取得時は ERR-TASK-004 を返す")
    void deletedTaskDetailReturnsTaskNotFoundError() throws Exception {
        User creator = createUser("Creator", "creator@example.com", "password123");
        Task task = createTask("Deleted task", creator, null, TaskStatus.TODO, Priority.HIGH);
        String token = loginAndGetToken("creator@example.com", "password123");
        deleteTask(token, task.getId());

        mockMvc.perform(get("/api/tasks/{taskId}", task.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-004"));
    }

    @Test
    @DisplayName("TSK-D-05: 削除済みタスク取得時は ERR-TASK-004 扱いになる")
    void deletedTaskGetReturnsTaskNotFoundError() throws Exception {
        User creator = createUser("Creator", "creator@example.com", "password123");
        Task task = createTask("Deleted task", creator, null, TaskStatus.TODO, Priority.HIGH);
        String token = loginAndGetToken("creator@example.com", "password123");
        deleteTask(token, task.getId());

        mockMvc.perform(get("/api/tasks/{taskId}", task.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-004"));
    }

    @Test
    @DisplayName("TSK-G-06: 他人タスクの詳細参照は ERR-AUTH-005 を返す")
    void viewForbiddenTaskReturnsAuth005() throws Exception {
        User creator = createUser("Creator", "creator@example.com", "password123");
        createUser("Outsider", "outsider@example.com", "password123");
        Task hiddenTask = createTask("Hidden Task", creator, null, TaskStatus.TODO, Priority.HIGH);

        String token = loginAndGetToken("outsider@example.com", "password123");

        mockMvc.perform(get("/api/tasks/{taskId}", hiddenTask.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-AUTH-005"));
    }

    @Test
    @DisplayName("TSK-U-01: 最新 version を指定した場合にタスク更新できる")
    void updateTaskSucceeds() throws Exception {
        User creator = createUser("Creator", "creator@example.com", "password123");
        User assignee = createUser("Assignee", "assignee@example.com", "password123");
        Task task = createTask("Initial Task", creator, assignee, TaskStatus.TODO, Priority.HIGH);
        String token = loginAndGetToken("creator@example.com", "password123");

        mockMvc.perform(put("/api/tasks/{taskId}", task.getId())
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(taskPayload(
                                "Updated Task",
                                "Updated Description",
                                "DOING",
                                "MEDIUM",
                                null,
                                getTaskVersion(token, task.getId())
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Task"))
                .andExpect(jsonPath("$.status").value("DOING"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.assignedUser").doesNotExist())
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    @DisplayName("タスク更新: タイトルが101文字以上の場合は ERR-TASK-001 を返す")
    void updateTaskRejectsTitleLongerThan100Chars() throws Exception {
        User creator = createUser("Creator", "creator@example.com", "password123");
        String token = loginAndGetToken("creator@example.com", "password123");
        Long taskId = createTask("Short Title", creator, null, TaskStatus.TODO, Priority.HIGH).getId();

        mockMvc.perform(put("/api/tasks/{taskId}", taskId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of(
                                "title", "a".repeat(101),
                                "description", "Too long",
                                "status", "DOING",
                                "priority", "MEDIUM",
                                "version", taskRepository.findById(taskId).orElseThrow().getVersion()
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-001"))
                .andExpect(jsonPath("$.details[*].field", hasItems("title")));
    }

    @Test
    @DisplayName("TSK-U-02: version が不一致なら ERR-TASK-007 を返す")
    void updateTaskWithStaleVersionReturnsConflict() throws Exception {
        User creator = createUser("Creator", "creator@example.com", "password123");
        String token = loginAndGetToken("creator@example.com", "password123");
        Long taskId = createTask("Versioned Task", creator, null, TaskStatus.TODO, Priority.MEDIUM).getId();

        mockMvc.perform(put("/api/tasks/{taskId}", taskId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of(
                                "title", "Stale Update",
                                "description", "Should fail",
                                "status", "DOING",
                                "priority", "HIGH",
                                "version", 99
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-007"));
    }

    @Test
    @DisplayName("TSK-U-03: version 競合時は更新・履歴・通知が発生しない")
    void staleTaskUpdateHasNoSideEffects() throws Exception {
        User creator = createUser("Creator", "creator@example.com", "password123");
        Task task = createTask("Original title", creator, null, TaskStatus.TODO, Priority.MEDIUM);
        String token = loginAndGetToken("creator@example.com", "password123");

        mockMvc.perform(put("/api/tasks/{taskId}", task.getId())
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of(
                                "title", "Stale update",
                                "description", "Should not persist",
                                "status", "DOING",
                                "priority", "HIGH",
                                "version", 999L
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-007"));

        assertEquals("Original title", taskRepository.findById(task.getId()).orElseThrow().getTitle());
        assertEquals(0L, countRows("activity_logs"));
        assertEquals(0L, countRows("notifications"));
    }

    @Test
    @DisplayName("TSK-U-04: version 競合後に最新タスク詳細を再取得できる")
    void latestTaskCanBeReloadedAfterStaleUpdate() throws Exception {
        User creator = createUser("Creator", "creator@example.com", "password123");
        Task task = createTask("Original title", creator, null, TaskStatus.TODO, Priority.MEDIUM);
        String token = loginAndGetToken("creator@example.com", "password123");

        mockMvc.perform(put("/api/tasks/{taskId}", task.getId())
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of(
                                "title", "Stale update",
                                "description", "Should not persist",
                                "status", "DOING",
                                "priority", "HIGH",
                                "version", 999L
                        ))))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/tasks/{taskId}", task.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Original title"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    @DisplayName("ACT-02: タスク更新時に TASK_UPDATED が記録される")
    void taskUpdateRecordsActivityLog() throws Exception {
        User creator = createUser("Creator", "creator@example.com", "password123");
        Task task = createTask("Initial Task", creator, null, TaskStatus.TODO, Priority.HIGH);
        String token = loginAndGetToken("creator@example.com", "password123");

        updateTask(token, task.getId(), getTaskVersion(token, task.getId()));

        mockMvc.perform(get("/api/tasks/{taskId}/activities", task.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].eventType", hasItems("TASK_UPDATED")));
    }

    @Test
    @DisplayName("ACT-11: タスク更新差分が detail_json に保持される")
    void taskUpdateRecordsChangeDetails() throws Exception {
        User creator = createUser("Creator", "creator@example.com", "password123");
        User assignee = createUser("Assignee", "assignee@example.com", "password123");
        Task task = createTask("Initial Task", creator, assignee, TaskStatus.TODO, Priority.HIGH);
        String token = loginAndGetToken("creator@example.com", "password123");

        updateTask(token, task.getId(), getTaskVersion(token, task.getId()));

        MvcResult activityResult = mockMvc.perform(get("/api/tasks/{taskId}/activities", task.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode taskActivities = objectMapper.readTree(activityResult.getResponse().getContentAsString()).path("content");
        JsonNode taskUpdatedActivity = findActivityByEventType(taskActivities, "TASK_UPDATED");
        JsonNode taskChanges = taskUpdatedActivity.path("detailJson").path("changes");
        assertTrue(taskChanges.isArray());
        assertEquals("Initial Task", findChangeByField(taskChanges, "title").path("oldValue").asText());
        assertEquals("Updated Task", findChangeByField(taskChanges, "title").path("newValue").asText());
        assertEquals("TODO", findChangeByField(taskChanges, "status").path("oldValue").asText());
        assertEquals("DOING", findChangeByField(taskChanges, "status").path("newValue").asText());
        assertEquals(String.valueOf(assignee.getId()), findChangeByField(taskChanges, "assignedUserId").path("oldValue").asText());
        assertTrue(findChangeByField(taskChanges, "assignedUserId").path("newValue").isNull());
    }

    @Test
    @DisplayName("AUTH-02: 他人タスクの更新は ERR-TASK-005 を返す")
    void updateForbiddenTaskReturnsTaskPermissionCode() throws Exception {
        User creator = createUser("Creator", "creator@example.com", "password123");
        createUser("Outsider", "outsider@example.com", "password123");
        Task hiddenTask = createTask("Hidden Task", creator, null, TaskStatus.TODO, Priority.HIGH);

        String token = loginAndGetToken("outsider@example.com", "password123");

        mockMvc.perform(put("/api/tasks/{taskId}", hiddenTask.getId())
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(taskPayload(
                                "Blocked Update",
                                "No permission",
                                "DOING",
                                "LOW",
                                null,
                                hiddenTask.getVersion()
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-005"));
    }

    @Test
    @DisplayName("TSK-D-01: 削除権限を持つユーザーがタスク削除できる")
    void deleteTaskSucceeds() throws Exception {
        User creator = createUser("Creator", "creator@example.com", "password123");
        Task task = createTask("Deleted task", creator, null, TaskStatus.TODO, Priority.HIGH);
        String token = loginAndGetToken("creator@example.com", "password123");

        deleteTask(token, task.getId());
    }

    @Test
    @DisplayName("TSK-D-02: タスク削除時に deleted_at と deleted_by が設定される")
    void taskDeleteSetsLogicalDeletionFields() throws Exception {
        User creator = createUser("Creator", "creator@example.com", "password123");
        Task task = createTask("Deleted task", creator, null, TaskStatus.TODO, Priority.HIGH);
        String token = loginAndGetToken("creator@example.com", "password123");

        deleteTask(token, task.getId());

        assertNotNull(jdbcTemplate.queryForObject(
                "select deleted_at from " + table("tasks") + " where id = ?",
                Timestamp.class,
                task.getId()
        ));
        assertEquals(creator.getId(), jdbcTemplate.queryForObject(
                "select deleted_by from " + table("tasks") + " where id = ?",
                Long.class,
                task.getId()
        ));
    }

    @Test
    @DisplayName("TSK-D-04: 削除済みタスク詳細を通常表示しない")
    void deletedTaskDetailIsHidden() throws Exception {
        User creator = createUser("Creator", "creator@example.com", "password123");
        Task task = createTask("Deleted task", creator, null, TaskStatus.TODO, Priority.HIGH);
        String token = loginAndGetToken("creator@example.com", "password123");
        deleteTask(token, task.getId());

        mockMvc.perform(get("/api/tasks/{taskId}", task.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("TSK-D-06: 削除済みタスクは更新できない")
    void deletedTaskCannotBeUpdated() throws Exception {
        User creator = createUser("Creator", "creator@example.com", "password123");
        Task task = createTask("Deleted task", creator, null, TaskStatus.TODO, Priority.HIGH);
        String token = loginAndGetToken("creator@example.com", "password123");
        deleteTask(token, task.getId());

        mockMvc.perform(put("/api/tasks/{taskId}", task.getId())
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(taskPayload(
                                "Cannot update",
                                "Cannot update",
                                "DOING",
                                "LOW",
                                null,
                                0L
                        ))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-004"));
    }

    @Test
    @DisplayName("TSK-D-07: 削除済みタスクのコメント一覧は取得できない")
    void deletedTaskCommentsCannotBeListed() throws Exception {
        User creator = createUser("Creator", "creator@example.com", "password123");
        Task task = createTask("Deleted task", creator, null, TaskStatus.TODO, Priority.HIGH);
        String token = loginAndGetToken("creator@example.com", "password123");
        deleteTask(token, task.getId());

        mockMvc.perform(get("/api/tasks/{taskId}/comments", task.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-004"));
    }

    @Test
    @DisplayName("TSK-D-08: 削除済みタスクの添付一覧は取得できない")
    void deletedTaskAttachmentsCannotBeListed() throws Exception {
        User creator = createUser("Creator", "creator@example.com", "password123");
        Task task = createTask("Deleted task", creator, null, TaskStatus.TODO, Priority.HIGH);
        String token = loginAndGetToken("creator@example.com", "password123");
        deleteTask(token, task.getId());

        mockMvc.perform(get("/api/tasks/{taskId}/attachments", task.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-004"));
    }

    @Test
    @DisplayName("TSK-D-09: 削除済みタスクの履歴参照は設計通り拒否される")
    void deletedTaskActivitiesCannotBeListed() throws Exception {
        User creator = createUser("Creator", "creator@example.com", "password123");
        Task task = createTask("Deleted task", creator, null, TaskStatus.TODO, Priority.HIGH);
        String token = loginAndGetToken("creator@example.com", "password123");
        deleteTask(token, task.getId());

        mockMvc.perform(get("/api/tasks/{taskId}/activities", task.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-004"));
    }

    @Test
    @DisplayName("TSK-D-10: 削除成功時に TASK_DELETED が記録される")
    void deleteTaskRecordsActivityLog() throws Exception {
        User creator = createUser("Creator", "creator@example.com", "password123");
        Task task = createTask("Deleted task", creator, null, TaskStatus.TODO, Priority.HIGH);
        String token = loginAndGetToken("creator@example.com", "password123");

        deleteTask(token, task.getId());

        assertEquals(1L, jdbcTemplate.queryForObject(
                "select count(*) from " + table("activity_logs") + " where task_id = ? and event_type = 'TASK_DELETED'",
                Long.class,
                task.getId()
        ));
    }

    @Test
    @DisplayName("ACT-03: タスク削除時に TASK_DELETED が記録される")
    void taskDeleteActivityLogIsRecorded() throws Exception {
        User creator = createUser("Creator", "creator@example.com", "password123");
        Task task = createTask("Deleted task", creator, null, TaskStatus.TODO, Priority.HIGH);
        String token = loginAndGetToken("creator@example.com", "password123");

        deleteTask(token, task.getId());

        assertEquals(1L, jdbcTemplate.queryForObject(
                "select count(*) from " + table("activity_logs") + " where task_id = ? and event_type = 'TASK_DELETED'",
                Long.class,
                task.getId()
        ));
    }

    @Test
    @DisplayName("AUTH-03: 他人タスクの削除は ERR-TASK-006 を返す")
    void deleteForbiddenTaskReturnsTaskPermissionCode() throws Exception {
        User creator = createUser("Creator", "creator@example.com", "password123");
        createUser("Outsider", "outsider@example.com", "password123");
        Task hiddenTask = createTask("Hidden Task", creator, null, TaskStatus.TODO, Priority.HIGH);

        String token = loginAndGetToken("outsider@example.com", "password123");

        mockMvc.perform(delete("/api/tasks/{taskId}", hiddenTask.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-006"));
    }

    private void updateTask(String token, Long taskId, Long version) throws Exception {
        mockMvc.perform(put("/api/tasks/{taskId}", taskId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(taskPayload(
                                "Updated Task",
                                "Updated Description",
                                "DOING",
                                "MEDIUM",
                                null,
                                version
                        ))))
                .andExpect(status().isOk());
    }

    private void deleteTask(String token, Long taskId) throws Exception {
        mockMvc.perform(delete("/api/tasks/{taskId}", taskId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
    }

    private Map<String, Object> taskPayload(
            String title,
            String description,
            String status,
            String priority,
            Long assignedUserId,
            Long version
    ) {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("title", title);
        payload.put("description", description);
        payload.put("status", status);
        payload.put("priority", priority);
        payload.put("dueDate", "2026-04-25");
        if (assignedUserId != null) {
            payload.put("assignedUserId", assignedUserId);
        }
        if (version != null) {
            payload.put("version", version);
        }
        return payload;
    }
}
