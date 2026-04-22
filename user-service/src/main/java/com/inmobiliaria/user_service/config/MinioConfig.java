package com.inmobiliaria.user_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class MinioConfig {

  @Value("${minio.endpoint}")
  private String endpoint;

  @Value("${minio.access-key}")
  private String accessKey;

  @Value("${minio.secret-key}")
  private String secretKey;

  @Value("${minio.bucket-name}")
  private String bucketName;

  @Bean
  public MinioClient minioClient() {
    MinioClient client =
        MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();

    ensureBucketExists(client);
    return client;
  }

  private void ensureBucketExists(MinioClient client) {
    try {
      boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());

      if (!exists) {
        client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        log.info("[MinIO] Bucket '{}' created successfully.", bucketName);
      } else {
        log.info("[MinIO] Bucket '{}' already exists.", bucketName);
      }
    } catch (Exception e) {
      log.error("[MinIO] Failed to verify/create bucket '{}': {}", bucketName, e.getMessage());
    }
  }
}
