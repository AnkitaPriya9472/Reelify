package com.example.Reelify.model;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("video_metadata")
public class VideoMetadata {
    @PrimaryKey
    private UUID videoId;
    private String title;
    private String status;           // PENDING, PROCESSING, READY, FAILED
    private String rawKey;           // e.g. "{videoId}/raw/input.mp4"
    private String masterPlaylistKey;// e.g. "{videoId}/master.m3u8"

    public UUID getVideoId() {
        return videoId;
    }

    public String getTitle() {
        return title;
    }

    public String getStatus() {
        return status;
    }

    public String getRawKey() {
        return rawKey;
    }

    public String getMasterPlaylistKey() {
        return masterPlaylistKey;
    }
}
