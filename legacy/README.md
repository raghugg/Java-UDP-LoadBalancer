# Legacy: Docker Compose

This is the original deployment setup, replaced by the [Kubernetes manifests](../k8s/). Kept here for reference.

Two virtual networks: one public-facing (client → proxy), one internal (proxy ↔ workers, also carries UDP heartbeats).

## How to run

```bash
# build jars (from repo root)
./gradlew proxyJar workerJar

# start everything
docker compose -f legacy/docker-compose.yml up --build
```
