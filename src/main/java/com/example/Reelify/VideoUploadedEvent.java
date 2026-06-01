package com.example.Reelify;

public class VideoUploadedEvent {

    private String videoId;
    private String title;
    private String rawKey;  // "{videoId}/raw/input.mp4"

    public VideoUploadedEvent() {}

    public VideoUploadedEvent(String videoId, String title, String rawKey) {
        this.videoId = videoId;
        this.title = title;
        this.rawKey = rawKey;
    }

    public String getVideoId() { return videoId; }
    public void setVideoId(String videoId) { this.videoId = videoId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getRawKey() { return rawKey; }
    public void setRawKey(String rawKey) { this.rawKey = rawKey; }
}
