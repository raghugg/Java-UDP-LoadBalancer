package com.loadbalancer.proxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ProxyServer {

    public static void main(String[] args) throws IOException {
        int tcpPort = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        int udpPort = System.getenv("PROXY_UDP_PORT") != null
            ? Integer.parseInt(System.getenv("PROXY_UDP_PORT")) : 9000;

        WorkerRegistry registry = new WorkerRegistry();
        HeartbeatReceiver heartbeat = new HeartbeatReceiver(udpPort, registry.getMap());
        CircuitBreaker circuitBreaker = new CircuitBreaker();
        heartbeat.start();

        ServerSocket serverSocket = new ServerSocket(tcpPort);
        serverSocket.setReuseAddress(true);
        ExecutorService pool = Executors.newCachedThreadPool();
        AtomicInteger counter = new AtomicInteger(0);

        System.out.println("[Proxy] Listening on port " + tcpPort);
        System.out.println("[Proxy] Waiting for workers to register via UDP...");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            heartbeat.stop();
            registry.stop();
            try { serverSocket.close(); } catch (IOException ignored) {}
            pool.shutdown();
            System.out.println("[Proxy] Shut down.");
        }));

        while (!serverSocket.isClosed()) {
            try {
                Socket client = serverSocket.accept();
                pool.submit(new ProxyHandler(client, registry, counter, circuitBreaker));
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    System.err.println("[Proxy] Accept error: " + e.getMessage());
                }
            }
        }
    }
}
