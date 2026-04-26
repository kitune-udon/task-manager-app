package com.example.task;

import com.example.task.entity.Priority;
import com.example.task.entity.Task;
import com.example.task.entity.TaskStatus;
import com.example.task.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 10. アクティビティログ と 11. 認可観点を横断的に検証する。
 */
class ActivityAuthorizationApiIntegrationTests extends ApiIntegrationTestBase {

    @Test
    @DisplayName("AUTH-01: 作成者と担当者はタスクを参照できる")
    void creatorAndAssigneeCanViewTask() throws Exception {
        AuthorizationContext context = newAuthorizationContext();

        mockMvc.perform(get("/api/tasks/{taskId}", context.task().getId())
                        .header("Authorization", bearer(context.creatorToken())))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/tasks/{taskId}", context.task().getId())
                        .header("Authorization", bearer(context.assigneeToken())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("AUTH-02: 作成者と担当者はタスクを更新できる")
    void creatorAndAssigneeCanUpdateTask() throws Exception {
        AuthorizationContext context = newAuthorizationContext();

        updateTask(context.creatorToken(), context.task(), "Creator update", 0L);

        long version = getTaskVersion(context.assigneeToken(), context.task().getId());
        updateTask(context.assigneeToken(), context.task(), "Assignee update", version);
    }

    @Test
    @DisplayName("AUTH-09: タスク参照権限がある場合のみ履歴参照できる")
    void onlyTaskViewersCanViewActivities() throws Exception {
        AuthorizationContext context = newAuthorizationContext();
        updateTask(context.creatorToken(), context.task(), "Creator update", 0L);

        mockMvc.perform(get("/api/tasks/{taskId}/activities", context.task().getId())
                        .header("Authorization", bearer(context.assigneeToken())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tasks/{taskId}/activities", context.task().getId())
                        .header("Authorization", bearer(context.outsiderToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-009"));
    }

    @Test
    @DisplayName("ACT-09: 履歴の actor_user_id に操作者が記録される")
    void activityHistoryRecordsActorUserId() throws Exception {
        AuthorizationContext context = newAuthorizationContext();
        updateTask(context.creatorToken(), context.task(), "Creator update", 0L);

        long version = getTaskVersion(context.assigneeToken(), context.task().getId());
        updateTask(context.assigneeToken(), context.task(), "Assignee update", version);

        mockMvc.perform(get("/api/tasks/{taskId}/activities", context.task().getId())
                        .header("Authorization", bearer(context.assigneeToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].actor.id", hasItems(
                        context.creator().getId().intValue(),
                        context.assignee().getId().intValue()
                )));
    }

    @Test
    @DisplayName("ACT-10: 履歴に関連タスクIDが記録される")
    void activityHistoryRecordsTaskId() throws Exception {
        AuthorizationContext context = newAuthorizationContext();
        updateTask(context.creatorToken(), context.task(), "Creator update", 0L);

        mockMvc.perform(get("/api/tasks/{taskId}/activities", context.task().getId())
                        .header("Authorization", bearer(context.assigneeToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].taskId", hasItems(context.task().getId().intValue())));
    }

    private AuthorizationContext newAuthorizationContext() throws Exception {
        User creator = createUser("Creator", "creator@example.com", "password123");
        User assignee = createUser("Assignee", "assignee@example.com", "password123");
        createUser("Outsider", "outsider@example.com", "password123");
        Task task = createTask("Authorization matrix", creator, assignee, TaskStatus.TODO, Priority.MEDIUM);
        String creatorToken = loginAndGetToken("creator@example.com", "password123");
        String assigneeToken = loginAndGetToken("assignee@example.com", "password123");
        String outsiderToken = loginAndGetToken("outsider@example.com", "password123");
        return new AuthorizationContext(creator, assignee, task, creatorToken, assigneeToken, outsiderToken);
    }

    private void updateTask(String token, Task task, String title, Long version) throws Exception {
        mockMvc.perform(put("/api/tasks/{taskId}", task.getId())
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of(
                                "title", title,
                                "status", "TODO",
                                "priority", "HIGH",
                                "assignedUserId", task.getAssignedUser().getId(),
                                "version", version
                        ))))
                .andExpect(status().isOk());
    }

    private record AuthorizationContext(
            User creator,
            User assignee,
            Task task,
            String creatorToken,
            String assigneeToken,
            String outsiderToken
    ) {
    }
}
