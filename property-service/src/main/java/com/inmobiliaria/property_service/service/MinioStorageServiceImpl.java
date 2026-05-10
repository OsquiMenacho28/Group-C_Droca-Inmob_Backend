package com.inmobiliaria.property_service.service;

import io.minio.*;
import io.minio.http.Method;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageServiceImpl implements StorageService {

  private final MinioClient minioClient;

  @org.springframework.beans.factory.annotation.Value("${minio.endpoint:http://localhost:9000}")
  private String endpoint;

  @Override
  public Map<String, Object> generateUploadPolicy(
      String bucket, String objectKey, String contentType, long maxSizeBytes, int expiryMinutes) {

    try {
      ensureBucketExists(bucket);

      PostPolicy policy = new PostPolicy(bucket, ZonedDateTime.now().plusMinutes(expiryMinutes));
      policy.addEqualsCondition("key", objectKey);
      policy.addContentLengthRangeCondition(1, maxSizeBytes);

      // Allow matching content type (e.g. image/jpeg, image/png)
      if (contentType != null) {
        if (contentType.endsWith("/*")) {
          policy.addStartsWithCondition("Content-Type", contentType.replace("/*", ""));
        } else {
          policy.addEqualsCondition("Content-Type", contentType);
        }
      }

      Map<String, String> formData = minioClient.getPresignedPostFormData(policy);

      Map<String, Object> response = new HashMap<>();
      // Construct the URL: endpoint/bucket
      String url = endpoint;
      if (!url.endsWith("/")) url += "/";
      url += bucket;

      response.put("url", url);

      // Extract form fields
      Map<String, String> fields = new HashMap<>(formData);
      fields.put("key", objectKey);
      if (contentType != null && !contentType.endsWith("/*")) {
        fields.put("Content-Type", contentType);
      }
      response.put("formData", fields);

      return response;
    } catch (Exception e) {
      log.error("Error generating MinIO PostPolicy: {}", e.getMessage());
      throw new RuntimeException("Failed to generate upload policy", e);
    }
  }

  @Override
  public Map<String, Object> getObjectMetadata(String bucket, String objectKey) {
    try {
      StatObjectResponse stat =
          minioClient.statObject(StatObjectArgs.builder().bucket(bucket).object(objectKey).build());

      Map<String, Object> metadata = new HashMap<>();
      metadata.put("size", stat.size());
      metadata.put("contentType", stat.contentType());
      metadata.put("lastModified", stat.lastModified());
      return metadata;
    } catch (Exception e) {
      log.error("Error getting object metadata for {}: {}", objectKey, e.getMessage());
      throw new RuntimeException("Object not found or inaccessible", e);
    }
  }

  @Override
  public void deleteObject(String bucket, String objectKey) {
    try {
      minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectKey).build());
    } catch (Exception e) {
      log.error("Error deleting object {}: {}", objectKey, e.getMessage());
    }
  }

  @Override
  public void moveObject(String bucket, String sourceKey, String destinationKey) {
    try {
      // Copy object
      minioClient.copyObject(
          CopyObjectArgs.builder()
              .bucket(bucket)
              .object(destinationKey)
              .source(CopySource.builder().bucket(bucket).object(sourceKey).build())
              .build());
      // Delete source
      deleteObject(bucket, sourceKey);
      log.info("Moved object from {} to {}", sourceKey, destinationKey);
    } catch (Exception e) {
      log.error("Error moving object from {} to {}: {}", sourceKey, destinationKey, e.getMessage());
      throw new RuntimeException("Failed to move object", e);
    }
  }

  @Override
  public String generateTemporaryUrl(String bucket, String objectKey, int expiryMinutes) {
    try {
      return minioClient.getPresignedObjectUrl(
          GetPresignedObjectUrlArgs.builder()
              .method(Method.GET)
              .bucket(bucket)
              .object(objectKey)
              .expiry(expiryMinutes, java.util.concurrent.TimeUnit.MINUTES)
              .build());
    } catch (Exception e) {
      log.error("Error generating temporary URL: {}", e.getMessage());
      throw new RuntimeException("Failed to generate temporary URL", e);
    }
  }

  @Override
  public String generatePresignedPutUrl(String bucket, String objectKey, int expiryMinutes) {
    try {
      ensureBucketExists(bucket);
      return minioClient.getPresignedObjectUrl(
          GetPresignedObjectUrlArgs.builder()
              .method(Method.PUT)
              .bucket(bucket)
              .object(objectKey)
              .expiry(expiryMinutes, java.util.concurrent.TimeUnit.MINUTES)
              .build());
    } catch (Exception e) {
      log.error("Error generating presigned PUT URL: {}", e.getMessage());
      throw new RuntimeException("Failed to generate presigned PUT URL", e);
    }
  }

  @Override
  public void uploadObject(
      String bucket,
      String objectKey,
      java.io.InputStream inputStream,
      long size,
      String contentType) {
    try {
      minioClient.putObject(
          PutObjectArgs.builder().bucket(bucket).object(objectKey).stream(inputStream, size, -1)
              .contentType(contentType)
              .build());
    } catch (Exception e) {
      log.error("Error uploading object {}: {}", objectKey, e.getMessage());
      throw new RuntimeException("Failed to upload object", e);
    }
  }

  @Override
  public void ensureBucketExists(String bucket) {
    try {
      boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
      if (!found) {
        minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        log.info("Created bucket: {}", bucket);
      }
    } catch (Exception e) {
      log.error("Error ensuring bucket {} exists: {}", bucket, e.getMessage());
    }
  }
}
