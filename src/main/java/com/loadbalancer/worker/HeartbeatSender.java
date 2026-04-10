package com.loadbalancer.worker;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HeartbeatSender {

    private final String workerId;
    private final int workerPort;
    private final InetAddress proxyAddress;
    private final int proxyUdpPort;
    private final DatagramSocket socket;
    private final ScheduledExecutorService scheduler;

    public HeartbeatSender(String workerId, int workerPort, String proxyHost, int proxyUdpPort) throws IOException {
        this.workerId = workerId;
        this.workerPort = workerPort;
        this.proxyAddress = InetAddress.getByName(proxyHost);
        this.proxyUdpPort = proxyUdpPort;
        this.socket = new DatagramSocket();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::sendHeartbeat, 0, 500, TimeUnit.MILLISECONDS);
        System.out.println("[" + workerId + "] Heartbeat started -> " + proxyAddress + ":" + proxyUdpPort);
    }

    public void stop() {
        scheduler.shutdown();
        socket.close();
    }

    private void sendHeartbeat() {
        try {
            // Hand-rolled JSON — no external library needed
            String payload = "{\"id\":\"" + workerId + "\",\"port\":" + workerPort + ",\"load\":0.0}";
            byte[] data = payload.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, proxyAddress, proxyUdpPort);
            socket.send(packet);
        } catch (IOException e) {
            if (!socket.isClosed()) {
                System.err.println("[" + workerId + "] Heartbeat send failed: " + e.getMessage());
            }
        }
    }
}
