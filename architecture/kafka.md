**Step 1 — pom.xml**
Add this inside <dependencies> after the existing cassandra dependency:

<!-- Kafka — publish video.uploaded events -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>

**Step 2 — model/VideoStatus.java**
Create a new Java enum called VideoStatus inside the model package of the main Reelify app

**Step 3 — event/VideoUploadedEvent.java**
This class in the Reelify app produces the event. The same class in the ingestion service consumes it. 
The only thing connecting them is the JSON shape on the Kafka topic:
```
Reelify app publishes:        Kafka wire format:         Ingestion service reads:
VideoUploadedEvent  →  →  →  {"videoId":"abc",     →  →  VideoUploadedEvent
  videoId = "abc"             "title":"My Video",          videoId = "abc"
  title = "My Video"          "rawKey":"abc/raw/.."}        title = "My Video"
  rawKey = "abc/raw/.."                                     rawKey = "abc/raw/.."
```

**Step 4 — Update config/S3Config.java** 
````
                                                    S3Client	                               S3Presigner
Used for	            Server-side operations (download raw video, upload segments)	Generating signed URLs for client uploads
Makes network calls 	Yes — directly to MinIO	                                        No — just generates a URL
Used by	                VideoService, SegmentUploadService	                            VideoService.initiateUpload() only
````

**Step 5 — application.properties in main Reelify app**

