package com.loadbalancer.worker;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Worker {

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        String workerId = args.length > 1 ? args[1] : "worker-default";

        ServerSocket serverSocket = new ServerSocket(port);
        serverSocket.setReuseAddress(true);
        ExecutorService pool = Executors.newCachedThreadPool();

        System.out.println("[" + workerId + "] Listening on port " + port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
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
