package com.example.task;

import com.example.task.entity.ActivityEventType;
import com.example.task.entity.ActivityLog;
import com.example.task.entity.ActivityTargetType;
import com.example.task.entity.Notification;
import com.example.task.entity.Priority;
import com.example.task.entity.Task;
import com.example.task.entity.TaskStatus;
import com.example.task.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 8. 通知機能のAPI観点を検証する。
 */
class NotificationApiIntegrationTests extends ApiIntegrationTestBase {

    @Test
    @DisplayName("CORS: 通知既読化 API の preflight が PATCH を許可する")
    void notificationReadPreflightAllowsPatch() throws Exception {
        mockMvc.perform(options("/api/notifications/{notificationId}/read", 1L)
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "PATCH"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("PATCH")));
    }

    @Test
    @DisplayName("NTF-L-01: 自分宛通知一覧を取得できる")
    void notificationListReturnsOwnNotifications() throws Exception {
        NotificationContext context = newNotificationContext("Notification list");
        createNotification(context.recipient(), createActivity(
                context.actor(),
                context.task(),
                ActivityEventType.COMMENT_CREATED,
                ActivityTargetType.COMMENT,
                101L
        ), false);
        createNotification(context.recipient(), createActivity(
                context.actor(),
                context.task(),
                ActivityEventType.ATTACHMENT_UPLOADED,
                ActivityTargetType.ATTACHMENT,
                102L
        ), false);

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", bearer(context.recipientToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].relatedTaskTitle", hasItems("Notification list")));
    }

    @Test
    @DisplayName("NTF-L-02: 通知一覧は createdAt DESC, id DESC の順で返す")
    void notificationListOrdersByCreatedAtDescThenIdDesc() throws Exception {
        NotificationContext context = newNotificationContext("Notification order");
        Notification first = createNotification(context.recipient(), createActivity(context.actor(), context.task(), ActivityEventType.TASK_UPDATED, ActivityTargetType.TASK, context.task().getId()), false);
        Notification second = createNotification(context.recipient(), createActivity(context.actor(), context.task(), ActivityEventType.COMMENT_CREATED, ActivityTargetType.COMMENT, 1001L), true);
        Notification third = createNotification(context.recipient(), createActivity(context.actor(), context.task(), ActivityEventType.ATTACHMENT_UPLOADED, ActivityTargetType.ATTACHMENT, 1002L), false);
        setCreatedAt("notifications", LocalDateTime.of(2026, 4, 21, 13, 0), first.getId(), second.getId(), third.getId());

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", bearer(context.recipientToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(third.getId()))
                .andExpect(jsonPath("$.content[1].id").value(second.getId()))
                .andExpect(jsonPath("$.content[2].id").value(first.getId()));
    }

    @Test
    @DisplayName("NTF-L-03: unreadOnly=true で未読のみ取得できる")
    void notificationListFiltersUnreadOnly() throws Exception {
        NotificationContext context = newNotificationContext("Notification unread");
        Notification unread = createNotification(context.recipient(), createActivity(context.actor(), context.task(), ActivityEventType.TASK_UPDATED, ActivityTargetType.TASK, context.task().getId()), false);
        createNotification(context.recipient(), createActivity(context.actor(), context.task(), ActivityEventType.COMMENT_CREATED, ActivityTargetType.COMMENT, 1001L), true);

        mockMvc.perform(get("/api/notifications")
                        .queryParam("unreadOnly", "true")
                        .header("Authorization", bearer(context.recipientToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(unread.getId()));
    }

    @Test
    @DisplayName("NTF-L-04: page と size に応じて通知取得結果が切り替わる")
    void notificationListPaginates() throws Exception {
        NotificationContext context = newNotificationContext("Notification pagination");
        Notification first = createNotification(context.recipient(), createActivity(context.actor(), context.task(), ActivityEventType.TASK_UPDATED, ActivityTargetType.TASK, context.task().getId()), false);
        Notification second = createNotification(context.recipient(), createActivity(context.actor(), context.task(), ActivityEventType.COMMENT_CREATED, ActivityTargetType.COMMENT, 1001L), true);
        Notification third = createNotification(context.recipient(), createActivity(context.actor(), context.task(), ActivityEventType.ATTACHMENT_UPLOADED, ActivityTargetType.ATTACHMENT, 1002L), false);
        setCreatedAt("notifications", LocalDateTime.of(2026, 4, 21, 13, 0), first.getId(), second.getId(), third.getId());

        mockMvc.perform(get("/api/notifications")
                        .queryParam("page", "0")
                        .queryParam("size", "2")
                        .header("Authorization", bearer(context.recipientToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(third.getId()))
                .andExpect(jsonPath("$.content[1].id").value(second.getId()))
                .andExpect(jsonPath("$.totalElements").value(3));

        mockMvc.perform(get("/api/notifications")
                        .queryParam("page", "1")
                        .queryParam("size", "2")
                        .header("Authorization", bearer(context.recipientToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(first.getId()));
    }

    @Test
    @DisplayName("NTF-L-05: 他人宛通知は取得されない")
    void notificationListExcludesOtherRecipientNotifications() throws Exception {
        NotificationContext context = newNotificationContext("Notification ownership");
        createNotification(context.recipient(), createActivity(context.actor(), context.task(), ActivityEventType.TASK_UPDATED, ActivityTargetType.TASK, context.task().getId()), false);
        createNotification(context.otherRecipient(), createActivity(context.actor(), context.task(), ActivityEventType.COMMENT_UPDATED, ActivityTargetType.COMMENT, 1003L), false);

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", bearer(context.recipientToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @DisplayName("AUTH-07: 自分宛通知のみ参照できる")
    void notificationListAuthorizationExcludesOtherRecipientNotifications() throws Exception {
        NotificationContext context = newNotificationContext("Notification list authorization");
        createNotification(context.recipient(), createActivity(context.actor(), context.task(), ActivityEventType.TASK_UPDATED, ActivityTargetType.TASK, context.task().getId()), false);
        createNotification(context.otherRecipient(), createActivity(context.actor(), context.task(), ActivityEventType.COMMENT_UPDATED, ActivityTargetType.COMMENT, 1003L), false);

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", bearer(context.recipientToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @DisplayName("NTF-R-01: 自分宛通知を既読化できる")
    void notificationReadSucceeds() throws Exception {
        NotificationContext context = newNotificationContext("Notification read");
        Notification notification = createNotification(context.recipient(), createActivity(context.actor(), context.task(), ActivityEventType.COMMENT_CREATED, ActivityTargetType.COMMENT, 101L), false);

        mockMvc.perform(patch("/api/notifications/{notificationId}/read", notification.getId())
                        .header("Authorization", bearer(context.recipientToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notification.getId()))
                .andExpect(jsonPath("$.isRead").value(true));
    }

    @Test
    @DisplayName("NTF-R-02: 既読済み通知への既読API再実行は正常終了し readAt を維持する")
    void notificationReadIsIdempotent() throws Exception {
        NotificationContext context = newNotificationContext("Notification idempotent read");
        Notification notification = createNotification(context.recipient(), createActivity(context.actor(), context.task(), ActivityEventType.COMMENT_CREATED, ActivityTargetType.COMMENT, 101L), true);
        Timestamp readAtBefore = jdbcTemplate.queryForObject(
                "select read_at from " + table("notifications") + " where id = ?",
                Timestamp.class,
                notification.getId()
        );

        mockMvc.perform(patch("/api/notifications/{notificationId}/read", notification.getId())
                        .header("Authorization", bearer(context.recipientToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead").value(true));

        Timestamp readAtAfter = jdbcTemplate.queryForObject(
                "select read_at from " + table("notifications") + " where id = ?",
                Timestamp.class,
                notification.getId()
        );
        assertEquals(readAtBefore, readAtAfter);
    }

    @Test
    @DisplayName("NTF-R-03: 自分宛未読通知を一括既読化できる")
    void notificationReadAllSucceeds() throws Exception {
        NotificationContext context = newNotificationContext("Notification read all");
        createNotification(context.recipient(), createActivity(context.actor(), context.task(), ActivityEventType.COMMENT_CREATED, ActivityTargetType.COMMENT, 101L), false);
        createNotification(context.recipient(), createActivity(context.actor(), context.task(), ActivityEventType.ATTACHMENT_UPLOADED, ActivityTargetType.ATTACHMENT, 102L), false);

        mockMvc.perform(patch("/api/notifications/read-all")
                        .header("Authorization", bearer(context.recipientToken())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", bearer(context.recipientToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));
    }

    @Test
    @DisplayName("NTF-R-04: 他人宛通知は既読化できない")
    void notificationReadRejectsOtherRecipientNotification() throws Exception {
        NotificationContext context = newNotificationContext("Notification read auth");
        Notification otherNotification = createNotification(
                context.otherRecipient(),
                createActivity(context.actor(), context.task(), ActivityEventType.COMMENT_UPDATED, ActivityTargetType.COMMENT, 1003L),
                false
        );

        mockMvc.perform(patch("/api/notifications/{notificationId}/read", otherNotification.getId())
                        .header("Authorization", bearer(context.recipientToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-NOTIFY-002"));
    }

    @Test
    @DisplayName("AUTH-08: 自分宛通知のみ既読化できる")
    void notificationReadAuthorizationRejectsOtherRecipientNotification() throws Exception {
        NotificationContext context = newNotificationContext("Notification read authorization");
        Notification otherNotification = createNotification(
                context.otherRecipient(),
                createActivity(context.actor(), context.task(), ActivityEventType.COMMENT_UPDATED, ActivityTargetType.COMMENT, 1003L),
                false
        );

        mockMvc.perform(patch("/api/notifications/{notificationId}/read", otherNotification.getId())
                        .header("Authorization", bearer(context.recipientToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-NOTIFY-002"));
    }

    @Test
    @DisplayName("NTF-R-05: 個別既読化時に readAt が設定される")
    void notificationReadSetsReadAt() throws Exception {
        NotificationContext context = newNotificationContext("Notification readAt");
        Notification notification = createNotification(context.recipient(), createActivity(context.actor(), context.task(), ActivityEventType.COMMENT_CREATED, ActivityTargetType.COMMENT, 101L), false);

        mockMvc.perform(patch("/api/notifications/{notificationId}/read", notification.getId())
                        .header("Authorization", bearer(context.recipientToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readAt").exists());
    }

    @Test
    @DisplayName("NTF-R-06: 既読化後に未読件数が更新される")
    void notificationReadUpdatesUnreadCount() throws Exception {
        NotificationContext context = newNotificationContext("Notification unread count");
        Notification first = createNotification(context.recipient(), createActivity(context.actor(), context.task(), ActivityEventType.COMMENT_CREATED, ActivityTargetType.COMMENT, 101L), false);
        createNotification(context.recipient(), createActivity(context.actor(), context.task(), ActivityEventType.ATTACHMENT_UPLOADED, ActivityTargetType.ATTACHMENT, 102L), false);

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", bearer(context.recipientToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(2));

        mockMvc.perform(patch("/api/notifications/{notificationId}/read", first.getId())
                        .header("Authorization", bearer(context.recipientToken())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", bearer(context.recipientToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1));
    }

    @Test
    @DisplayName("NTF-R-07: 未読通知0件でも一括既読化を再実行できる")
    void notificationReadAllIsIdempotent() throws Exception {
        NotificationContext context = newNotificationContext("Notification read all idempotent");
        createNotification(context.recipient(), createActivity(context.actor(), context.task(), ActivityEventType.COMMENT_CREATED, ActivityTargetType.COMMENT, 101L), false);

        mockMvc.perform(patch("/api/notifications/read-all")
                        .header("Authorization", bearer(context.recipientToken())))
                .andExpect(status().isNoContent());

        mockMvc.perform(patch("/api/notifications/read-all")
                        .header("Authorization", bearer(context.recipientToken())))
                .andExpect(status().isNoContent());
    }

    private NotificationContext newNotificationContext(String taskTitle) throws Exception {
        User recipient = createUser("Recipient", "recipient@example.com", "password123");
        User otherRecipient = createUser("Other Recipient", "other-recipient@example.com", "password123");
        User actor = createUser("Actor", "actor@example.com", "password123");
        Task task = createTask(taskTitle, actor, recipient, TaskStatus.TODO, Priority.HIGH);
        String recipientToken = loginAndGetToken("recipient@example.com", "password123");
        return new NotificationContext(recipient, otherRecipient, actor, task, recipientToken);
    }

    private record NotificationContext(
            User recipient,
            User otherRecipient,
            User actor,
            Task task,
            String recipientToken
    ) {
    }
}
