package com.example.Reelify.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class S3Config {

    @Value("${minio.endpoint}")
    private String endPoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(endPoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .region(Region.SA_EAST_1)
                .forcePathStyle(true)
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()          //S3Presigner is for delegated operations — generates a URL that the Angular client uses to upload directly to MinIO,
                .endpointOverride(URI.create(endPoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .region(Region.SA_EAST_1)
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)   // same as forcePathStyle for S3Client
                        .build())
                .build();
    }
}

//With presigner (new):
//Angular → GET /initiate-upload → Reelify → returns presigned URL
//Angular → PUT 500MB directly → MinIO      ← your server handles 0 bytes