package com.example.task.config;

import com.example.task.entity.AttachmentStorageType;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 添付ファイル保存先の設定。
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    private AttachmentStorageType provider = AttachmentStorageType.LOCAL;

    @NotBlank
    private String localBasePath;

    private String s3Bucket;

    private String s3Region = "ap-northeast-1";

    @NotBlank
    private String s3Prefix = "attachments";
}
