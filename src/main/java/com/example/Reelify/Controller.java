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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

@CrossOrigin(origins = "http://localhost:4200")
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
                                                @RequestPart("title") String title) throws IOException, InterruptedException {
        return ResponseEntity.ok(videoService.uploadVideo(file, title));
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

    @GetMapping("/stream/playlist")
    public ResponseEntity<String> playlist() {
        String m3u8 = "#EXTM3U\n" +
                "#EXT-X-VERSION:3\n" +
                "#EXT-X-TARGETDURATION:10\n" +
                "#EXT-X-MEDIA-SEQUENCE:0\n" +
                "#EXTINF:10.0,\n" + "http://localhost:8080/videos/stream/output_000.ts\n" +
                "#EXTINF:10.0,\n" + "http://localhost:8080/videos/stream/output_001.ts\n" +
                "#EXTINF:10.0,\n" + "http://localhost:8080/videos/stream/output_002.ts\n" +
                "#EXTINF:10.0,\n" + "http://localhost:8080/videos/stream/output_003.ts\n" +
                "#EXTINF:10.0,\n" + "http://localhost:8080/videos/stream/output_004.ts\n" +
                "#EXT-X-ENDLIST";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"))
                .body(m3u8);

        //a UTF-8 encoded playlist file used by websites to stream audio and video,
        // acting as a manifest that tells a media player where to find video segments and what order to play them in.
        // It is the foundation of HTTP Live Streaming (HLS)
    }

}
