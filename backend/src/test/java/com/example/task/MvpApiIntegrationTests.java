package com.example.task;

import com.example.task.entity.Priority;
import com.example.task.entity.Task;
import com.example.task.entity.TaskStatus;
import com.example.task.entity.User;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MVP 段階で提供する主要 API の疎通と認可ルールをまとめて検証する。
 */
@SpringBootTest
@AutoConfigureMockMvc
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
    private PasswordEncoder passwordEncoder;

    /**
     * テスト同士が独立するよう、毎回データを全消去してから開始する。
     */
    @BeforeEach
    void setUp() {
        taskRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
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
                .andExpect(jsonPath("$.errorCode").value("VAL-INPUT-001"))
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
    @DisplayName("ログイン: 認証失敗時は AUTH-002 を返す")
    void loginFailsWithBadCredentials() throws Exception {
        createUser("Login User", "login@example.com", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of(
                                "email", "login@example.com",
                                "password", "wrongpass123"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH-002"));
    }

    @Test
    @DisplayName("認証: token なしの保護 API は AUTH-001 を返す")
    void protectedApiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH-001"));
    }

    @Test
    @DisplayName("認証: 不正 token の保護 API は AUTH-003 を返す")
    void invalidTokenReturnsAuth003() throws Exception {
        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", bearer("invalid-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH-003"));
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
                                "dueDate", "2026-04-25"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Task"))
                .andExpect(jsonPath("$.status").value("DOING"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.assignedUser").doesNotExist());

        mockMvc.perform(delete("/api/tasks/{taskId}", taskId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tasks/{taskId}", taskId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RES-TASK-404"));
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
    @DisplayName("タスク: バリデーションエラー時に VAL-TASK-001 を返す")
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
                .andExpect(jsonPath("$.errorCode").value("VAL-TASK-001"))
                .andExpect(jsonPath("$.details[*].field", hasItems("title")));
    }

    @Test
    @DisplayName("認可: 他人タスクの詳細参照は AUTH-005 を返す")
    void viewForbiddenTaskReturnsAuth005() throws Exception {
        User creator = createUser("Creator", "creator@example.com", "password123");
        User outsider = createUser("Outsider", "outsider@example.com", "password123");
        Task hiddenTask = createTask("Hidden Task", creator, null, TaskStatus.TODO, Priority.HIGH);

        String token = loginAndGetToken("outsider@example.com", "password123");

        mockMvc.perform(get("/api/tasks/{taskId}", hiddenTask.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("AUTH-005"));
    }

    @Test
    @DisplayName("認可: 他人タスクの更新は PERM-TASK-403-UPD を返す")
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
                                "priority", "LOW"
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("PERM-TASK-403-UPD"));
    }

    @Test
    @DisplayName("認可: 他人タスクの削除は PERM-TASK-403-DEL を返す")
    void deleteForbiddenTaskReturnsTaskPermissionCode() throws Exception {
        User creator = createUser("Creator", "creator@example.com", "password123");
        User outsider = createUser("Outsider", "outsider@example.com", "password123");
        Task hiddenTask = createTask("Hidden Task", creator, null, TaskStatus.TODO, Priority.HIGH);

        String token = loginAndGetToken("outsider@example.com", "password123");

        mockMvc.perform(delete("/api/tasks/{taskId}", hiddenTask.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("PERM-TASK-403-DEL"));
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
