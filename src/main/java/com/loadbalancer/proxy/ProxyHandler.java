package com.loadbalancer.proxy;

import com.loadbalancer.model.WorkerInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ProxyHandler implements Runnable {

    private static final int WORKER_TIMEOUT_MS = 5000;

    private final Socket clientSocket;
    private final WorkerRegistry registry;
    private final AtomicInteger counter;
    private final CircuitBreaker circuitBreaker;

    public ProxyHandler(Socket clientSocket, WorkerRegistry registry,
                        AtomicInteger counter, CircuitBreaker circuitBreaker) {
        this.clientSocket = clientSocket;
        this.registry = registry;
        this.counter = counter;
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    public void run() {
        // Filter out workers with open circuit breakers
        List<WorkerInfo> workers = registry.getLiveWorkers().stream()
            .filter(w -> !circuitBreaker.isOpen(w.id))
            .collect(Collectors.toList());

        if (workers.isEmpty()) {
            sendErrorAndClose("503 Service Unavailable: no healthy workers\n");
            return;
        }

        // Round robin across healthy workers
        int index = Math.abs(counter.getAndIncrement() % workers.size());
        WorkerInfo worker = workers.get(index);

        System.out.println("[Proxy] Forwarding to " + worker);

        try (clientSocket;
             Socket workerSocket = new Socket(worker.host, worker.port)) {

            workerSocket.setSoTimeout(WORKER_TIMEOUT_MS);
            relay(clientSocket, workerSocket);
            circuitBreaker.recordSuccess(worker.id);

        } catch (SocketTimeoutException e) {
            System.err.println("[Proxy] Timeout on " + worker.id);
            circuitBreaker.recordTimeout(worker.id);
            sendErrorAndClose("504 Gateway Timeout\n");
        } catch (IOException e) {
            System.err.println("[Proxy] Error relaying to " + worker.id + ": " + e.getMessage());
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

    private void sendErrorAndClose(String message) {
        try (clientSocket) {
            clientSocket.getOutputStream().write(message.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {}
    }
}
