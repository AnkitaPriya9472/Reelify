File 1: kong/docker-compose.yml
This starts Kong in DB-less mode (no database needed):

version: "3.8"

services:
kong:
image: kong:3.6
container_name: reelify-gateway
environment:
KONG_DATABASE: "off"
KONG_DECLARATIVE_CONFIG: /kong/declarative/kong.yml
KONG_PROXY_LISTEN: "0.0.0.0:9000"
KONG_ADMIN_LISTEN: "0.0.0.0:9001"
KONG_LOG_LEVEL: info
ports:
- "9000:9000"    # all client traffic comes here
- "9001:9001"    # kong admin API (for debugging)
volumes:
- ./kong.yml:/kong/declarative/kong.yml
File 2: kong/kong.yml
This is where routing, CORS, and rate limiting live:

_format_version: "3.0"
_transform: true

services:
- name: reelify-service
  url: http://host.docker.internal:8080    # your Spring Boot app
  routes:
    - name: videos-route
      paths:
        - /videos
          strip_path: false

plugins:
- name: cors
  config:
  origins:
  - http://localhost:4200
  methods:
  - GET
  - POST
  - PUT
  - DELETE
  - OPTIONS
  headers:
  - Authorization
  - Content-Type
  exposed_headers:
  - Authorization
  credentials: true
  max_age: 3600

- name: rate-limiting
  config:
  minute: 100
  policy: local

  How to run it 
```cd kong/
  docker-compose up
```

Section	What it does
services	Tells Kong where your Reelify app lives (localhost:8080)
routes	Any request to localhost:9000/videos/** gets forwarded to Reelify
cors plugin	Allows your Angular UI at port 4200 to call the gateway
rate-limiting plugin	Max 100 requests/minute per client IP
strip_path: false	Keeps /videos in the URL when forwarding — Reelify needs it

Test after running
# Before (calling Reelify directly)
curl http://localhost:8080/videos/metadata/{id}

# After (calling through Kong gateway)
curl http://localhost:9000/videos/metadata/some-id
Both should return the same response — Kong transparently forwards it.


Add auth-service plugin
plugins:
- name: jwt
  route: videos-route           # protect only /videos, NOT /auth
  config:
  claims_to_verify:
  - exp   