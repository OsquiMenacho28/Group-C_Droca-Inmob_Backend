package com.inmobiliaria.property_service.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MinioConfig {

  @Value("${minio.endpoint:http://minio:9000}")
  private String endpoint;

  @Value("${minio.external-endpoint:http://127.0.0.1:9000}")
  private String externalEndpoint;

  @Value("${minio.access-key:minioadmin}")
  private String accessKey;

  @Value("${minio.secret-key:minioadmin}")
  private String secretKey;

  @Value("${minio.bucket:properties}")
  private String bucket;

  @Value("${minio.documents.bucket:documents}")
  private String documentsBucket;

  @Bean
  @Primary
  public MinioClient minioClient() {
    // Internal client uses docker hostname (minio:9000)
    return MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
  }

  @Bean(name = "externalMinioClient")
  public MinioClient externalMinioClient() {
    // External client uses external hostname (127.0.0.1:9000) and EXPLICIT REGION
    // Explicit region prevents connection attempts during pre-signed URL generation
    return MinioClient.builder()
        .endpoint(externalEndpoint)
        .region("us-east-1")
        .credentials(accessKey, secretKey)
        .build();
  }

  public String getBucket() {
    return bucket;
  }

  public String getDocumentsBucket() {
    return documentsBucket;
  }
}
