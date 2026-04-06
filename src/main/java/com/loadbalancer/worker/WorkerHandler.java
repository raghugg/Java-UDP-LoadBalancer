package com.loadbalancer.worker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class WorkerHandler implements Runnable {

    private final Socket clientSocket;
    private final String workerId;

    public WorkerHandler(Socket clientSocket, String workerId) {
        this.clientSocket = clientSocket;
        this.workerId = workerId;
    }

    @Override
    public void run() {
        try (clientSocket;
             BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String line;
            StringBuilder request = new StringBuilder();
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                request.append(line).append("\n");
            }

            System.out.println("[" + workerId + "] Received request:\n" + request);

            out.println("Response from " + workerId);

        } catch (IOException e) {
            System.err.println("[" + workerId + "] Error handling connection: " + e.getMessage());
        }
    }
}
