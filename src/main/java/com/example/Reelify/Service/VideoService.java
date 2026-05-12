package com.example.Reelify.Service;

import com.example.Reelify.model.VideoMetadata;
import com.example.Reelify.repository.VideoMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class VideoService {

    @Autowired
    private VideoMetadataRepository videoMetadataRepository;

    @Autowired
    private S3Client s3Client;

    @Value("${minio.bucket}")
    private String bucket;

    public VideoMetadata uploadVideo(MultipartFile file, String title) throws IOException, InterruptedException {

        // Step 1: save uploaded file to a temp location on disk (ffmpeg needs a real file path)
        String videoId = UUID.randomUUID().toString();
        Path tempDir = Files.createTempDirectory("reelify_" + videoId);
        Path inputFile = tempDir.resolve("input" + getExtension(file.getOriginalFilename()));
        file.transferTo(inputFile.toFile());

        // Step 2: run ffmpeg to split into 10-second .ts segments
        String outputPattern = tempDir.resolve("segment_%03d.ts").toString();
        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-i", inputFile.toString(),
                "-c", "copy",
                "-f", "segment",
                "-segment_time", "10",
                outputPattern
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("ffmpeg failed with exit code: " + exitCode);
        }

        // Step 3: upload each segment to MinIO
        File[] segments = tempDir.toFile().listFiles(
                f -> f.getName().startsWith("segment_") && f.getName().endsWith(".ts")
        );

        if (segments != null) {
            for (File segment : segments) {
                String segmentKey = videoId + "/" + segment.getName();
                s3Client.putObject(
                        PutObjectRequest.builder()
                                .bucket(bucket)
                                .key(segmentKey)
                                .contentType("video/MP2T")
                                .contentLength(segment.length())
                                .build(),
                        RequestBody.fromFile(segment)
                );
                segment.delete();
            }
        }

        // Step 4: clean up temp files
        inputFile.toFile().delete();
        tempDir.toFile().delete();

        // Step 5: save metadata to Cassandra
        VideoMetadata metadata = new VideoMetadata();
        metadata.setVideoId(UUID.fromString(videoId));
        metadata.setTitle(title);
        return videoMetadataRepository.save(metadata);
    }

    public VideoMetadata retrieveMetadata(UUID videoId) {
        return videoMetadataRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found: " + videoId));
    }

    private String getExtension(String filename) {
        if (filename == null) return ".mp4";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : ".mp4";
    }
}
