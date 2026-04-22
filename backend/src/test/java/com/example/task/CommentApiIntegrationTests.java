package com.example.task;

import com.example.task.entity.Priority;
import com.example.task.entity.Task;
import com.example.task.entity.TaskComment;
import com.example.task.entity.TaskStatus;
import com.example.task.entity.User;
import com.example.task.service.ActivityNotificationService;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 6. コメント機能のAPI観点を検証する。
 */
class CommentApiIntegrationTests extends ApiIntegrationTestBase {

    @Autowired
    private ActivityNotificationService activityNotificationService;

    @Test
    @DisplayName("CMT-L-01: 対象タスクのコメント一覧を取得できる")
    void commentListReturnsComments() throws Exception {
        CommentContext context = newCommentContext("Comment list");
        JsonNode created = postComment(context.creatorToken(), context.task().getId(), "listed comment");

        mockMvc.perform(get("/api/tasks/{taskId}/comments", context.task().getId())
                        .header("Authorization", bearer(context.creatorToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(created.path("id").asLong()))
                .andExpect(jsonPath("$.content[0].content").value("listed comment"));
    }

    @Test
    @DisplayName("CMT-L-02: コメント一覧は createdAt ASC, id ASC の順で返す")
    void commentListOrdersByCreatedAtAscThenIdAsc() throws Exception {
        CommentContext context = newCommentContext("Comment ordering");
        JsonNode first = postComment(context.creatorToken(), context.task().getId(), "first comment");
        JsonNode second = postComment(context.creatorToken(), context.task().getId(), "second comment");
        setCreatedAt("task_comments", LocalDateTime.of(2026, 4, 21, 11, 0), first.path("id").asLong(), second.path("id").asLong());

        mockMvc.perform(get("/api/tasks/{taskId}/comments", context.task().getId())
                        .header("Authorization", bearer(context.creatorToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(first.path("id").asLong()))
                .andExpect(jsonPath("$.content[1].id").value(second.path("id").asLong()));
    }

    @Test
    @DisplayName("CMT-L-03: 削除済みコメントは一覧から除外される")
    void commentListExcludesDeletedCommentsWithoutPlaceholder() throws Exception {
        CommentContext context = newCommentContext("Comment deletion filter");
        JsonNode deleted = postComment(context.creatorToken(), context.task().getId(), "deleted comment");
        JsonNode active = postComment(context.creatorToken(), context.task().getId(), "active comment");

        deleteComment(context.creatorToken(), deleted.path("id").asLong());

        mockMvc.perform(get("/api/tasks/{taskId}/comments", context.task().getId())
                        .header("Authorization", bearer(context.creatorToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(active.path("id").asLong()))
                .andExpect(jsonPath("$.content[0].content").value("active comment"));
    }

    @Test
    @DisplayName("CMT-L-08: 削除済みコメントをプレースホルダ表示せず一覧から除外する")
    void commentListDoesNotShowDeletedPlaceholder() throws Exception {
        CommentContext context = newCommentContext("Comment deletion placeholder");
        JsonNode deleted = postComment(context.creatorToken(), context.task().getId(), "deleted comment");
        JsonNode active = postComment(context.creatorToken(), context.task().getId(), "active comment");

        deleteComment(context.creatorToken(), deleted.path("id").asLong());

        mockMvc.perform(get("/api/tasks/{taskId}/comments", context.task().getId())
                        .header("Authorization", bearer(context.creatorToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(active.path("id").asLong()))
                .andExpect(jsonPath("$.content[0].content").value("active comment"));
    }

    @Test
    @DisplayName("CMT-L-04: コメント0件時は空ページを返す")
    void commentListReturnsEmptyPage() throws Exception {
        CommentContext context = newCommentContext("No comments");

        mockMvc.perform(get("/api/tasks/{taskId}/comments", context.task().getId())
                        .header("Authorization", bearer(context.creatorToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    @DisplayName("CMT-L-05: page と size に応じてコメント取得結果が切り替わる")
    void commentListPaginates() throws Exception {
        CommentContext context = newCommentContext("Comment pagination");
        JsonNode first = postComment(context.creatorToken(), context.task().getId(), "first comment");
        JsonNode second = postComment(context.creatorToken(), context.task().getId(), "second comment");
        setCreatedAt("task_comments", LocalDateTime.of(2026, 4, 21, 11, 0), first.path("id").asLong(), second.path("id").asLong());

        mockMvc.perform(get("/api/tasks/{taskId}/comments", context.task().getId())
                        .queryParam("page", "0")
                        .queryParam("size", "1")
                        .header("Authorization", bearer(context.creatorToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(first.path("id").asLong()))
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/tasks/{taskId}/comments", context.task().getId())
                        .queryParam("page", "1")
                        .queryParam("size", "1")
                        .header("Authorization", bearer(context.creatorToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(second.path("id").asLong()));
    }

    @Test
    @DisplayName("CMT-L-07: タスク未存在時は ERR-TASK-004 を返す")
    void commentListRejectsMissingTask() throws Exception {
        createUser("Creator", "creator@example.com", "password123");
        String token = loginAndGetToken("creator@example.com", "password123");

        mockMvc.perform(get("/api/tasks/{taskId}/comments", 999999L)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-004"));
    }

    @Test
    @DisplayName("CMT-L-06: 参照不可タスクのコメント一覧は取得できない")
    void commentListRejectsForbiddenTask() throws Exception {
        CommentContext context = newCommentContext("Comment list auth");
        User outsider = createUser("Outsider", "outsider@example.com", "password123");
        String outsiderToken = loginAndGetToken(outsider.getEmail(), "password123");

        mockMvc.perform(get("/api/tasks/{taskId}/comments", context.task().getId())
                        .header("Authorization", bearer(outsiderToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-AUTH-005"));
    }

    @Test
    @DisplayName("CMT-C-01: コメントを投稿できる")
    void commentCreateSucceeds() throws Exception {
        CommentContext context = newCommentContext("Comment create");

        mockMvc.perform(post("/api/tasks/{taskId}/comments", context.task().getId())
                        .header("Authorization", bearer(context.creatorToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of("content", "Initial comment"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Initial comment"))
                .andExpect(jsonPath("$.createdBy.id").value(context.creator().getId()));
    }

    @Test
    @DisplayName("CMT-C-02: 投稿成功後にコメント一覧へ反映される")
    void commentCreateReflectsInList() throws Exception {
        CommentContext context = newCommentContext("Comment list reflection");

        postComment(context.creatorToken(), context.task().getId(), "reflected comment");

        mockMvc.perform(get("/api/tasks/{taskId}/comments", context.task().getId())
                        .header("Authorization", bearer(context.creatorToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].content").value("reflected comment"));
    }

    @Test
    @DisplayName("CMT-C-03: 1000文字以内ならコメント投稿できる")
    void commentCreateAllowsMaximumLength() throws Exception {
        CommentContext context = newCommentContext("Comment max length");

        postComment(context.creatorToken(), context.task().getId(), "a".repeat(1000));
    }

    @Test
    @DisplayName("CMT-C-04: 1001文字以上のコメント投稿は ERR-COMMENT-003 を返す")
    void commentCreateRejectsTooLongContent() throws Exception {
        CommentContext context = newCommentContext("Comment too long");

        mockMvc.perform(post("/api/tasks/{taskId}/comments", context.task().getId())
                        .header("Authorization", bearer(context.creatorToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of("content", "a".repeat(1001)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ERR-COMMENT-003"));
    }

    @Test
    @DisplayName("CMT-C-05: trim 後空文字のコメント投稿は ERR-COMMENT-001 を返す")
    void commentCreateRejectsBlankContent() throws Exception {
        CommentContext context = newCommentContext("Comment blank");

        mockMvc.perform(post("/api/tasks/{taskId}/comments", context.task().getId())
                        .header("Authorization", bearer(context.creatorToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of("content", "   "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ERR-COMMENT-001"));
    }

    @Test
    @DisplayName("CMT-C-06: 参照不可タスクにはコメント投稿できない")
    void commentCreateRejectsForbiddenTask() throws Exception {
        CommentContext context = newCommentContext("Comment auth");
        User outsider = createUser("Outsider", "outsider@example.com", "password123");
        String outsiderToken = loginAndGetToken(outsider.getEmail(), "password123");

        mockMvc.perform(post("/api/tasks/{taskId}/comments", context.task().getId())
                        .header("Authorization", bearer(outsiderToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of("content", "blocked"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-AUTH-005"));
    }

    @Test
    @DisplayName("CMT-C-07: コメント投稿時に COMMENT_CREATED が記録される")
    void commentCreateRecordsActivityLog() throws Exception {
        CommentContext context = newCommentContext("Comment create activity");

        postComment(context.creatorToken(), context.task().getId(), "activity comment");

        mockMvc.perform(get("/api/tasks/{taskId}/activities", context.task().getId())
                        .header("Authorization", bearer(context.creatorToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].eventType", hasItems("COMMENT_CREATED")));
    }

    @Test
    @DisplayName("ACT-04: コメント投稿時に COMMENT_CREATED が記録される")
    void commentCreateActivityLogIsRecorded() throws Exception {
        CommentContext context = newCommentContext("Comment create activity");

        postComment(context.creatorToken(), context.task().getId(), "activity comment");

        mockMvc.perform(get("/api/tasks/{taskId}/activities", context.task().getId())
                        .header("Authorization", bearer(context.creatorToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].eventType", hasItems("COMMENT_CREATED")));
    }

    @Test
    @DisplayName("CMT-C-08: コメント投稿時に対象関係者へ通知が生成される")
    void commentCreateGeneratesNotification() throws Exception {
        CommentContext context = newCommentContext("Comment notification");

        postComment(context.creatorToken(), context.task().getId(), "notify assignee");

        assertEquals(1L, jdbcTemplate.queryForObject(
                "select count(*) from " + table("notifications") + " where recipient_user_id = ?",
                Long.class,
                context.assignee().getId()
        ));
    }

    @Test
    @DisplayName("CMT-C-09: コメント通知で操作者本人を除外する")
    void commentNotificationsExcludeActor() throws Exception {
        CommentContext context = newCommentContext("Notification target");

        postComment(context.creatorToken(), context.task().getId(), "notify assignee only");

        assertEquals(0L, jdbcTemplate.queryForObject(
                "select count(*) from " + table("notifications") + " where recipient_user_id = ?",
                Long.class,
                context.creator().getId()
        ));
        assertEquals(1L, jdbcTemplate.queryForObject(
                "select count(*) from " + table("notifications") + " where recipient_user_id = ?",
                Long.class,
                context.assignee().getId()
        ));
    }

    @Test
    @DisplayName("CMT-C-10: 同一ユーザーにコメント通知が重複生成されない")
    @Transactional
    void commentNotificationsDeduplicateSameRecipient() {
        User recipient = createUser("Recipient", "recipient@example.com", "password123");
        User actor = createUser("Actor", "actor@example.com", "password123");
        Task task = createTask("Duplicate recipient task", recipient, recipient, TaskStatus.TODO, Priority.HIGH);
        TaskComment comment = taskCommentRepository.save(TaskComment.builder()
                .task(task)
                .content("deduplicate recipient")
                .createdBy(actor)
                .build());

        activityNotificationService.recordCommentCreated(actor, comment);

        assertEquals(1L, jdbcTemplate.queryForObject(
                "select count(*) from " + table("notifications") + " where recipient_user_id = ?",
                Long.class,
                recipient.getId()
        ));
    }

    @Test
    @DisplayName("CMT-U-01: コメント投稿者本人が更新できる")
    void commentUpdateSucceeds() throws Exception {
        CommentContext context = newCommentContext("Comment update");
        JsonNode created = postComment(context.creatorToken(), context.task().getId(), "before update");

        updateComment(context.creatorToken(), created.path("id").asLong(), "Updated comment", created.path("version").asLong())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Updated comment"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    @DisplayName("CMT-U-03: MEMBER は他人コメントを更新できない")
    void commentUpdateRejectsOtherMember() throws Exception {
        CommentContext context = newCommentContext("Comment update auth");
        JsonNode created = postComment(context.creatorToken(), context.task().getId(), "before update");

        updateComment(context.assigneeToken(), created.path("id").asLong(), "assignee update", created.path("version").asLong())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-COMMENT-004"));
    }

    @Test
    @DisplayName("AUTH-04: コメント投稿者本人以外の MEMBER はコメント更新できない")
    void commentUpdateAuthorizationRejectsOtherMember() throws Exception {
        CommentContext context = newCommentContext("Comment update authorization");
        JsonNode created = postComment(context.creatorToken(), context.task().getId(), "before update");

        updateComment(context.assigneeToken(), created.path("id").asLong(), "assignee update", created.path("version").asLong())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-COMMENT-004"));
    }

    @Test
    @DisplayName("CMT-U-04: 1000文字以内ならコメント更新できる")
    void commentUpdateAllowsMaximumLength() throws Exception {
        CommentContext context = newCommentContext("Comment update max length");
        JsonNode created = postComment(context.creatorToken(), context.task().getId(), "before update");

        updateComment(context.creatorToken(), created.path("id").asLong(), "a".repeat(1000), created.path("version").asLong())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("a".repeat(1000)));
    }

    @Test
    @DisplayName("CMT-U-05: trim 後空文字ではコメント更新できない")
    void commentUpdateRejectsBlankContent() throws Exception {
        CommentContext context = newCommentContext("Comment update blank");
        JsonNode created = postComment(context.creatorToken(), context.task().getId(), "before update");

        updateComment(context.creatorToken(), created.path("id").asLong(), "   ", created.path("version").asLong())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ERR-COMMENT-001"));
    }

    @Test
    @DisplayName("CMT-U-06: コメント更新後に updatedAt が更新される")
    void commentUpdateChangesUpdatedAt() throws Exception {
        CommentContext context = newCommentContext("Comment update timestamp");
        JsonNode created = postComment(context.creatorToken(), context.task().getId(), "before update");

        MvcResult updateResult = updateComment(
                context.creatorToken(),
                created.path("id").asLong(),
                "updated timestamp",
                created.path("version").asLong()
        )
                .andExpect(status().isOk())
                .andReturn();

        String updatedAt = objectMapper.readTree(updateResult.getResponse().getContentAsString()).path("updatedAt").asText();
        assertNotEquals(created.path("updatedAt").asText(), updatedAt);
    }

    @Test
    @DisplayName("CMT-U-07: コメント更新時に COMMENT_UPDATED が記録される")
    void commentUpdateRecordsActivityLog() throws Exception {
        CommentContext context = newCommentContext("Comment update activity");
        JsonNode created = postComment(context.creatorToken(), context.task().getId(), "before update");

        updateComment(context.creatorToken(), created.path("id").asLong(), "updated activity", created.path("version").asLong())
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tasks/{taskId}/activities", context.task().getId())
                        .header("Authorization", bearer(context.creatorToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].eventType", hasItems("COMMENT_UPDATED")));
    }

    @Test
    @DisplayName("ACT-05: コメント更新時に COMMENT_UPDATED が記録される")
    void commentUpdateActivityLogIsRecorded() throws Exception {
        CommentContext context = newCommentContext("Comment update activity");
        JsonNode created = postComment(context.creatorToken(), context.task().getId(), "before update");

        updateComment(context.creatorToken(), created.path("id").asLong(), "updated activity", created.path("version").asLong())
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tasks/{taskId}/activities", context.task().getId())
                        .header("Authorization", bearer(context.creatorToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].eventType", hasItems("COMMENT_UPDATED")));
    }

    @Test
    @DisplayName("CMT-U-08: 古い version で更新すると ERR-COMMENT-006 を返す")
    void commentUpdateWithStaleVersionReturnsConflict() throws Exception {
        CommentContext context = newCommentContext("Comment stale update");
        JsonNode created = postComment(context.creatorToken(), context.task().getId(), "before update");
        updateComment(context.creatorToken(), created.path("id").asLong(), "first update", created.path("version").asLong())
                .andExpect(status().isOk());

        updateComment(context.creatorToken(), created.path("id").asLong(), "stale update", created.path("version").asLong())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ERR-COMMENT-006"));
    }

    @Test
    @DisplayName("CMT-U-09: コメント更新競合時は本文更新・履歴・通知が発生しない")
    void staleCommentUpdateHasNoSideEffects() throws Exception {
        CommentContext context = newCommentContext("Comment stale side effects");
        JsonNode created = postComment(context.creatorToken(), context.task().getId(), "before update");
        long activityCountBeforeConflict = countRows("activity_logs");
        long notificationCountBeforeConflict = countRows("notifications");

        updateComment(context.creatorToken(), created.path("id").asLong(), "stale update", 999L)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ERR-COMMENT-006"));

        assertEquals("before update", taskCommentRepository.findById(created.path("id").asLong()).orElseThrow().getContent());
        assertEquals(activityCountBeforeConflict, countRows("activity_logs"));
        assertEquals(notificationCountBeforeConflict, countRows("notifications"));
    }

    @Test
    @DisplayName("CMT-D-01: コメント投稿者本人が削除できる")
    void commentDeleteSucceeds() throws Exception {
        CommentContext context = newCommentContext("Comment delete");
        JsonNode created = postComment(context.creatorToken(), context.task().getId(), "before delete");

        deleteComment(context.creatorToken(), created.path("id").asLong());
    }

    @Test
    @DisplayName("CMT-D-03: MEMBER は他人コメントを削除できない")
    void commentDeleteRejectsOtherMember() throws Exception {
        CommentContext context = newCommentContext("Comment delete auth");
        JsonNode created = postComment(context.creatorToken(), context.task().getId(), "before delete");

        mockMvc.perform(delete("/api/comments/{commentId}", created.path("id").asLong())
                        .header("Authorization", bearer(context.assigneeToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-COMMENT-005"));
    }

    @Test
    @DisplayName("AUTH-05: コメント投稿者本人以外の MEMBER はコメント削除できない")
    void commentDeleteAuthorizationRejectsOtherMember() throws Exception {
        CommentContext context = newCommentContext("Comment delete authorization");
        JsonNode created = postComment(context.creatorToken(), context.task().getId(), "before delete");

        mockMvc.perform(delete("/api/comments/{commentId}", created.path("id").asLong())
                        .header("Authorization", bearer(context.assigneeToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-COMMENT-005"));
    }

    @Test
    @DisplayName("CMT-D-04: コメント削除時に deleted_at と deleted_by が設定される")
    void commentDeleteSetsLogicalDeletionFields() throws Exception {
        CommentContext context = newCommentContext("Comment logical delete");
        JsonNode created = postComment(context.creatorToken(), context.task().getId(), "before delete");
        long commentId = created.path("id").asLong();

        deleteComment(context.creatorToken(), commentId);

        assertNotNull(jdbcTemplate.queryForObject(
                "select deleted_at from " + table("task_comments") + " where id = ?",
                Timestamp.class,
                commentId
        ));
        assertEquals(context.creator().getId(), jdbcTemplate.queryForObject(
                "select deleted_by from " + table("task_comments") + " where id = ?",
                Long.class,
                commentId
        ));
    }

    @Test
    @DisplayName("CMT-D-05: 削除後にコメント一覧へ表示されない")
    void commentDeleteHidesCommentFromList() throws Exception {
        CommentContext context = newCommentContext("Comment delete list");
        JsonNode deleted = postComment(context.creatorToken(), context.task().getId(), "deleted comment");

        deleteComment(context.creatorToken(), deleted.path("id").asLong());

        mockMvc.perform(get("/api/tasks/{taskId}/comments", context.task().getId())
                        .header("Authorization", bearer(context.creatorToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    @DisplayName("CMT-D-06: コメント削除時に COMMENT_DELETED が記録される")
    void commentDeleteRecordsActivityLog() throws Exception {
        CommentContext context = newCommentContext("Comment delete activity");
        JsonNode created = postComment(context.creatorToken(), context.task().getId(), "before delete");

        deleteComment(context.creatorToken(), created.path("id").asLong());

        mockMvc.perform(get("/api/tasks/{taskId}/activities", context.task().getId())
                        .header("Authorization", bearer(context.creatorToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].eventType", hasItems("COMMENT_DELETED")));
    }

    @Test
    @DisplayName("ACT-06: コメント削除時に COMMENT_DELETED が記録される")
    void commentDeleteActivityLogIsRecorded() throws Exception {
        CommentContext context = newCommentContext("Comment delete activity");
        JsonNode created = postComment(context.creatorToken(), context.task().getId(), "before delete");

        deleteComment(context.creatorToken(), created.path("id").asLong());

        mockMvc.perform(get("/api/tasks/{taskId}/activities", context.task().getId())
                        .header("Authorization", bearer(context.creatorToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].eventType", hasItems("COMMENT_DELETED")));
    }

    @Test
    @DisplayName("CMT-D-07: 履歴でコメント削除イベントを確認できる")
    void commentDeleteAppearsInActivityHistory() throws Exception {
        CommentContext context = newCommentContext("Comment delete history");
        JsonNode created = postComment(context.creatorToken(), context.task().getId(), "before delete");
        long commentId = created.path("id").asLong();

        deleteComment(context.creatorToken(), commentId);

        MvcResult result = mockMvc.perform(get("/api/tasks/{taskId}/activities", context.task().getId())
                        .header("Authorization", bearer(context.creatorToken())))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode activities = objectMapper.readTree(result.getResponse().getContentAsString()).path("content");
        assertEquals(commentId, findActivityByEventType(activities, "COMMENT_DELETED").path("detailJson").path("commentId").asLong());
    }

    @Test
    @DisplayName("CMT-D-08: 削除済みコメント本文はコメント一覧・履歴一覧に表示されない")
    void deletedCommentContentIsHiddenFromCommentsAndHistory() throws Exception {
        CommentContext context = newCommentContext("Comment deleted content");
        String deletedContent = "deleted-secret-comment-body";
        JsonNode created = postComment(context.creatorToken(), context.task().getId(), deletedContent);

        deleteComment(context.creatorToken(), created.path("id").asLong());

        mockMvc.perform(get("/api/tasks/{taskId}/comments", context.task().getId())
                        .header("Authorization", bearer(context.creatorToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        MvcResult activityResult = mockMvc.perform(get("/api/tasks/{taskId}/activities", context.task().getId())
                        .header("Authorization", bearer(context.creatorToken())))
                .andExpect(status().isOk())
                .andReturn();
        assertFalse(activityResult.getResponse().getContentAsString().contains(deletedContent));
    }

    private CommentContext newCommentContext(String title) throws Exception {
        User creator = createUser("Creator", "creator@example.com", "password123");
        User assignee = createUser("Assignee", "assignee@example.com", "password123");
        Task task = createTask(title, creator, assignee, TaskStatus.TODO, Priority.HIGH);
        String creatorToken = loginAndGetToken("creator@example.com", "password123");
        String assigneeToken = loginAndGetToken("assignee@example.com", "password123");
        return new CommentContext(creator, assignee, task, creatorToken, assigneeToken);
    }

    private org.springframework.test.web.servlet.ResultActions updateComment(
            String token,
            Long commentId,
            String content,
            Long version
    ) throws Exception {
        return mockMvc.perform(put("/api/comments/{commentId}", commentId)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJson(Map.of(
                        "content", content,
                        "version", version
                ))));
    }

    private void deleteComment(String token, Long commentId) throws Exception {
        mockMvc.perform(delete("/api/comments/{commentId}", commentId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
    }

    private record CommentContext(
            User creator,
            User assignee,
            Task task,
            String creatorToken,
            String assigneeToken
    ) {
    }
}
