package com.example.task;

import com.example.task.config.StorageProperties;
import com.example.task.entity.AttachmentStorageType;
import com.example.task.entity.Priority;
import com.example.task.entity.Task;
import com.example.task.entity.TaskAttachment;
import com.example.task.entity.TaskStatus;
import com.example.task.entity.User;
import com.example.task.exception.ErrorCode;
import com.example.task.exception.StorageException;
import com.example.task.logging.StructuredLogService;
import com.example.task.repository.TaskAttachmentRepository;
import com.example.task.repository.UserRepository;
import com.example.task.security.CurrentUserProvider;
import com.example.task.service.ActivityNotificationService;
import com.example.task.service.AttachmentService;
import com.example.task.service.TaskAuthorizationService;
import com.example.task.service.storage.AttachmentStorageService;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 7. 添付ファイル機能のAPI観点を検証する。
 */
class AttachmentApiIntegrationTests extends ApiIntegrationTestBase {

    @Test
    @DisplayName("ATT-L-01: 添付一覧を取得できる")
    void attachmentListReturnsAttachments() throws Exception {
        AttachmentContext context = newAttachmentContext("Attachment list");
        JsonNode uploaded = uploadTextAttachment(context.creatorToken(), context.task().getId(), "evidence.txt", "hello attachment");

        mockMvc.perform(get("/api/tasks/{taskId}/attachments", context.task().getId())
                        .header("Authorization", bearer(context.creatorToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(uploaded.path("id").asLong()))
                .andExpect(jsonPath("$[0].originalFileName").value("evidence.txt"));
    }

    @Test
    @DisplayName("ATT-L-02: 添付一覧は createdAt DESC, id DESC の順で返す")
    void attachmentListOrdersByCreatedAtDescThenIdDesc() throws Exception {
        AttachmentContext context = newAttachmentContext("Attachment order");
        JsonNode first = uploadTextAttachment(context.creatorToken(), context.task().getId(), "first.txt", "first");
        JsonNode second = uploadTextAttachment(context.creatorToken(), context.task().getId(), "second.txt", "second");
        setCreatedAt("task_attachments", LocalDateTime.of(2026, 4, 21, 12, 0), first.path("id").asLong(), second.path("id").asLong());

        mockMvc.perform(get("/api/tasks/{taskId}/attachments", context.task().getId())
                        .header("Authorization", bearer(context.creatorToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(second.path("id").asLong()))
                .andExpect(jsonPath("$[1].id").value(first.path("id").asLong()));
    }

    @Test
    @DisplayName("ATT-L-03: 削除済み添付は一覧から除外される")
    void attachmentListExcludesDeletedAttachments() throws Exception {
        AttachmentContext context = newAttachmentContext("Attachment deleted list");
        JsonNode deleted = uploadTextAttachment(context.creatorToken(), context.task().getId(), "deleted.txt", "deleted");
        JsonNode active = uploadTextAttachment(context.creatorToken(), context.task().getId(), "active.txt", "active");

        deleteAttachment(context.creatorToken(), deleted.path("id").asLong());

        mockMvc.perform(get("/api/tasks/{taskId}/attachments", context.task().getId())
                        .header("Authorization", bearer(context.creatorToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(active.path("id").asLong()));
    }

    @Test
    @DisplayName("ATT-D-05: 添付削除後に一覧へ表示されない")
    void deletedAttachmentIsHiddenFromList() throws Exception {
        AttachmentContext context = newAttachmentContext("Attachment deleted list");
        JsonNode deleted = uploadTextAttachment(context.creatorToken(), context.task().getId(), "deleted.txt", "deleted");
        JsonNode active = uploadTextAttachment(context.creatorToken(), context.task().getId(), "active.txt", "active");

        deleteAttachment(context.creatorToken(), deleted.path("id").asLong());

        mockMvc.perform(get("/api/tasks/{taskId}/attachments", context.task().getId())
                        .header("Authorization", bearer(context.creatorToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(active.path("id").asLong()));
    }

    @Test
    @DisplayName("ATT-L-04: 添付0件時は空配列を返す")
    void attachmentListReturnsEmptyArray() throws Exception {
        AttachmentContext context = newAttachmentContext("No attachments");

        mockMvc.perform(get("/api/tasks/{taskId}/attachments", context.task().getId())
                        .header("Authorization", bearer(context.creatorToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("ATT-L-05: 対象タスクを参照できない場合は添付一覧を取得できない")
    void attachmentListRejectsForbiddenTask() throws Exception {
        AttachmentContext context = newAttachmentContext("Attachment list auth");

        mockMvc.perform(get("/api/tasks/{taskId}/attachments", context.task().getId())
                        .header("Authorization", bearer(context.outsiderToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-009"));
    }

    @Test
    @DisplayName("ATT-C-01: 許可ファイルをアップロードできる")
    void attachmentUploadSucceeds() throws Exception {
        AttachmentContext context = newAttachmentContext("Attachment upload");

        mockMvc.perform(multipart("/api/tasks/{taskId}/attachments", context.task().getId())
                        .file(new MockMultipartFile("file", "evidence.txt", "text/plain", "hello".getBytes()))
                        .header("Authorization", bearer(context.creatorToken())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalFileName").value("evidence.txt"));
    }

    @Test
    @DisplayName("ATT-C-02: ローカルストレージ代替に添付実体が保存される")
    void attachmentUploadStoresObject() throws Exception {
        AttachmentContext context = newAttachmentContext("Attachment storage");

        JsonNode uploaded = uploadTextAttachment(context.creatorToken(), context.task().getId(), "stored.txt", "stored content");
        String storageKey = taskAttachmentRepository.findById(uploaded.path("id").asLong()).orElseThrow().getStorageKey();

        assertEquals("stored content", Files.readString(localAttachmentPath(storageKey)));
    }

    @Test
    @DisplayName("ATT-C-03: アップロード時に task_attachments へメタ情報が登録される")
    void attachmentUploadRegistersMetadata() throws Exception {
        AttachmentContext context = newAttachmentContext("Attachment metadata");

        JsonNode uploaded = uploadTextAttachment(context.creatorToken(), context.task().getId(), "metadata.txt", "metadata");
        TaskAttachment attachment = taskAttachmentRepository.findById(uploaded.path("id").asLong()).orElseThrow();

        assertEquals(context.task().getId(), attachment.getTask().getId());
        assertEquals("metadata.txt", attachment.getOriginalFileName());
        assertEquals("text/plain", attachment.getContentType());
        assertEquals(8L, attachment.getFileSize());
    }

    @Test
    @DisplayName("ATT-C-04: storage_key は attachments/tasks/{taskId}/ で始まる一意キーになる")
    void attachmentUploadGeneratesStorageKey() throws Exception {
        AttachmentContext context = newAttachmentContext("Attachment storage key");

        JsonNode uploaded = uploadTextAttachment(context.creatorToken(), context.task().getId(), "key.txt", "key");
        String storageKey = taskAttachmentRepository.findById(uploaded.path("id").asLong()).orElseThrow().getStorageKey();

        assertTrue(storageKey.startsWith("attachments/tasks/" + context.task().getId() + "/"));
    }

    @Test
    @DisplayName("ATT-C-05: 元ファイル名が保持される")
    void attachmentUploadKeepsOriginalFileName() throws Exception {
        AttachmentContext context = newAttachmentContext("Attachment original name");

        JsonNode uploaded = uploadTextAttachment(context.creatorToken(), context.task().getId(), "original-name.txt", "name");

        assertEquals("original-name.txt", uploaded.path("originalFileName").asText());
    }

    @Test
    @DisplayName("ATT-C-06: 10MB以内ならアップロードできる")
    void attachmentUploadAllowsMaximumSize() throws Exception {
        AttachmentContext context = newAttachmentContext("Attachment max size");

        mockMvc.perform(multipart("/api/tasks/{taskId}/attachments", context.task().getId())
                        .file(new MockMultipartFile(
                                "file",
                                "max.zip",
                                "application/zip",
                                new byte[10 * 1024 * 1024]
                        ))
                        .header("Authorization", bearer(context.creatorToken())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileSize").value(10 * 1024 * 1024));
    }

    @Test
    @DisplayName("ATT-C-07: 10MB超過では ERR-FILE-005 を返す")
    void attachmentUploadRejectsTooLargeFile() throws Exception {
        AttachmentContext context = newAttachmentContext("Attachment too large");

        mockMvc.perform(multipart("/api/tasks/{taskId}/attachments", context.task().getId())
                        .file(new MockMultipartFile(
                                "file",
                                "too-large.zip",
                                "application/zip",
                                new byte[(10 * 1024 * 1024) + 1]
                        ))
                        .header("Authorization", bearer(context.creatorToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ERR-FILE-005"));
    }

    @Test
    @DisplayName("ATT-C-08: 許可外拡張子では ERR-FILE-006 を返す")
    void attachmentUploadRejectsBlockedExtension() throws Exception {
        AttachmentContext context = newAttachmentContext("Attachment extension");

        mockMvc.perform(multipart("/api/tasks/{taskId}/attachments", context.task().getId())
                        .file(new MockMultipartFile("file", "blocked.exe", "application/zip", new byte[]{1}))
                        .header("Authorization", bearer(context.creatorToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ERR-FILE-006"));
    }

    @Test
    @DisplayName("ATT-C-09: 許可外MIMEタイプでは ERR-FILE-006 を返す")
    void attachmentUploadRejectsBlockedMimeType() throws Exception {
        AttachmentContext context = newAttachmentContext("Attachment MIME");

        mockMvc.perform(multipart("/api/tasks/{taskId}/attachments", context.task().getId())
                        .file(new MockMultipartFile("file", "blocked.txt", "application/octet-stream", new byte[]{1}))
                        .header("Authorization", bearer(context.creatorToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ERR-FILE-006"));
    }

    @Test
    @DisplayName("ATT-C-10: 1タスク20件超過では ERR-FILE-007 を返す")
    void attachmentUploadRejectsCountLimit() throws Exception {
        AttachmentContext context = newAttachmentContext("Attachment count limit");
        for (int index = 0; index < 20; index++) {
            createAttachment(context.task(), context.creator(), "existing-" + index + ".txt", 1L);
        }

        mockMvc.perform(multipart("/api/tasks/{taskId}/attachments", context.task().getId())
                        .file(new MockMultipartFile("file", "extra.txt", "text/plain", new byte[]{1}))
                        .header("Authorization", bearer(context.creatorToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ERR-FILE-007"));
    }

    @Test
    @DisplayName("ATT-C-11: 1タスク100MB超過では ERR-FILE-008 を返す")
    void attachmentUploadRejectsTotalSizeLimit() throws Exception {
        AttachmentContext context = newAttachmentContext("Attachment total size limit");
        createAttachment(context.task(), context.creator(), "huge.txt", (100L * 1024 * 1024) - 1L);

        mockMvc.perform(multipart("/api/tasks/{taskId}/attachments", context.task().getId())
                        .file(new MockMultipartFile("file", "overflow.txt", "text/plain", new byte[]{1, 2}))
                        .header("Authorization", bearer(context.creatorToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ERR-FILE-008"));
    }

    @Test
    @DisplayName("ATT-C-12: 更新権限がないタスクにはアップロードできない")
    void attachmentUploadRejectsForbiddenTask() throws Exception {
        AttachmentContext context = newAttachmentContext("Attachment upload auth");

        mockMvc.perform(multipart("/api/tasks/{taskId}/attachments", context.task().getId())
                        .file(new MockMultipartFile("file", "blocked.txt", "text/plain", new byte[]{1}))
                        .header("Authorization", bearer(context.outsiderToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-009"));
    }

    @Test
    @DisplayName("ATT-C-13: 添付追加時に ATTACHMENT_UPLOADED が記録される")
    void attachmentUploadRecordsActivityLog() throws Exception {
        AttachmentContext context = newAttachmentContext("Attachment upload activity");

        uploadTextAttachment(context.creatorToken(), context.task().getId(), "activity.txt", "activity");

        mockMvc.perform(get("/api/tasks/{taskId}/activities", context.task().getId())
                        .header("Authorization", bearer(context.creatorToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].eventType", hasItems("ATTACHMENT_UPLOADED")));
    }

    @Test
    @DisplayName("ACT-07: 添付追加時に ATTACHMENT_UPLOADED が記録される")
    void attachmentUploadActivityLogIsRecorded() throws Exception {
        AttachmentContext context = newAttachmentContext("Attachment upload activity");

        uploadTextAttachment(context.creatorToken(), context.task().getId(), "activity.txt", "activity");

        mockMvc.perform(get("/api/tasks/{taskId}/activities", context.task().getId())
                        .header("Authorization", bearer(context.creatorToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].eventType", hasItems("ATTACHMENT_UPLOADED")));
    }

    @Test
    @DisplayName("ATT-C-14: 添付追加時に対象関係者へ通知が生成される")
    void attachmentUploadGeneratesNotification() throws Exception {
        AttachmentContext context = newAttachmentContext("Attachment notification");

        uploadTextAttachment(context.creatorToken(), context.task().getId(), "notice.txt", "notice");

        assertEquals(1L, jdbcTemplate.queryForObject(
                "select count(*) from " + table("notifications") + " where recipient_user_id = ?",
                Long.class,
                context.assignee().getId()
        ));
    }

    @Test
    @DisplayName("ATT-C-15: 添付通知で操作者本人を除外する")
    void attachmentNotificationsExcludeActor() throws Exception {
        AttachmentContext context = newAttachmentContext("Notification target");

        uploadTextAttachment(context.creatorToken(), context.task().getId(), "notice.txt", "notice");

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
    @DisplayName("ATT-G-01: 添付をダウンロードできる")
    void attachmentDownloadSucceeds() throws Exception {
        AttachmentContext context = newAttachmentContext("Attachment download");
        JsonNode uploaded = uploadTextAttachment(context.creatorToken(), context.task().getId(), "evidence.txt", "hello attachment");

        mockMvc.perform(get("/api/attachments/{attachmentId}/download", uploaded.path("id").asLong())
                        .header("Authorization", bearer(context.creatorToken())))
                .andExpect(status().isOk())
                .andExpect(content().bytes("hello attachment".getBytes()));
    }

    @Test
    @DisplayName("ATT-G-02: ダウンロード時に元ファイル名が維持される")
    void attachmentDownloadKeepsOriginalFileName() throws Exception {
        AttachmentContext context = newAttachmentContext("Attachment download name");
        JsonNode uploaded = uploadTextAttachment(context.creatorToken(), context.task().getId(), "evidence.txt", "hello attachment");

        mockMvc.perform(get("/api/attachments/{attachmentId}/download", uploaded.path("id").asLong())
                        .header("Authorization", bearer(context.creatorToken())))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("evidence.txt")));
    }

    @Test
    @DisplayName("ATT-G-03: 参照権限がない場合は添付をダウンロードできない")
    void attachmentDownloadRejectsForbiddenTask() throws Exception {
        AttachmentContext context = newAttachmentContext("Attachment download auth");
        JsonNode uploaded = uploadTextAttachment(context.creatorToken(), context.task().getId(), "secret.txt", "secret");

        mockMvc.perform(get("/api/attachments/{attachmentId}/download", uploaded.path("id").asLong())
                        .header("Authorization", bearer(context.outsiderToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-TASK-009"));
    }

    @Test
    @DisplayName("ATT-G-04: 論理削除済み添付はダウンロードできない")
    void deletedAttachmentCannotBeDownloaded() throws Exception {
        AttachmentContext context = newAttachmentContext("Attachment deleted download");
        JsonNode uploaded = uploadTextAttachment(context.creatorToken(), context.task().getId(), "deleted.txt", "deleted");
        deleteAttachment(context.creatorToken(), uploaded.path("id").asLong());

        mockMvc.perform(get("/api/attachments/{attachmentId}/download", uploaded.path("id").asLong())
                        .header("Authorization", bearer(context.creatorToken())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ERR-FILE-002"));
    }

    @Test
    @DisplayName("ATT-G-05: ストレージ実体なしでは ERR-FILE-010 を返す")
    void missingStoredAttachmentCannotBeDownloaded() throws Exception {
        AttachmentContext context = newAttachmentContext("Attachment missing storage");
        JsonNode uploaded = uploadTextAttachment(context.creatorToken(), context.task().getId(), "missing.txt", "missing");
        String storageKey = taskAttachmentRepository.findById(uploaded.path("id").asLong()).orElseThrow().getStorageKey();
        Files.deleteIfExists(localAttachmentPath(storageKey));

        mockMvc.perform(get("/api/attachments/{attachmentId}/download", uploaded.path("id").asLong())
                        .header("Authorization", bearer(context.creatorToken())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("ERR-FILE-010"));
    }

    @Test
    @DisplayName("ATT-D-01: 添付登録者本人が削除できる")
    void attachmentDeleteSucceeds() throws Exception {
        AttachmentContext context = newAttachmentContext("Attachment delete");
        JsonNode uploaded = uploadTextAttachment(context.creatorToken(), context.task().getId(), "delete.txt", "delete");

        deleteAttachment(context.creatorToken(), uploaded.path("id").asLong());
    }

    @Test
    @DisplayName("ATT-D-03: 他人添付は削除できない")
    void attachmentDeleteRejectsOtherMember() throws Exception {
        AttachmentContext context = newAttachmentContext("Attachment delete auth");
        JsonNode uploaded = uploadTextAttachment(context.creatorToken(), context.task().getId(), "delete.txt", "delete");

        mockMvc.perform(delete("/api/attachments/{attachmentId}", uploaded.path("id").asLong())
                        .header("Authorization", bearer(context.assigneeToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-FILE-004"));
    }

    @Test
    @DisplayName("AUTH-06: 添付登録者本人以外の MEMBER は添付削除できない")
    void attachmentDeleteAuthorizationRejectsOtherMember() throws Exception {
        AttachmentContext context = newAttachmentContext("Attachment delete authorization");
        JsonNode uploaded = uploadTextAttachment(context.creatorToken(), context.task().getId(), "delete.txt", "delete");

        mockMvc.perform(delete("/api/attachments/{attachmentId}", uploaded.path("id").asLong())
                        .header("Authorization", bearer(context.assigneeToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR-FILE-004"));
    }

    @Test
    @DisplayName("ATT-D-04: 添付削除時に deleted_at と deleted_by が設定される")
    void attachmentDeleteSetsLogicalDeletionFields() throws Exception {
        AttachmentContext context = newAttachmentContext("Attachment logical delete");
        JsonNode uploaded = uploadTextAttachment(context.creatorToken(), context.task().getId(), "delete.txt", "delete");
        long attachmentId = uploaded.path("id").asLong();

        deleteAttachment(context.creatorToken(), attachmentId);

        assertNotNull(jdbcTemplate.queryForObject(
                "select deleted_at from " + table("task_attachments") + " where id = ?",
                Timestamp.class,
                attachmentId
        ));
        assertEquals(context.creator().getId(), jdbcTemplate.queryForObject(
                "select deleted_by from " + table("task_attachments") + " where id = ?",
                Long.class,
                attachmentId
        ));
    }

    @Test
    @DisplayName("ATT-D-06: 本フェーズでは削除後もストレージオブジェクトを残置する")
    void attachmentDeleteKeepsStoredObject() throws Exception {
        AttachmentContext context = newAttachmentContext("Attachment storage remains");
        JsonNode uploaded = uploadTextAttachment(context.creatorToken(), context.task().getId(), "remain.txt", "remain");
        String storageKey = taskAttachmentRepository.findById(uploaded.path("id").asLong()).orElseThrow().getStorageKey();
        Path storagePath = localAttachmentPath(storageKey);

        deleteAttachment(context.creatorToken(), uploaded.path("id").asLong());

        assertTrue(Files.exists(storagePath));
    }

    @Test
    @DisplayName("ATT-D-07: 添付削除時に ATTACHMENT_DELETED が記録される")
    void attachmentDeleteRecordsActivityLog() throws Exception {
        AttachmentContext context = newAttachmentContext("Attachment delete activity");
        JsonNode uploaded = uploadTextAttachment(context.creatorToken(), context.task().getId(), "delete.txt", "delete");

        deleteAttachment(context.creatorToken(), uploaded.path("id").asLong());

        mockMvc.perform(get("/api/tasks/{taskId}/activities", context.task().getId())
                        .header("Authorization", bearer(context.creatorToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].eventType", hasItems("ATTACHMENT_DELETED")));
    }

    @Test
    @DisplayName("ACT-08: 添付削除時に ATTACHMENT_DELETED が記録される")
    void attachmentDeleteActivityLogIsRecorded() throws Exception {
        AttachmentContext context = newAttachmentContext("Attachment delete activity");
        JsonNode uploaded = uploadTextAttachment(context.creatorToken(), context.task().getId(), "delete.txt", "delete");

        deleteAttachment(context.creatorToken(), uploaded.path("id").asLong());

        mockMvc.perform(get("/api/tasks/{taskId}/activities", context.task().getId())
                        .header("Authorization", bearer(context.creatorToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].eventType", hasItems("ATTACHMENT_DELETED")));
    }

    @Test
    @DisplayName("ATT-R-01: DB登録失敗時に保存済みオブジェクトを補償削除する")
    void deletesStoredObjectWhenMetadataSaveFails() {
        AttachmentServiceFixture fixture = newAttachmentServiceFixture();
        MockMultipartFile file = textFile("db-failure.txt");
        RuntimeException saveFailure = new RuntimeException("metadata save failed");
        when(fixture.taskAttachmentRepository().save(any(TaskAttachment.class))).thenThrow(saveFailure);

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> fixture.attachmentService().uploadAttachment(fixture.task().getId(), file)
        );

        assertSame(saveFailure, thrown);
        ArgumentCaptor<String> storageKey = ArgumentCaptor.forClass(String.class);
        verify(fixture.attachmentStorageService()).store(storageKey.capture(), eq(file));
        verify(fixture.attachmentStorageService()).delete(storageKey.getValue());
    }

    @Test
    @DisplayName("ATT-R-02: 履歴作成失敗時に保存済みオブジェクトを補償削除する")
    void deletesStoredObjectWhenActivityLogCreationFails() {
        assertUploadFailureDeletesStoredObject("activity log failed");
    }

    @Test
    @DisplayName("ATT-R-03: 通知作成失敗時に保存済みオブジェクトを補償削除する")
    void deletesStoredObjectWhenNotificationCreationFails() {
        assertUploadFailureDeletesStoredObject("notification failed");
    }

    @Test
    @DisplayName("ATT-R-04: ダウンロード時のストレージ取得失敗はファイル取得失敗として扱う")
    void downloadPropagatesStorageLoadFailureWithoutAuditLog() {
        AttachmentServiceFixture fixture = newAttachmentServiceFixture();
        TaskAttachment attachment = TaskAttachment.builder()
                .id(20L)
                .task(fixture.task())
                .originalFileName("missing.txt")
                .storageKey("attachments/tasks/10/missing.txt")
                .contentType("text/plain")
                .fileSize(7L)
                .storageType(AttachmentStorageType.LOCAL)
                .createdBy(fixture.actor())
                .build();
        StorageException loadFailure = new StorageException(ErrorCode.FILE_010, "load failed");
        when(fixture.taskAttachmentRepository().findByIdAndDeletedAtIsNull(attachment.getId()))
                .thenReturn(Optional.of(attachment));
        doThrow(loadFailure).when(fixture.attachmentStorageService()).load(attachment.getStorageKey());

        StorageException thrown = assertThrows(
                StorageException.class,
                () -> fixture.attachmentService().downloadAttachment(attachment.getId())
        );

        assertSame(loadFailure, thrown);
        verify(fixture.activityNotificationService(), never())
                .logAttachmentDownloaded(any(User.class), any(TaskAttachment.class));
    }

    @Test
    @DisplayName("ATT-R-05: 補償削除失敗時も利用者向けには元の保存失敗として扱う")
    void compensationDeleteFailureReturnsOriginalFailure() {
        AttachmentServiceFixture fixture = newAttachmentServiceFixture();
        MockMultipartFile file = textFile("compensation-failure.txt");
        RuntimeException saveFailure = new RuntimeException("metadata save failed");
        when(fixture.taskAttachmentRepository().save(any(TaskAttachment.class))).thenThrow(saveFailure);
        doThrow(new StorageException(ErrorCode.FILE_011, "delete failed"))
                .when(fixture.attachmentStorageService())
                .delete(any(String.class));
        when(fixture.structuredLogService().currentRequestFields(500, true, false, false))
                .thenReturn(new LinkedHashMap<>());

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> fixture.attachmentService().uploadAttachment(fixture.task().getId(), file)
        );

        assertSame(saveFailure, thrown);
    }

    @Test
    @DisplayName("ATT-R-06: 補償削除失敗時は ERROR ログに追跡情報を残す")
    void compensationDeleteFailureWritesErrorLog() {
        AttachmentServiceFixture fixture = newAttachmentServiceFixture();
        MockMultipartFile file = textFile("compensation-log-failure.txt");
        RuntimeException saveFailure = new RuntimeException("metadata save failed");
        when(fixture.taskAttachmentRepository().save(any(TaskAttachment.class))).thenThrow(saveFailure);
        doThrow(new StorageException(ErrorCode.FILE_011, "delete failed"))
                .when(fixture.attachmentStorageService())
                .delete(any(String.class));
        when(fixture.structuredLogService().currentRequestFields(500, true, false, false))
                .thenReturn(new LinkedHashMap<>());

        assertThrows(
                RuntimeException.class,
                () -> fixture.attachmentService().uploadAttachment(fixture.task().getId(), file)
        );

        verify(fixture.structuredLogService()).errorApplication(
                eq("LOG-SYS-001"),
                eq("添付補償削除失敗"),
                anyMap()
        );
    }

    private void assertUploadFailureDeletesStoredObject(String failureMessage) {
        AttachmentServiceFixture fixture = newAttachmentServiceFixture();
        MockMultipartFile file = textFile(failureMessage.replace(' ', '-') + ".txt");
        RuntimeException uploadFailure = new RuntimeException(failureMessage);
        when(fixture.taskAttachmentRepository().save(any(TaskAttachment.class))).thenAnswer(invocation -> {
            TaskAttachment attachment = invocation.getArgument(0);
            attachment.setId(100L);
            return attachment;
        });
        doThrow(uploadFailure)
                .when(fixture.activityNotificationService())
                .recordAttachmentUploaded(eq(fixture.actor()), any(TaskAttachment.class));

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> fixture.attachmentService().uploadAttachment(fixture.task().getId(), file)
        );

        assertSame(uploadFailure, thrown);
        ArgumentCaptor<String> storageKey = ArgumentCaptor.forClass(String.class);
        verify(fixture.attachmentStorageService()).store(storageKey.capture(), eq(file));
        verify(fixture.attachmentStorageService()).delete(storageKey.getValue());
    }

    private AttachmentContext newAttachmentContext(String title) throws Exception {
        User creator = createUser("Creator", "creator@example.com", "password123");
        User assignee = createUser("Assignee", "assignee@example.com", "password123");
        User outsider = createUser("Outsider", "outsider@example.com", "password123");
        Task task = createTask(title, creator, assignee, TaskStatus.TODO, Priority.HIGH);
        String creatorToken = loginAndGetToken("creator@example.com", "password123");
        String assigneeToken = loginAndGetToken("assignee@example.com", "password123");
        String outsiderToken = loginAndGetToken("outsider@example.com", "password123");
        return new AttachmentContext(creator, assignee, outsider, task, creatorToken, assigneeToken, outsiderToken);
    }

    private void deleteAttachment(String token, Long attachmentId) throws Exception {
        mockMvc.perform(delete("/api/attachments/{attachmentId}", attachmentId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
    }

    private AttachmentServiceFixture newAttachmentServiceFixture() {
        TaskAttachmentRepository fixtureTaskAttachmentRepository = mock(TaskAttachmentRepository.class);
        UserRepository fixtureUserRepository = mock(UserRepository.class);
        CurrentUserProvider fixtureCurrentUserProvider = mock(CurrentUserProvider.class);
        TaskAuthorizationService fixtureTaskAuthorizationService = mock(TaskAuthorizationService.class);
        ActivityNotificationService fixtureActivityNotificationService = mock(ActivityNotificationService.class);
        AttachmentStorageService fixtureAttachmentStorageService = mock(AttachmentStorageService.class);
        StorageProperties fixtureStorageProperties = mock(StorageProperties.class);
        StructuredLogService fixtureStructuredLogService = mock(StructuredLogService.class);

        AttachmentService fixtureAttachmentService = new AttachmentService(
                fixtureTaskAttachmentRepository,
                fixtureUserRepository,
                fixtureCurrentUserProvider,
                fixtureTaskAuthorizationService,
                fixtureActivityNotificationService,
                fixtureAttachmentStorageService,
                fixtureStorageProperties,
                fixtureStructuredLogService
        );

        User actor = User.builder()
                .id(1L)
                .name("Actor")
                .email("actor@example.com")
                .password("encoded")
                .build();
        Task task = Task.builder()
                .id(10L)
                .title("Attachment task")
                .description("Attachment task description")
                .status(TaskStatus.TODO)
                .priority(Priority.MEDIUM)
                .dueDate(LocalDate.of(2026, 4, 21))
                .createdBy(actor)
                .assignedUser(actor)
                .build();

        when(fixtureCurrentUserProvider.getCurrentUserId()).thenReturn(actor.getId());
        when(fixtureUserRepository.findById(actor.getId())).thenReturn(Optional.of(actor));
        when(fixtureTaskAuthorizationService.getUpdatableTask(task.getId(), actor.getId())).thenReturn(task);
        when(fixtureTaskAttachmentRepository.countActiveByTaskId(task.getId())).thenReturn(0L);
        when(fixtureTaskAttachmentRepository.sumActiveFileSizeByTaskId(task.getId())).thenReturn(0L);
        when(fixtureStorageProperties.getS3Prefix()).thenReturn("attachments");
        when(fixtureStorageProperties.getProvider()).thenReturn(AttachmentStorageType.LOCAL);

        return new AttachmentServiceFixture(
                fixtureAttachmentService,
                fixtureTaskAttachmentRepository,
                fixtureActivityNotificationService,
                fixtureAttachmentStorageService,
                fixtureStructuredLogService,
                actor,
                task
        );
    }

    private MockMultipartFile textFile(String fileName) {
        return new MockMultipartFile("file", fileName, "text/plain", "content".getBytes());
    }

    private record AttachmentContext(
            User creator,
            User assignee,
            User outsider,
            Task task,
            String creatorToken,
            String assigneeToken,
            String outsiderToken
    ) {
    }

    private record AttachmentServiceFixture(
            AttachmentService attachmentService,
            TaskAttachmentRepository taskAttachmentRepository,
            ActivityNotificationService activityNotificationService,
            AttachmentStorageService attachmentStorageService,
            StructuredLogService structuredLogService,
            User actor,
            Task task
    ) {
    }
}
