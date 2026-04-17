package com.example.task.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.task.entity.User;
import com.example.task.repository.ActivityLogRepository;
import com.example.task.repository.NotificationRepository;
import com.example.task.repository.TaskAttachmentRepository;
import com.example.task.repository.TaskCommentRepository;
import com.example.task.repository.TaskRepository;
import com.example.task.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 構造化ログのイベントID、logger名、重複抑止を API 経由で確認する。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(LoggingEventIntegrationTests.LoggingTestConfiguration.class)
class LoggingEventIntegrationTests {

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

    private Logger applicationLogger;
    private Logger securityLogger;
    private Logger auditLogger;
    private ListAppender<ILoggingEvent> applicationAppender;
    private ListAppender<ILoggingEvent> securityAppender;
    private ListAppender<ILoggingEvent> auditAppender;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAllInBatch();
        activityLogRepository.deleteAllInBatch();
        taskAttachmentRepository.deleteAllInBatch();
        taskCommentRepository.deleteAllInBatch();
        taskRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        attachAppenders();
    }

    @AfterEach
    void tearDown() {
        detachAppender(applicationLogger, applicationAppender);
        detachAppender(securityLogger, securityAppender);
        detachAppender(auditLogger, auditAppender);
    }

    @Test
    @DisplayName("登録バリデーションエラーは LOG-AUTH-005 を出し、LOG-SYS-002 を重複出力しない")
    void registerValidationFailureEmitsSecurityEventWithoutSystem4xx() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of(
                                "name", "",
                                "email", "invalid-mail",
                                "password", "short"
                        ))))
                .andExpect(status().isBadRequest())
                .andReturn();

        String requestId = result.getResponse().getHeader("X-Request-Id");
        CapturedLog registerFailure = findRequiredEvent(securityAppender, "LOG-AUTH-005");
        CapturedLog accessLog = findRequiredEvent(applicationAppender, "LOG-REQ-001");

        assertThat(requestId).isNotBlank();
        assertThat(registerFailure.loggerName()).isEqualTo("security");
        assertThat(registerFailure.payload().path("requestId").asText()).isEqualTo(requestId);
        assertThat(registerFailure.payload().path("status").asInt()).isEqualTo(400);
        assertThat(registerFailure.payload().path("errorCode").asText()).isEqualTo("ERR-INPUT-001");
        assertThat(countEvents(securityAppender, "LOG-AUTH-005")).isEqualTo(1);
        assertThat(countEvents(applicationAppender, "LOG-SYS-002")).isZero();
        assertThat(countEvents(applicationAppender, "LOG-REQ-001")).isEqualTo(1);
        assertThat(accessLog.payload().path("requestId").asText()).isEqualTo(requestId);
        assertThat(accessLog.payload().path("status").asInt()).isEqualTo(400);
    }

    @Test
    @DisplayName("不正JWTは LOG-AUTH-003 を出し、LOG-SYS-002 を重複出力しない")
    void invalidTokenEmitsJwtFailureWithoutSystem4xx() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/tasks")
                        .header("Authorization", bearer("invalid-token")))
                .andExpect(status().isUnauthorized())
                .andReturn();

        String requestId = result.getResponse().getHeader("X-Request-Id");
        CapturedLog jwtFailure = findRequiredEvent(securityAppender, "LOG-AUTH-003");
        CapturedLog accessLog = findRequiredEvent(applicationAppender, "LOG-REQ-001");

        assertThat(jwtFailure.loggerName()).isEqualTo("security");
        assertThat(jwtFailure.payload().path("requestId").asText()).isEqualTo(requestId);
        assertThat(jwtFailure.payload().path("status").asInt()).isEqualTo(401);
        assertThat(jwtFailure.payload().path("errorCode").asText()).isEqualTo("ERR-AUTH-003");
        assertThat(countEvents(securityAppender, "LOG-AUTH-003")).isEqualTo(1);
        assertThat(countEvents(applicationAppender, "LOG-SYS-002")).isZero();
        assertThat(countEvents(applicationAppender, "LOG-REQ-001")).isEqualTo(1);
        assertThat(accessLog.payload().path("requestId").asText()).isEqualTo(requestId);
        assertThat(accessLog.payload().path("status").asInt()).isEqualTo(401);
    }

    @Test
    @DisplayName("404業務エラーは LOG-SYS-002 の後に LOG-REQ-001 を出す")
    void missingTaskEmitsSystem4xxBeforeAccessLog() throws Exception {
        createUser("Viewer", "viewer@example.com", "password123");
        String token = loginAndGetToken("viewer@example.com", "password123");
        clearCapturedLogs();

        MvcResult result = mockMvc.perform(get("/api/tasks/{taskId}", 999999L)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andReturn();

        String requestId = result.getResponse().getHeader("X-Request-Id");
        List<CapturedLog> applicationEvents = capturedEvents(applicationAppender);
        CapturedLog system4xx = findRequiredEvent(applicationAppender, "LOG-SYS-002");
        CapturedLog accessLog = findRequiredEvent(applicationAppender, "LOG-REQ-001");

        assertThat(system4xx.loggerName()).isEqualTo("application");
        assertThat(system4xx.payload().path("requestId").asText()).isEqualTo(requestId);
        assertThat(system4xx.payload().path("status").asInt()).isEqualTo(404);
        assertThat(system4xx.payload().path("errorCode").asText()).isEqualTo("ERR-TASK-004");
        assertThat(countEvents(applicationAppender, "LOG-SYS-002")).isEqualTo(1);
        assertThat(countEvents(applicationAppender, "LOG-REQ-001")).isEqualTo(1);
        assertThat(indexOfEvent(applicationEvents, "LOG-SYS-002"))
                .isLessThan(indexOfEvent(applicationEvents, "LOG-REQ-001"));
        assertThat(accessLog.payload().path("requestId").asText()).isEqualTo(requestId);
    }

    @Test
    @DisplayName("タスク作成成功は audit logger に LOG-TASK-001 を出す")
    void createTaskEmitsAuditEvent() throws Exception {
        User creator = createUser("Creator", "creator@example.com", "password123");
        String token = loginAndGetToken("creator@example.com", "password123");
        clearCapturedLogs();

        MvcResult result = mockMvc.perform(post("/api/tasks")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of(
                                "title", "Log Test Task",
                                "description", "Created for logging verification",
                                "status", "TODO",
                                "priority", "HIGH",
                                "dueDate", "2026-04-20"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        String requestId = result.getResponse().getHeader("X-Request-Id");
        CapturedLog taskCreated = findRequiredEvent(auditAppender, "LOG-TASK-001");

        assertThat(taskCreated.loggerName()).isEqualTo("audit");
        assertThat(taskCreated.payload().path("requestId").asText()).isEqualTo(requestId);
        assertThat(taskCreated.payload().path("status").asInt()).isEqualTo(201);
        assertThat(taskCreated.payload().path("userId").asLong()).isEqualTo(creator.getId());
        assertThat(taskCreated.payload().path("taskId").asLong()).isPositive();
        assertThat(countEvents(auditAppender, "LOG-TASK-001")).isEqualTo(1);
    }

    @Test
    @DisplayName("5xx 例外ログは既定設定では stackTrace を出さない")
    void unexpectedExceptionOmitsStackTraceByDefault() throws Exception {
        createUser("Boom User", "boom@example.com", "password123");
        String token = loginAndGetToken("boom@example.com", "password123");
        clearCapturedLogs();

        MvcResult result = mockMvc.perform(get("/api/test/logging/runtime-error")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isInternalServerError())
                .andReturn();

        String requestId = result.getResponse().getHeader("X-Request-Id");
        CapturedLog system5xx = findRequiredEvent(applicationAppender, "LOG-SYS-001");

        assertThat(system5xx.payload().path("requestId").asText()).isEqualTo(requestId);
        assertThat(system5xx.payload().path("status").asInt()).isEqualTo(500);
        assertThat(system5xx.payload().path("errorCode").asText()).isEqualTo("ERR-SYS-999");
        assertThat(system5xx.payload().path("exceptionClass").asText()).isEqualTo(IllegalStateException.class.getName());
        assertThat(system5xx.payload().has("stackTrace")).isFalse();
        assertThat(countEvents(applicationAppender, "LOG-SYS-001")).isEqualTo(1);
    }

    private void attachAppenders() {
        applicationLogger = (Logger) LoggerFactory.getLogger("application");
        securityLogger = (Logger) LoggerFactory.getLogger("security");
        auditLogger = (Logger) LoggerFactory.getLogger("audit");

        applicationAppender = createAppender("application-test");
        securityAppender = createAppender("security-test");
        auditAppender = createAppender("audit-test");

        applicationLogger.addAppender(applicationAppender);
        securityLogger.addAppender(securityAppender);
        auditLogger.addAppender(auditAppender);
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
        securityAppender.list.clear();
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

    private int indexOfEvent(List<CapturedLog> events, String eventId) {
        for (int i = 0; i < events.size(); i++) {
            if (eventId.equals(events.get(i).payload().path("eventId").asText())) {
                return i;
            }
        }
        return -1;
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

    private User createUser(String name, String email, String rawPassword) {
        User user = User.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .build();
        return userRepository.save(user);
    }

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

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String asJson(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private record CapturedLog(String loggerName, JsonNode payload) {
    }

    @TestConfiguration
    static class LoggingTestConfiguration {

        @Bean
        LoggingTestController loggingTestController() {
            return new LoggingTestController();
        }
    }

    @RestController
    static class LoggingTestController {

        @GetMapping("/api/test/logging/runtime-error")
        Map<String, Object> runtimeError() {
            throw new IllegalStateException("Unexpected logging test failure");
        }
    }
}
