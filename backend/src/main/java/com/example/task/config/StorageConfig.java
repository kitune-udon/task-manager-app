package com.example.task.config;

import com.example.task.entity.AttachmentStorageType;
import com.example.task.service.storage.AttachmentStorageService;
import com.example.task.service.storage.LocalAttachmentStorageService;
import com.example.task.service.storage.S3AttachmentStorageService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * 添付ストレージ実装の切り替え設定。
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    @Bean
    public AttachmentStorageService attachmentStorageService(StorageProperties properties) {
        if (properties.getProvider() == AttachmentStorageType.S3) {
            S3Client s3Client = S3Client.builder()
                    .region(Region.of(properties.getS3Region()))
                    .build();
            return new S3AttachmentStorageService(s3Client, properties);
        }

        return new LocalAttachmentStorageService(properties);
    }
}
