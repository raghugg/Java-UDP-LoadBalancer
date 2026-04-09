package com.loadbalancer.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ProxyHandler implements Runnable {

    private final Socket clientSocket;
    private final List<String[]> workers;
    private final AtomicInteger counter;

    public ProxyHandler(Socket clientSocket, List<String[]> workers, AtomicInteger counter) {
        this.clientSocket = clientSocket;
        this.workers = workers;
        this.counter = counter;
    }

    @Override
    public void run() {
        // Round robin: atomically increment counter, mod by worker count
        int index = Math.abs(counter.getAndIncrement() % workers.size());
        String[] worker = workers.get(index);
        String workerHost = worker[0];
        int workerPort = Integer.parseInt(worker[1]);

        System.out.println("[Proxy] Forwarding to " + workerHost + ":" + workerPort);

        try (clientSocket;
             Socket workerSocket = new Socket(workerHost, workerPort)) {

            relay(clientSocket, workerSocket);

        } catch (IOException e) {
            System.err.println("[Proxy] Error relaying to worker: " + e.getMessage());
        }
    }

    private void relay(Socket from, Socket to) throws IOException {
        InputStream clientIn = from.getInputStream();
        OutputStream clientOut = from.getOutputStream();
        InputStream workerIn = to.getInputStream();
        OutputStream workerOut = to.getOutputStream();

        // Forward client request to worker
        pipe(clientIn, workerOut);
        // Signal to worker that client is done sending
        to.shutdownOutput();

        // Forward worker response back to client
        pipe(workerIn, clientOut);
    }

    private void pipe(InputStream in, OutputStream out) {
        byte[] buf = new byte[4096];
        int n;
        try {
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
                out.flush();
            }
        } catch (IOException ignored) {
            // EOF or socket closed — normal end of stream
        }
    }
}
