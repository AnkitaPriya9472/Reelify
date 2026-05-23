Steps:
1.  mvn clean+install
2. docker start reelify-cassandra
2. docker ps
3. Run application

┌─────────────┐         ┌──────────────────────────┐          ┌─────────────┐
│             │  upload │                          │ segments │             │
│   Angular   │────────▶│      Spring Boot         │─────────▶│    MinIO    │
│  Reelify UI │         │        Reelify           │          │  (Storage)  │
│             │◀────────│                          │◀─────────│             │
│             │  stream │                          │  bytes   │             │
└─────────────┘         └──────────┬───────────────┘          └─────────────┘
                      metadata     │
                                   ▼
                            ┌─────────────────┐
                            │    Cassandra    │
                            │ (videoId+title) │
                            └─────────────────┘


![img.png](img.png)


```
│
POST /videos/upload
│  ├── video file (multipart)
│  └── metadata (title, description, uploader, etc.)
│
Spring Boot
├── saves video binary ──→ Personal Server
└── saves metadata ──────→ Cassandra
```

## 1.Dependencies in pom.xml
#### For Cassandra:
```
docker run --name reelify-cassandra \
  -p 9042:9042 \
  -e CASSANDRA_CLUSTER_NAME=ReelifyCluster \
  -d cassandra:4.1
```
## 2.Open an interactive shell inside the running Docker container named reelify-cassandra, and run Cassandra’s command-line client cqlsh.
-it

* -i = keep STDIN open (interactive input)
* -t = allocate terminal
* cqlsh = Cassandra Query Language shell.
```
docker exec -it reelify-cassandra cqlsh
```

create
```
CREATE KEYSPACE reelify
WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};
```





