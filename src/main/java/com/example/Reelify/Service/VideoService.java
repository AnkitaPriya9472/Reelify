package com.example.Reelify.Service;

import com.example.Reelify.event.VideoUploadedEvent;
import com.example.Reelify.model.VideoMetadata;
import com.example.Reelify.model.VideoStatus;
import com.example.Reelify.repository.VideoMetadataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VideoService {

    private static final Logger log = LoggerFactory.getLogger(VideoService.class);

    @Autowired
    private VideoMetadataRepository videoMetadataRepository;

    @Autowired
    private S3Client s3Client;

    @Autowired
    private S3Presigner s3Presigner;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${minio.bucket}")
    private String bucket;

    // ── NEW: Step 1 of upload flow ─────────────────────────────────────────
    // Angular calls this first to get a pre-signed URL
    public Map<String, String> initiateUpload(String title) {
        String videoId = UUID.randomUUID().toString();
        String rawKey  = videoId + "/raw/input.mp4";

        // Save metadata with PENDING status
        VideoMetadata metadata = new VideoMetadata();
        metadata.setVideoId(UUID.fromString(videoId));
        metadata.setTitle(title);
        metadata.setStatus(VideoStatus.PENDING.name());
        metadata.setRawKey(rawKey);
        videoMetadataRepository.save(metadata);

        // Generate pre-signed PUT URL valid for 15 minutes
        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(
                PutObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(15))
                        .putObjectRequest(PutObjectRequest.builder()
                                .bucket(bucket)
                                .key(rawKey)
                                .build())
                        .build()
        );

        log.info("Pre-signed URL generated for videoId: {}", videoId);

        Map<String, String> response = new HashMap<>();
        response.put("videoId", videoId);
        response.put("presignedUrl", presigned.url().toString());
        return response;
    }

    // ── NEW: Step 2 of upload flow ─────────────────────────────────────────
    // Angular calls this after it finishes uploading to MinIO directly
    public void confirmUpload(String videoId) {
        VideoMetadata metadata = videoMetadataRepository
                .findById(UUID.fromString(videoId))
                .orElseThrow(() -> new RuntimeException("Video not found: " + videoId));

        VideoUploadedEvent event = new VideoUploadedEvent(
                videoId,
                metadata.getTitle(),
                metadata.getRawKey()
        );

        try{
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("video.uploaded", videoId, json);
            log.info("Published video.uploaded event for videoId: {}", videoId);
        }
        catch(Exception e) {
           throw new RuntimeException("Failed to publish Kafka event", e);
        }
    }

    // ── EXISTING methods below (unchanged) ────────────────────────────────

    public VideoMetadata uploadVideo(MultipartFile file, String title)
            throws IOException, InterruptedException {
        String videoId = UUID.randomUUID().toString();
        Path tempDir   = Files.createTempDirectory("reelify_" + videoId);
        Path inputFile = tempDir.resolve("input" + getExtension(file.getOriginalFilename()));
        file.transferTo(inputFile.toFile());

        String outputPattern = tempDir.resolve("segment_%03d.ts").toString();
        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-i", inputFile.toString(),
                "-c", "copy", "-f", "segment", "-segment_time", "10", outputPattern
        );
        pb.redirectErrorStream(true);
        int exitCode = pb.start().waitFor();
        if (exitCode != 0) throw new RuntimeException("ffmpeg failed: " + exitCode);

        File[] segments = tempDir.toFile().listFiles(
                f -> f.getName().startsWith("segment_") && f.getName().endsWith(".ts")
        );
        if (segments != null) {
            for (File segment : segments) {
                String segmentKey = videoId + "/" + segment.getName();
                s3Client.putObject(
                        PutObjectRequest.builder()
                                .bucket(bucket).key(segmentKey)
                                .contentType("video/MP2T")
                                .contentLength(segment.length()).build(),
                        RequestBody.fromFile(segment)
                );
                segment.delete();
            }
        }
        inputFile.toFile().delete();
        tempDir.toFile().delete();

        VideoMetadata metadata = new VideoMetadata();
        metadata.setVideoId(UUID.fromString(videoId));
        metadata.setTitle(title);
        return videoMetadataRepository.save(metadata);
    }

    public VideoMetadata retrieveMetadata(UUID videoId) {
        return videoMetadataRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found: " + videoId));
    }

    public List<VideoMetadata> getAllVideos() {
        return videoMetadataRepository.findAll();
    }

    public byte[] getMasterPlaylist(String videoId) {
        String masterKey = videoId + "/master.m3u8";
        return s3Client.getObjectAsBytes(
                r -> r.bucket(bucket).key(masterKey)
        ).asByteArray();
    }

    public byte[] getSegments(String videoId, String segmentName) {
        String key = videoId + "/" + segmentName;
        return s3Client.getObjectAsBytes(r -> r.bucket(bucket).key(key)).asByteArray();
    }

    private String getExtension(String filename) {
        if (filename == null) return ".mp4";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : ".mp4";
    }
}