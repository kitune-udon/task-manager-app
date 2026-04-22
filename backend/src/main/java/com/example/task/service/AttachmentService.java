package com.example.task.service;

import com.example.task.config.StorageProperties;
import com.example.task.dto.AttachmentResponse;
import com.example.task.dto.TaskUserResponse;
import com.example.task.entity.Task;
import com.example.task.entity.TaskAttachment;
import com.example.task.entity.User;
import com.example.task.exception.BusinessException;
import com.example.task.exception.ErrorCode;
import com.example.task.exception.ResourceNotFoundException;
import com.example.task.exception.StorageException;
import com.example.task.logging.StructuredLogService;
import com.example.task.repository.TaskAttachmentRepository;
import com.example.task.repository.UserRepository;
import com.example.task.security.CurrentUserProvider;
import com.example.task.service.storage.AttachmentStorageService;
import com.example.task.service.storage.StoredAttachment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 添付ファイルの一覧・アップロード・ダウンロード・削除を扱うサービス。
 */
@Service
public class AttachmentService {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final int MAX_ATTACHMENT_COUNT = 20;
    private static final long MAX_TOTAL_FILE_SIZE = 100L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "txt", "md", "csv", "docx", "xlsx", "pptx", "png", "jpg", "jpeg", "gif", "webp", "zip"
    );
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "text/plain",
            "text/markdown",
            "text/csv",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "image/png",
            "image/jpeg",
            "image/gif",
            "image/webp",
            "application/zip",
            "application/x-zip-compressed"
    );

    private final TaskAttachmentRepository taskAttachmentRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final TaskAuthorizationService taskAuthorizationService;
    private final ActivityNotificationService activityNotificationService;
    private final AttachmentStorageService attachmentStorageService;
    private final StorageProperties storageProperties;
    private final StructuredLogService structuredLogService;

    /**
     * コンストラクタ。
     *
     * @param taskAttachmentRepository 添付ファイルリポジトリ
     * @param userRepository ユーザーリポジトリ
     * @param currentUserProvider 現在のユーザー提供者
     * @param taskAuthorizationService タスク認可サービス
     * @param activityNotificationService アクティビティ通知サービス
     * @param attachmentStorageService 添付ファイルストレージサービス
     * @param storageProperties ストレージ設定
     * @param structuredLogService 構造化ログサービス
     */
    public AttachmentService(
            TaskAttachmentRepository taskAttachmentRepository,
            UserRepository userRepository,
            CurrentUserProvider currentUserProvider,
            TaskAuthorizationService taskAuthorizationService,
            ActivityNotificationService activityNotificationService,
            AttachmentStorageService attachmentStorageService,
            StorageProperties storageProperties,
            StructuredLogService structuredLogService
    ) {
        this.taskAttachmentRepository = taskAttachmentRepository;
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
        this.taskAuthorizationService = taskAuthorizationService;
        this.activityNotificationService = activityNotificationService;
        this.attachmentStorageService = attachmentStorageService;
        this.storageProperties = storageProperties;
        this.structuredLogService = structuredLogService;
    }

    /**
     * 指定されたタスクの添付ファイル一覧を取得します。
     * 現在のユーザーがタスクを閲覧可能であることを確認してから取得します。
     *
     * @param taskId タスクID
     * @return 添付ファイルレスポンスリスト
     */
    @Transactional(readOnly = true)
    public List<AttachmentResponse> getAttachments(Long taskId) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        Task task = taskAuthorizationService.getViewableTask(taskId, currentUserId);
        return taskAttachmentRepository.findActiveByTaskId(task.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * タスクに添付ファイルをアップロードします。
     * ファイル検証、ストレージ保存、DB補償削除を処理します。
     *
     * @param taskId タスクID
     * @param file アップロードするファイル
     * @return アップロードされた添付ファイルレスポンス
     */
    @Transactional
    public AttachmentResponse uploadAttachment(Long taskId, MultipartFile file) {
        User currentUser = resolveCurrentUser();
        Task task = taskAuthorizationService.getUpdatableTask(taskId, currentUser.getId());

        validateFile(file, taskId);

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = extractExtension(originalFileName);
        String storageKey = generateStorageKey(task.getId(), extension);

        attachmentStorageService.store(storageKey, file);

        try {
            TaskAttachment attachment = TaskAttachment.builder()
                    .task(task)
                    .originalFileName(originalFileName)
                    .storageKey(storageKey)
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .storageType(storageProperties.getProvider())
                    .checksumSha256(calculateChecksum(file))
                    .createdBy(currentUser)
                    .build();

            TaskAttachment saved = taskAttachmentRepository.save(attachment);
            activityNotificationService.recordAttachmentUploaded(currentUser, saved);
            return toResponse(saved);
        } catch (RuntimeException ex) {
            compensateStorageDelete(storageKey);
            throw ex;
        }
    }

    /**
     * 添付ファイルをダウンロードします。
     * 現在のユーザーがタスクを閲覧可能であることを確認してから取得します。
     *
     * @param attachmentId 添付ファイルID
     * @return ファイルコンテンツとメタデータを含むダウンロード結果
     */
    @Transactional(readOnly = true)
    public DownloadResult downloadAttachment(Long attachmentId) {
        User currentUser = resolveCurrentUser();
        TaskAttachment attachment = taskAttachmentRepository.findByIdAndDeletedAtIsNull(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.FILE_002, "添付ファイルが存在しません"));

        if (attachment.getTask() == null || attachment.getTask().getDeletedAt() != null) {
            throw new ResourceNotFoundException(ErrorCode.FILE_002, "添付ファイルが存在しません");
        }

        taskAuthorizationService.authorizeView(attachment.getTask(), currentUser.getId());
        StoredAttachment storedAttachment = attachmentStorageService.load(attachment.getStorageKey());
        activityNotificationService.logAttachmentDownloaded(currentUser, attachment);
        return new DownloadResult(attachment.getOriginalFileName(), storedAttachment.content(), storedAttachment.contentType());
    }

    /**
     * 添付ファイルを削除します。削除を実施して将来的に補償削除を処理します。
     * アップロードしたユーザーのみ削除可能です。
     *
     * @param attachmentId 添付ファイルID
     */
    @Transactional
    public void deleteAttachment(Long attachmentId) {
        User currentUser = resolveCurrentUser();
        TaskAttachment attachment = taskAttachmentRepository.findByIdAndDeletedAtIsNull(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.FILE_002, "添付ファイルが存在しません"));

        if (attachment.getTask() == null || attachment.getTask().getDeletedAt() != null) {
            throw new ResourceNotFoundException(ErrorCode.FILE_002, "添付ファイルが存在しません");
        }

        if (!currentUser.getId().equals(attachment.getCreatedBy().getId())) {
            throw new BusinessException(ErrorCode.FILE_004);
        }

        attachment.setDeletedAt(LocalDateTime.now());
        attachment.setDeletedBy(currentUser);
        taskAttachmentRepository.save(attachment);
        activityNotificationService.recordAttachmentDeleted(currentUser, attachment);
    }

    /**
     * アップロードされたファイルを検証します。
     * ファイル存在性、サイズ、拡張子、コンテントタイプ、件数、合計サイズを検証します。
     *
     * @param file 検証するファイル
     * @param taskId タスクID
     * @throws BusinessException 検証失敗時
     */
    private void validateFile(MultipartFile file, Long taskId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_001);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.FILE_005);
        }

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = extractExtension(originalFileName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.FILE_006);
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(ErrorCode.FILE_006);
        }

        long currentCount = taskAttachmentRepository.countActiveByTaskId(taskId);
        if (currentCount >= MAX_ATTACHMENT_COUNT) {
            throw new BusinessException(ErrorCode.FILE_007);
        }

        long currentTotalSize = taskAttachmentRepository.sumActiveFileSizeByTaskId(taskId);
        if (currentTotalSize + file.getSize() > MAX_TOTAL_FILE_SIZE) {
            throw new BusinessException(ErrorCode.FILE_008);
        }
    }

    /**
     * ストレージキーを生成します。
     * 形式: {prefix}/tasks/{taskId}/{date}/{uuid}.{extension}
     *
     * @param taskId タスクID
     * @param extension ファイル拡張子
     * @return 生成されたストレージキー
     */
    private String generateStorageKey(Long taskId, String extension) {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String prefix = normalizeStoragePrefix(storageProperties.getS3Prefix());
        return prefix + "/tasks/" + taskId + "/" + date + "/" + UUID.randomUUID() + "." + extension;
    }

    /**
     * ストレージプリフィクスを正規化します。
     * 前後のスラッシュを削除し、空文字列を「attachments」に置き換えます。
     *
     * @param prefix 正規化するプリフィクス
     * @return 正規化されたプリフィクス
     */
    private String normalizeStoragePrefix(String prefix) {
        if (!StringUtils.hasText(prefix)) {
            return "attachments";
        }

        String normalized = prefix.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return StringUtils.hasText(normalized) ? normalized : "attachments";
    }

    /**
     * ファイル名から拡張子を抽出します。
     *
     * @param originalFileName ファイル名
     * @return 小文字化された拡張子
     * @throws BusinessException 拡張子がない場合
     */
    private String extractExtension(String originalFileName) {
        if (!StringUtils.hasText(originalFileName) || !originalFileName.contains(".")) {
            throw new BusinessException(ErrorCode.FILE_006);
        }

        return originalFileName.substring(originalFileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * SHA-256チェックサムを計算します。
     *
     * @param file チェックサムを計算するファイル
     * @return SHA-256ハッシュ（十六進数表記）
     * @throws StorageException 計算失敗時
     */
    private String calculateChecksum(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(file.getBytes());
            StringBuilder builder = new StringBuilder();
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException | java.io.IOException ex) {
            throw new StorageException(ErrorCode.FILE_009, "Failed to calculate attachment checksum", ex);
        }
    }

    /**
     * ストレージを補償削除します。
     * DB保存失敗時にストレージを削除します。
     * 削除失敗はログを記録して処理を続けます。
     *
     * @param storageKey 削除するストレージキー
     */
    private void compensateStorageDelete(String storageKey) {
        try {
            attachmentStorageService.delete(storageKey);
        } catch (StorageException ex) {
            java.util.LinkedHashMap<String, Object> fields = structuredLogService.currentRequestFields(500, true, false, false);
            fields.put("storageKey", storageKey);
            fields.put("errorCode", ErrorCode.FILE_011.getCode());
            fields.put("safeMessage", ErrorCode.FILE_011.getDefaultMessage());
            structuredLogService.errorApplication("LOG-SYS-001", "添付補償削除失敗", fields);
        }
    }

    /**
     * 現在ログイン中のユーザーを得る。
     *
     * @return 現在のユーザー
     * @throws ResourceNotFoundException ユーザーが見つからない場合
     */
    private User resolveCurrentUser() {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        return userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USR_002, "ユーザーが存在しません"));
    }

    /**
     * 添付ファイルをレスポンスDTOに変換します。
     *
     * @param attachment 添付ファイルエンティティ
     * @return 添付ファイルレスポンスDTO
     */
    private AttachmentResponse toResponse(TaskAttachment attachment) {
        return AttachmentResponse.builder()
                .id(attachment.getId())
                .taskId(attachment.getTask().getId())
                .originalFileName(attachment.getOriginalFileName())
                .contentType(attachment.getContentType())
                .fileSize(attachment.getFileSize())
                .storageType(attachment.getStorageType())
                .uploadedBy(TaskUserResponse.builder()
                        .id(attachment.getCreatedBy().getId())
                        .name(attachment.getCreatedBy().getName())
                        .build())
                .createdAt(attachment.getCreatedAt())
                .build();
    }

    /**
     * ダウンロード結果を表現します。
     * ファイル名、コンテント、コンテントタイプを保持します。
     *
     * @param fileName ファイル名
     * @param content ファイルコンテンツ
     * @param contentType コンテントタイプ
     */
    public record DownloadResult(String fileName, byte[] content, String contentType) {
    }
}
