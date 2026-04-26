package com.example.task;

import com.example.task.config.StorageProperties;
import com.example.task.entity.ActivityEventType;
import com.example.task.entity.ActivityLog;
import com.example.task.entity.ActivityTargetType;
import com.example.task.entity.AttachmentStorageType;
import com.example.task.entity.Notification;
import com.example.task.entity.Priority;
import com.example.task.entity.Task;
import com.example.task.entity.TaskAttachment;
import com.example.task.entity.TaskStatus;
import com.example.task.entity.Team;
import com.example.task.entity.TeamMember;
import com.example.task.entity.TeamRole;
import com.example.task.entity.User;
import com.example.task.repository.ActivityLogRepository;
import com.example.task.repository.NotificationRepository;
import com.example.task.repository.TaskAttachmentRepository;
import com.example.task.repository.TaskCommentRepository;
import com.example.task.repository.TaskRepository;
import com.example.task.repository.TeamMemberRepository;
import com.example.task.repository.TeamRepository;
import com.example.task.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API統合テストで共通利用するSpring設定、DB初期化、テストデータ作成ヘルパー。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class ApiIntegrationTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected TaskRepository taskRepository;

    @Autowired
    protected TeamRepository teamRepository;

    @Autowired
    protected TeamMemberRepository teamMemberRepository;

    @Autowired
    protected TaskCommentRepository taskCommentRepository;

    @Autowired
    protected TaskAttachmentRepository taskAttachmentRepository;

    @Autowired
    protected ActivityLogRepository activityLogRepository;

    @Autowired
    protected NotificationRepository notificationRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected StorageProperties storageProperties;

    @Value("${spring.jpa.properties.hibernate.default_schema}")
    protected String dbSchema;

    @BeforeEach
    void cleanDatabase() {
        notificationRepository.deleteAllInBatch();
        activityLogRepository.deleteAllInBatch();
        taskAttachmentRepository.deleteAllInBatch();
        taskCommentRepository.deleteAllInBatch();
        taskRepository.deleteAllInBatch();
        teamMemberRepository.deleteAllInBatch();
        teamRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    protected User createUser(String name, String email, String rawPassword) {
        User user = User.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .build();
        return userRepository.save(user);
    }

    protected Task createTask(String title, User createdBy, User assignedUser, TaskStatus status, Priority priority) {
        Team team = createTeamWithMember(createdBy, createdBy.getName() + "のチーム", TeamRole.OWNER);
        if (assignedUser != null) {
            addTeamMember(team, assignedUser, TeamRole.MEMBER);
        }
        return createTask(title, createdBy, assignedUser, team, status, priority);
    }

    protected Task createTask(String title, User createdBy, User assignedUser, Team team, TaskStatus status, Priority priority) {
        addTeamMember(team, createdBy, TeamRole.OWNER);
        if (assignedUser != null) {
            addTeamMember(team, assignedUser, TeamRole.MEMBER);
        }
        Task task = Task.builder()
                .title(title)
                .description(title + " description")
                .status(status)
                .priority(priority)
                .dueDate(LocalDate.of(2026, 4, 20))
                .assignedUser(assignedUser)
                .createdBy(createdBy)
                .team(team)
                .build();
        return taskRepository.saveAndFlush(task);
    }

    protected Team createTeam(User createdBy, String name) {
        return teamRepository.findByCreatedByIdAndName(createdBy.getId(), name)
                .orElseGet(() -> teamRepository.save(Team.builder()
                        .name(name)
                        .description(name + " description")
                        .createdBy(createdBy)
                        .build()));
    }

    protected Team createTeamWithMember(User user, String name, TeamRole role) {
        Team team = createTeam(user, name);
        addTeamMember(team, user, role);
        return team;
    }

    protected TeamMember addTeamMember(Team team, User user, TeamRole role) {
        return teamMemberRepository.findByTeamIdAndUserId(team.getId(), user.getId())
                .map(existing -> {
                    existing.setRole(role);
                    return teamMemberRepository.save(existing);
                })
                .orElseGet(() -> teamMemberRepository.save(TeamMember.builder()
                        .team(team)
                        .user(user)
                        .role(role)
                        .build()));
    }

    protected TaskAttachment createAttachment(Task task, User createdBy, String fileName, long fileSize) {
        TaskAttachment attachment = TaskAttachment.builder()
                .task(task)
                .originalFileName(fileName)
                .storageKey("test/" + UUID.randomUUID() + "/" + fileName)
                .contentType("text/plain")
                .fileSize(fileSize)
                .storageType(AttachmentStorageType.LOCAL)
                .createdBy(createdBy)
                .build();
        return taskAttachmentRepository.save(attachment);
    }

    protected ActivityLog createActivity(
            User actor,
            Task task,
            ActivityEventType eventType,
            ActivityTargetType targetType,
            Long targetId
    ) {
        ActivityLog activityLog = ActivityLog.builder()
                .eventType(eventType)
                .actorUser(actor)
                .targetType(targetType)
                .targetId(targetId)
                .task(task)
                .summary(eventType.name())
                .build();
        return activityLogRepository.save(activityLog);
    }

    protected Notification createNotification(User recipient, ActivityLog activityLog, boolean read) {
        Notification notification = Notification.builder()
                .recipientUser(recipient)
                .activityLog(activityLog)
                .isRead(read)
                .build();
        if (read) {
            notification.setReadAt(LocalDateTime.of(2026, 4, 20, 10, 0));
        }
        return notificationRepository.save(notification);
    }

    protected JsonNode postComment(String token, Long taskId, String content) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tasks/{taskId}/comments", taskId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of("content", content))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    protected JsonNode uploadTextAttachment(String token, Long taskId, String fileName, String content) throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/tasks/{taskId}/attachments", taskId)
                        .file(new MockMultipartFile(
                                "file",
                                fileName,
                                "text/plain",
                                content.getBytes(StandardCharsets.UTF_8)
                        ))
                        .header("Authorization", bearer(token)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    protected String loginAndGetToken(String email, String password) throws Exception {
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

    protected Long createTaskAndGetId(String token, Map<String, Object> payload) throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/tasks")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(payload)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(createResult.getResponse().getContentAsString());
        return body.path("id").asLong();
    }

    protected long getTaskVersion(String token, Long taskId) throws Exception {
        MvcResult detailResult = mockMvc.perform(get("/api/tasks/{taskId}", taskId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(detailResult.getResponse().getContentAsString());
        return body.path("version").asLong();
    }

    protected JsonNode findActivityByEventType(JsonNode activities, String eventType) {
        for (JsonNode activity : activities) {
            if (eventType.equals(activity.path("eventType").asText())) {
                return activity;
            }
        }
        fail("Activity not found for event type: " + eventType);
        return objectMapper.nullNode();
    }

    protected JsonNode findNotificationByEventType(JsonNode notifications, String eventType) {
        for (JsonNode notification : notifications) {
            if (eventType.equals(notification.path("eventType").asText())) {
                return notification;
            }
        }
        fail("Notification not found for event type: " + eventType);
        return objectMapper.nullNode();
    }

    protected JsonNode findChangeByField(JsonNode changes, String field) {
        for (JsonNode change : changes) {
            if (field.equals(change.path("field").asText())) {
                return change;
            }
        }
        fail("Change not found for field: " + field);
        return objectMapper.nullNode();
    }

    protected void setCreatedAt(String tableName, LocalDateTime createdAt, Long... ids) {
        for (Long id : ids) {
            jdbcTemplate.update(
                    "update " + table(tableName) + " set created_at = ? where id = ?",
                    Timestamp.valueOf(createdAt),
                    id
            );
        }
    }

    protected long countRows(String tableName) {
        Long count = jdbcTemplate.queryForObject("select count(*) from " + table(tableName), Long.class);
        return count != null ? count : 0L;
    }

    protected String table(String tableName) {
        return dbSchema + "." + tableName;
    }

    protected Path localAttachmentPath(String storageKey) {
        return Path.of(storageProperties.getLocalBasePath()).resolve(storageKey);
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }

    protected String asJson(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
