package com.example.soundcloud.service;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
public class FileStorageService {

    private final MinioClient minioClient;
    private final String audioBucket;
    private final String imageBucket;

    public FileStorageService(@Value("${minio.url}") String url,
                              @Value("${minio.access-key}") String accessKey,
                              @Value("${minio.secret-key}") String secretKey,
                              @Value("${minio.bucket.audio}") String audioBucket,
                              @Value("${minio.bucket.images}") String imageBucket) throws Exception {
        this.minioClient = MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secretKey)
                .build();
        this.audioBucket = audioBucket;
        this.imageBucket = imageBucket;

        ensureBucket(audioBucket);
        ensureBucket(imageBucket);
    }

    private void ensureBucket(String bucket) throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    public String storeAudio(MultipartFile file) throws Exception {
        return storeFile(file, audioBucket, "audio");
    }

    public String storeImage(MultipartFile file) throws Exception {
        return storeFile(file, imageBucket, "image");
    }

    private String storeFile(MultipartFile file, String bucket, String prefix) throws Exception {
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : prefix);
        String ext = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex >= 0) {
            ext = originalFilename.substring(dotIndex);
        }
        String objectName = prefix + "/" + UUID.randomUUID() + ext;

        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(is, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
        }
        return objectName;
    }

    public InputStreamResource getAudio(String objectName) throws Exception {
        InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(audioBucket)
                        .object(objectName)
                        .build()
        );
        return new InputStreamResource(stream);
    }

    public InputStreamResource getAudioRange(String objectName, long offset, long length) throws Exception {
        InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(audioBucket)
                        .object(objectName)
                        .offset(offset)
                        .length(length)
                        .build()
        );
        return new InputStreamResource(stream);
    }

    public long getAudioSize(String objectName) throws Exception {
        StatObjectResponse stat = minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(audioBucket)
                        .object(objectName)
                        .build()
        );
        return stat.size();
    }

    public InputStreamResource getImage(String objectName) throws Exception {
        InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(imageBucket)
                        .object(objectName)
                        .build()
        );
        return new InputStreamResource(stream);
    }

    public void deleteAudio(String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(audioBucket).object(objectName).build());
        } catch (Exception ignored) {
        }
    }

    public void deleteImage(String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(imageBucket).object(objectName).build());
        } catch (Exception ignored) {
        }
    }
}

