package com.example.task.service.storage;

import com.example.task.config.StorageProperties;
import com.example.task.exception.ErrorCode;
import com.example.task.exception.StorageException;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;

/**
 * AWS S3 を利用する添付保存実装。
 */
public class S3AttachmentStorageService implements AttachmentStorageService {

    private final S3Client s3Client;
    private final String bucket;

    /**
     * S3添付ストレージを生成する。
     *
     * @param s3Client S3クライアント
     * @param properties ストレージ設定
     */
    public S3AttachmentStorageService(S3Client s3Client, StorageProperties properties) {
        this.s3Client = s3Client;
        this.bucket = properties.getS3Bucket();
    }

    /**
     * 添付ファイルをS3へ保存する。
     *
     * <p>ストレージキーをS3オブジェクトキーとして使用し、アップロードされたファイルのコンテントタイプを設定する。</p>
     *
     * @param storageKey 保存先を表すストレージキー
     * @param file 保存する添付ファイル
     * @throws StorageException ファイル読み取りまたはS3への保存に失敗した場合
     */
    @Override
    public void store(String storageKey, MultipartFile file) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(storageKey)
                    .contentType(file.getContentType())
                    .build();
            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
        } catch (IOException | S3Exception ex) {
            throw new StorageException(ErrorCode.FILE_009, "Failed to store file in S3", ex);
        }
    }

    /**
     * S3から添付ファイルを読み込む。
     *
     * <p>S3オブジェクトのコンテントタイプを利用し、取得できない場合は {@code application/octet-stream} を返す。</p>
     *
     * @param storageKey 読み込み対象を表すストレージキー
     * @return ファイル内容とコンテントタイプを含む保存済み添付ファイル
     * @throws StorageException 対象オブジェクトが存在しない、または読み込みに失敗した場合
     */
    @Override
    public StoredAttachment load(String storageKey) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(storageKey)
                    .build();
            ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request);
            String contentType = response.response().contentType();
            return new StoredAttachment(response.asByteArray(),
                    StringUtils.hasText(contentType) ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE);
        } catch (NoSuchKeyException ex) {
            throw new StorageException(ErrorCode.FILE_010, "File was not found in S3", ex);
        } catch (S3Exception ex) {
            throw new StorageException(ErrorCode.FILE_010, "Failed to load file from S3", ex);
        }
    }

    /**
     * S3から添付ファイルを削除する。
     *
     * @param storageKey 削除対象を表すストレージキー
     * @throws StorageException S3からの削除に失敗した場合
     */
    @Override
    public void delete(String storageKey) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(storageKey)
                    .build();
            s3Client.deleteObject(request);
        } catch (S3Exception ex) {
            throw new StorageException(ErrorCode.FILE_011, "Failed to delete file from S3", ex);
        }
    }
}
