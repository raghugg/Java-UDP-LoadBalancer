# Java Load Balancer

Built this as a learning project to get hands-on with networking concepts I'd only read about. It's a reverse proxy that distributes TCP traffic across multiple worker servers.

## What it does

Clients connect to the proxy over TCP. The proxy picks a worker using round robin and forwards the request. The response comes back through the proxy to the client.

Workers announce themselves to the proxy by sending a small UDP packet every 500ms ("heartbeat"). If a worker goes silent for more than 2 seconds it gets dropped from the pool. If a worker times out 3 times in a row the proxy stops sending it traffic entirely (circuit breaker).

## Technologies

- **Java** - raw sockets (ServerSocket, Socket, DatagramSocket), multithreading, InputStream/OutputStream for byte-level stream relay
- **TCP** - used for the actual request/response data between client, proxy, and workers
- **UDP** - used for the heartbeat control plane. Workers blast a packet at the proxy every 500ms, proxy uses this to maintain a live registry of healthy workers
- **Docker / Docker Compose** - each worker runs in its own container, proxy runs in a separate container. Two virtual networks: one public-facing, one internal for proxy-worker communication

## How to run

```bash
# build jars
./gradlew proxyJar workerJar

# start everything
docker compose up --build
```
