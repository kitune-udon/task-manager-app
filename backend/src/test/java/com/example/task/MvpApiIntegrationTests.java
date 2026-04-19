package com.example.task;

import com.example.task.entity.Priority;
import com.example.task.entity.Task;
import com.example.task.entity.TaskStatus;
import com.example.task.entity.User;
import com.example.task.repository.ActivityLogRepository;
import com.example.task.repository.NotificationRepository;
import com.example.task.repository.TaskAttachmentRepository;
import com.example.task.repository.TaskCommentRepository;
import com.example.task.repository.TaskRepository;
import com.example.task.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MVP 段階で提供する主要 API の疎通と認可ルールをまとめて検証する。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MvpApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskCommentRepository taskCommentRepository;

    @Autowired
    private TaskAttachmentRepository taskAttachmentRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * テスト同士が独立するよう、毎回データを全消去してから開始する。
     */
    @BeforeEach
    void setUp() {
        notificationRepository.deleteAllInBatch();
        activityLogRepository.deleteAllInBatch();
        taskAttachmentRepository.deleteAllInBatch();
        taskCommentRepository.deleteAllInBatch();
        taskRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

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
    @DisplayName("新規登録: 正常系でユーザーを作成できる")
    void registerSucceeds() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of(
                                "name", "Test User",
                                "email", "register@example.com",
                                "password", "password123"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.email").value("register@example.com"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("新規登録: バリデーションエラー時に項目エラーを返す")
    void registerValidationFails() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of(
                                "name", "",
                                "email", "invalid-mail",
                                "password", "short"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ERR-INPUT-001"))
                .andExpect(jsonPath("$.details", hasSize(3)))
                .andExpect(jsonPath("$.details[*].field", hasItems("name", "email", "password")));
    }

    @Test
    @DisplayName("ログイン: 成功時に token と user を返し、認証確認 API が利用できる")
    void loginAndAuthMeSucceed() throws Exception {
        User user = createUser("Login User", "login@example.com", "password123");

        String token = loginAndGetToken("login@example.com", "password123");

        mockMvc.perform(get("/api/auth-test/me")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.email").value("login@example.com"));
    }

    @Test
    @DisplayName("ログイン: 認証失敗時は ERR-AUTH-002 を返す")
    void loginFailsWithBadCredentials() throws Exception {
        createUser("Login User", "login@example.com", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of(
                                "email", "login@example.com",
                                "password", "wrongpass123"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("ERR-AUTH-002"));
    }

    @Test
    @DisplayName("認証: token なしの保護 API は ERR-AUTH-001 を返す")
    void protectedApiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("ERR-AUTH-001"));
    }

    @Test
    @DisplayName("認証: 不正 token の保護 API は ERR-AUTH-003 を返す")
    void invalidTokenReturnsAuth003() throws Exception {
        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", bearer("invalid-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("ERR-AUTH-003"));
    }

    @Test
    @DisplayName("担当者候補: 認証済みユーザーは /api/users で候補一覧を取得できる")
    void getUsersReturnsCandidates() throws Exception {
        createUser("Bravo User", "bravo@example.com", "password123");
        createUser("Alpha User", "alpha@example.com", "password123");

        String token = loginAndGetToken("alpha@example.com", "password123");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Alpha User"))
                .andExpect(jsonPath("$[1].name").value("Bravo User"))
                .andExpect(jsonPath("$[0].email").value("alpha@example.com"));
    }

    @Test
    @DisplayName("タスク: 作成、詳細取得、更新、削除を一連で実行できる")
    void taskCrudFlowSucceeds() throws Exception {
        User creator = createUser("Creator", "creator@example.com", "password123");
        User assignee = createUser("Assignee", "assignee@example.com", "password123");
        String token = loginAndGetToken("creator@example.com", "password123");

        Long taskId = createTaskAndGetId(token, Map.of(
                "title", "Initial Task",
                "description", "Initial Description",
                "status", "TODO",
                "priority", "HIGH",
                "dueDate", "2026-04-20",
                "assignedUserId", assignee.getId()
        ));

        mockMvc.perform(get("/api/tasks/{taskId}", taskId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId))
                .andExpect(jsonPath("$.title").value("Initial Task"))
                .andExpect(jsonPath("$.assignedUser.id").value(assignee.getId()))
                .andExpect(jsonPath("$.createdBy.id").value(creator.getId()));

        mockMvc.perform(put("/api/tasks/{taskId}", taskId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of(
                                "title", "Updated Task",
                                "description", "Updated Description",
                                "status", "DOING",
                                "priority", "MEDIUM",
                                "dueDate", "2026-04-25",
                                "version", getTaskVersion(token, taskId)
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Task"))
                .andExpect(jsonPath("$.status").value("DOING"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.assignedUser").doesNotExist())
                .andExpect(jsonPath("$.version").value(1));

        MvcResult activityResult = mockMvc.perform(get("/api/tasks/{taskId}/activities", taskId)
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

        mockMvc.perform(delete("/api/tasks/{taskId}", taskId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tasks/{taskId}", taskId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-004"));
    }

    @Test
    @DisplayName("タスク一覧: 作成者または担当者のタスクだけが返る")
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
    @DisplayName("タスク: バリデーションエラー時に ERR-TASK-001 を返す")
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
    @DisplayName("タスク: タイトルが101文字以上の作成は ERR-TASK-001 を返す")
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
    @DisplayName("タスク: タイトルが101文字以上の更新は ERR-TASK-001 を返す")
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
    @DisplayName("認可: 他人タスクの詳細参照は ERR-AUTH-005 を返す")
    void viewForbiddenTaskReturnsAuth005() throws Exception {
        User creator = createUser("Creator", "creator@example.com", "password123");
        User outsider = createUser("Outsider", "outsider@example.com", "password123");
        Task hiddenTask = createTask("Hidden Task", creator, null, TaskStatus.TODO, Priority.HIGH);

        String token = loginAndGetToken("outsider@example.com", "password123");

        mockMvc.perform(get("/api/tasks/{taskId}", hiddenTask.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-AUTH-005"));
    }

    @Test
    @DisplayName("認可: 他人タスクの更新は ERR-TASK-005 を返す")
    void updateForbiddenTaskReturnsTaskPermissionCode() throws Exception {
        User creator = createUser("Creator", "creator@example.com", "password123");
        User outsider = createUser("Outsider", "outsider@example.com", "password123");
        Task hiddenTask = createTask("Hidden Task", creator, null, TaskStatus.TODO, Priority.HIGH);

        String token = loginAndGetToken("outsider@example.com", "password123");

        mockMvc.perform(put("/api/tasks/{taskId}", hiddenTask.getId())
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of(
                                "title", "Blocked Update",
                                "description", "No permission",
                                "status", "DOING",
                                "priority", "LOW",
                                "version", hiddenTask.getVersion()
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-005"));
    }

    @Test
    @DisplayName("タスク更新: version が不一致なら ERR-TASK-007 を返す")
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
    @DisplayName("認可: 他人タスクの削除は ERR-TASK-006 を返す")
    void deleteForbiddenTaskReturnsTaskPermissionCode() throws Exception {
        User creator = createUser("Creator", "creator@example.com", "password123");
        User outsider = createUser("Outsider", "outsider@example.com", "password123");
        Task hiddenTask = createTask("Hidden Task", creator, null, TaskStatus.TODO, Priority.HIGH);

        String token = loginAndGetToken("outsider@example.com", "password123");

        mockMvc.perform(delete("/api/tasks/{taskId}", hiddenTask.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-006"));
    }

    @Test
    @DisplayName("次フェーズ API: コメント、添付、通知、履歴を一連で利用できる")
    void nextPhaseApisSucceed() throws Exception {
        User creator = createUser("Creator", "creator@example.com", "password123");
        User assignee = createUser("Assignee", "assignee@example.com", "password123");
        String creatorToken = loginAndGetToken("creator@example.com", "password123");
        String assigneeToken = loginAndGetToken("assignee@example.com", "password123");

        Long taskId = createTaskAndGetId(creatorToken, Map.of(
                "title", "Next Phase Task",
                "description", "Task for comment and attachment flow",
                "status", "TODO",
                "priority", "HIGH",
                "dueDate", "2026-04-30",
                "assignedUserId", assignee.getId()
        ));

        MvcResult commentCreateResult = mockMvc.perform(post("/api/tasks/{taskId}/comments", taskId)
                        .header("Authorization", bearer(creatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of("content", "Initial comment"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Initial comment"))
                .andExpect(jsonPath("$.createdBy.id").value(creator.getId()))
                .andReturn();

        JsonNode createdComment = objectMapper.readTree(commentCreateResult.getResponse().getContentAsString());
        long commentId = createdComment.path("id").asLong();
        long commentVersion = createdComment.path("version").asLong();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "evidence.txt",
                "text/plain",
                "hello attachment".getBytes(StandardCharsets.UTF_8)
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/tasks/{taskId}/attachments", taskId)
                        .file(file)
                        .header("Authorization", bearer(creatorToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalFileName").value("evidence.txt"))
                .andExpect(jsonPath("$.uploadedBy.id").value(creator.getId()))
                .andReturn();

        long attachmentId = objectMapper.readTree(uploadResult.getResponse().getContentAsString()).path("id").asLong();
        assertTrue(
                taskAttachmentRepository.findById(attachmentId).orElseThrow().getStorageKey().startsWith("attachments/tasks/" + taskId + "/"),
                "storageKey should start with attachments/tasks/{taskId}/"
        );

        mockMvc.perform(get("/api/tasks/{taskId}/comments", taskId)
                        .header("Authorization", bearer(creatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].content").value("Initial comment"));

        mockMvc.perform(put("/api/comments/{commentId}", commentId)
                        .header("Authorization", bearer(creatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of(
                                "content", "Updated comment",
                                "version", commentVersion
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Updated comment"));

        mockMvc.perform(get("/api/tasks/{taskId}/attachments", taskId)
                        .header("Authorization", bearer(creatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].originalFileName").value("evidence.txt"));

        mockMvc.perform(get("/api/attachments/{attachmentId}/download", attachmentId)
                        .header("Authorization", bearer(creatorToken)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("evidence.txt")))
                .andExpect(content().bytes("hello attachment".getBytes(StandardCharsets.UTF_8)));

        MvcResult collaborationActivityResult = mockMvc.perform(get("/api/tasks/{taskId}/activities", taskId)
                        .header("Authorization", bearer(creatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].eventType", hasItems(
                        "TASK_CREATED",
                        "COMMENT_CREATED",
                        "COMMENT_UPDATED",
                        "ATTACHMENT_UPLOADED"
                )))
                .andReturn();

        JsonNode collaborationActivities = objectMapper.readTree(collaborationActivityResult.getResponse().getContentAsString()).path("content");
        assertEquals(commentId, findActivityByEventType(collaborationActivities, "COMMENT_CREATED").path("detailJson").path("commentId").asLong());
        assertEquals(commentId, findActivityByEventType(collaborationActivities, "COMMENT_UPDATED").path("detailJson").path("commentId").asLong());
        JsonNode attachmentUploadedActivity = findActivityByEventType(collaborationActivities, "ATTACHMENT_UPLOADED");
        assertEquals(attachmentId, attachmentUploadedActivity.path("detailJson").path("attachmentId").asLong());
        assertEquals("evidence.txt", attachmentUploadedActivity.path("detailJson").path("fileName").asText());

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", bearer(assigneeToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].eventType", hasItems("COMMENT_CREATED", "ATTACHMENT_UPLOADED")))
                .andExpect(jsonPath("$.content[*].relatedTaskTitle", hasItems("Next Phase Task")));

        MvcResult notificationResult = mockMvc.perform(get("/api/notifications")
                        .header("Authorization", bearer(assigneeToken)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode notifications = objectMapper.readTree(notificationResult.getResponse().getContentAsString()).path("content");
        JsonNode attachmentUploadedNotification = findNotificationByEventType(notifications, "ATTACHMENT_UPLOADED");
        assertEquals("evidence.txt", attachmentUploadedNotification.path("detailJson").path("fileName").asText());
        long notificationId = notifications.get(0).path("id").asLong();

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", bearer(assigneeToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(2));

        mockMvc.perform(patch("/api/notifications/{notificationId}/read", notificationId)
                        .header("Authorization", bearer(assigneeToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notificationId))
                .andExpect(jsonPath("$.detailJson").exists())
                .andExpect(jsonPath("$.readAt").exists())
                .andExpect(jsonPath("$.isRead").value(true));

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", bearer(assigneeToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1));

        mockMvc.perform(patch("/api/notifications/read-all")
                        .header("Authorization", bearer(assigneeToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", bearer(assigneeToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));

        mockMvc.perform(delete("/api/comments/{commentId}", commentId)
                        .header("Authorization", bearer(creatorToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/attachments/{attachmentId}", attachmentId)
                        .header("Authorization", bearer(creatorToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tasks/{taskId}/comments", taskId)
                        .header("Authorization", bearer(creatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        mockMvc.perform(get("/api/tasks/{taskId}/attachments", taskId)
                        .header("Authorization", bearer(creatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        MvcResult deletedActivityResult = mockMvc.perform(get("/api/tasks/{taskId}/activities", taskId)
                        .header("Authorization", bearer(creatorToken)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode deletedActivities = objectMapper.readTree(deletedActivityResult.getResponse().getContentAsString()).path("content");
        assertEquals(commentId, findActivityByEventType(deletedActivities, "COMMENT_DELETED").path("detailJson").path("commentId").asLong());
        JsonNode attachmentDeletedActivity = findActivityByEventType(deletedActivities, "ATTACHMENT_DELETED");
        assertEquals(attachmentId, attachmentDeletedActivity.path("detailJson").path("attachmentId").asLong());
        assertEquals("evidence.txt", attachmentDeletedActivity.path("detailJson").path("fileName").asText());
    }

    /**
     * 認証済みユーザーを直接 DB に投入するテスト用ヘルパー。
     */
    private User createUser(String name, String email, String rawPassword) {
        User user = User.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .build();
        return userRepository.save(user);
    }

    /**
     * 参照・認可テスト用のタスクデータを作成するヘルパー。
     */
    private Task createTask(String title, User createdBy, User assignedUser, TaskStatus status, Priority priority) {
        Task task = Task.builder()
                .title(title)
                .description(title + " description")
                .status(status)
                .priority(priority)
                .dueDate(LocalDate.of(2026, 4, 20))
                .assignedUser(assignedUser)
                .createdBy(createdBy)
                .build();
        return taskRepository.save(task);
    }

    /**
     * ログイン API を呼び出してレスポンスの token 文字列だけを抜き出す。
     */
    private String loginAndGetToken(String email, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of(
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return body.path("token").asText();
    }

    /**
     * タスク作成 API を呼び出し、後続テストで使う ID を返す。
     */
    private Long createTaskAndGetId(String token, Map<String, Object> payload) throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/tasks")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(payload)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(createResult.getResponse().getContentAsString());
        return body.path("id").asLong();
    }

    private long getTaskVersion(String token, Long taskId) throws Exception {
        MvcResult detailResult = mockMvc.perform(get("/api/tasks/{taskId}", taskId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(detailResult.getResponse().getContentAsString());
        return body.path("version").asLong();
    }

    private JsonNode findActivityByEventType(JsonNode activities, String eventType) {
        for (JsonNode activity : activities) {
            if (eventType.equals(activity.path("eventType").asText())) {
                return activity;
            }
        }
        fail("Activity not found for event type: " + eventType);
        return objectMapper.nullNode();
    }

    private JsonNode findNotificationByEventType(JsonNode notifications, String eventType) {
        for (JsonNode notification : notifications) {
            if (eventType.equals(notification.path("eventType").asText())) {
                return notification;
            }
        }
        fail("Notification not found for event type: " + eventType);
        return objectMapper.nullNode();
    }

    private JsonNode findChangeByField(JsonNode changes, String field) {
        for (JsonNode change : changes) {
            if (field.equals(change.path("field").asText())) {
                return change;
            }
        }
        fail("Change not found for field: " + field);
        return objectMapper.nullNode();
    }

    /**
     * Authorization ヘッダー用の Bearer 形式へ整形する。
     */
    private String bearer(String token) {
        return "Bearer " + token;
    }

    /**
     * Map などのテストデータを JSON 文字列へ変換する。
     */
    private String asJson(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
