package com.example.Reelify;

import com.example.Reelify.Service.VideoService;
import com.example.Reelify.model.VideoMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/videos")
public class Controller {

    @Autowired
    VideoService videoService;
    @Autowired
    S3Client s3Client;

    @Value("${minio.bucket}")
    private String bucket;

    @PostMapping("/upload")
    public ResponseEntity<VideoMetadata> upload(@RequestPart("video") MultipartFile file,
                                                @RequestPart("title") String title) throws IOException {
        return ResponseEntity.ok(videoService.uploadVideo(file,title));
    }

    @GetMapping("/metadata/{id}")
    public ResponseEntity<VideoMetadata> getMetaData(@PathVariable UUID id){
        return ResponseEntity.ok(videoService.retrieveMetadata(id));
    }
    
    @GetMapping("/stream/{key}")
    public ResponseEntity<byte[]> stream(@PathVariable String key) {
        ResponseBytes<GetObjectResponse> object = s3Client.getObjectAsBytes(r -> r
                .bucket(bucket)
                .key(key)
        );
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp4"))
                .body(object.asByteArray());
    }
}
