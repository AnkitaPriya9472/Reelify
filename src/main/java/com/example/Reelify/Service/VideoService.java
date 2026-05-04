package com.example.Reelify.Service;

import com.example.Reelify.model.VideoMetadata;
import com.example.Reelify.repository.VideoMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class VideoService {

    @Autowired
    private VideoMetadataRepository videoMetadataRepository;

    @Autowired
    private S3Client s3Client;

    @Value("${minio.bucket}")
    private String bucket;

    public VideoMetadata uploadVideo(MultipartFile file, String title) throws IOException {
        String key = UUID.randomUUID() + "_" + file.getOriginalFilename();

        // Step 1: initiate multipart upload
        CreateMultipartUploadResponse initResponse = s3Client.createMultipartUpload(r -> r
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
        );
        String uploadId = initResponse.uploadId();

        // Step 2: split into 5MB chunks and upload each
        List<CompletedPart> completedParts = new ArrayList<>();
        byte[] bytes = file.getBytes();
        int chunkSize = 5 * 1024 * 1024;
        int partNumber = 1;

        for (int offset = 0; offset < bytes.length; offset += chunkSize) {
            int length = Math.min(chunkSize, bytes.length - offset);
            byte[] chunk = Arrays.copyOfRange(bytes, offset, offset + length);

            UploadPartResponse partResponse = s3Client.uploadPart(
                    UploadPartRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .uploadId(uploadId)
                            .partNumber(partNumber)
                            .contentLength((long) length)
                            .build(),
                    RequestBody.fromBytes(chunk)
            );

            completedParts.add(CompletedPart.builder()
                    .partNumber(partNumber)
                    .eTag(partResponse.eTag())
                    .build());
            partNumber++;
        }

        // Step 3: complete multipart upload
        s3Client.completeMultipartUpload(r -> r
                .bucket(bucket)
                .key(key)
                .uploadId(uploadId)
                .multipartUpload(m -> m.parts(completedParts))
        );

        // Step 4: save metadata to Cassandra
        VideoMetadata metadata = new VideoMetadata();
        metadata.setVideoId(UUID.randomUUID());
        metadata.setTitle(title);
        return videoMetadataRepository.save(metadata);
    }

    public VideoMetadata retrieveMetadata(UUID videoId) {
        return videoMetadataRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found: " + videoId));
    }
}
