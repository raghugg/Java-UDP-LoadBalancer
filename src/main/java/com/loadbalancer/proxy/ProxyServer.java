package com.loadbalancer.proxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ProxyServer {

    // Hardcoded worker list: each entry is { host, port }
    private static final List<String[]> WORKERS = Arrays.asList(
        new String[]{"localhost", "8081"},
        new String[]{"localhost", "8082"},
        new String[]{"localhost", "8083"}
    );

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;

        ServerSocket serverSocket = new ServerSocket(port);
        serverSocket.setReuseAddress(true);
        ExecutorService pool = Executors.newCachedThreadPool();
        AtomicInteger counter = new AtomicInteger(0);

        System.out.println("[Proxy] Listening on port " + port);
        System.out.println("[Proxy] Workers: " + WORKERS.size());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { serverSocket.close(); } catch (IOException ignored) {}
            pool.shutdown();
            System.out.println("[Proxy] Shut down.");
        }));

        while (!serverSocket.isClosed()) {
            try {
                Socket client = serverSocket.accept();
                pool.submit(new ProxyHandler(client, WORKERS, counter));
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    System.err.println("[Proxy] Accept error: " + e.getMessage());
                }
            }
        }
    }
}
