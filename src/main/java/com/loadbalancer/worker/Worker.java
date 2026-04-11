package com.loadbalancer.worker;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Worker {

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        String workerId = System.getenv("WORKER_ID") != null ? System.getenv("WORKER_ID") : "worker-default";

        String proxyHost = System.getenv("PROXY_HOST") != null ? System.getenv("PROXY_HOST") : "localhost";
        int proxyUdpPort = System.getenv("PROXY_UDP_PORT") != null
            ? Integer.parseInt(System.getenv("PROXY_UDP_PORT")) : 9000;

        ServerSocket serverSocket = new ServerSocket(port);
        serverSocket.setReuseAddress(true);
        ExecutorService pool = Executors.newCachedThreadPool();
        HeartbeatSender heartbeat = new HeartbeatSender(workerId, port, proxyHost, proxyUdpPort);

        System.out.println("[" + workerId + "] Listening on port " + port);
        heartbeat.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            heartbeat.stop();
            try { serverSocket.close(); } catch (IOException ignored) {}
            pool.shutdown();
            System.out.println("[" + workerId + "] Shut down.");
        }));

        while (!serverSocket.isClosed()) {
            try {
                Socket client = serverSocket.accept();
                pool.submit(new WorkerHandler(client, workerId));
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    System.err.println("[" + workerId + "] Accept error: " + e.getMessage());
                }
            }
        }
    }
}
