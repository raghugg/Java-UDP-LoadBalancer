package com.loadbalancer.proxy;

import com.loadbalancer.model.WorkerInfo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.ConcurrentHashMap;

public class HeartbeatReceiver {

    private final int udpPort;
    private final ConcurrentHashMap<String, WorkerInfo> registry;
    private DatagramSocket socket;

    public HeartbeatReceiver(int udpPort, ConcurrentHashMap<String, WorkerInfo> registry) {
        this.udpPort = udpPort;
        this.registry = registry;
    }

    public void start() throws IOException {
        socket = new DatagramSocket(udpPort);
        Thread t = new Thread(this::receiveLoop, "heartbeat-receiver");
        t.setDaemon(true);
        t.start();
        System.out.println("[Proxy] Heartbeat receiver listening on UDP port " + udpPort);
    }

    public void stop() {
        if (socket != null) socket.close();
    }

    private void receiveLoop() {
        byte[] buf = new byte[256];
        while (!socket.isClosed()) {
            try {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);

                String json = new String(packet.getData(), 0, packet.getLength());
                String senderHost = packet.getAddress().getHostAddress();

                String id = extractField(json, "id");
                int workerPort = Integer.parseInt(extractField(json, "port"));
                double load = Double.parseDouble(extractField(json, "load"));

                WorkerInfo info = registry.computeIfAbsent(id, k -> {
                    System.out.println("[Proxy] New worker registered: " + id + " @ " + senderHost + ":" + workerPort);
                    return new WorkerInfo(id, senderHost, workerPort);
                });

                info.load = load;
                info.lastSeenMs = System.currentTimeMillis();

            } catch (IOException e) {
                if (!socket.isClosed()) {
                    System.err.println("[Proxy] Heartbeat receive error: " + e.getMessage());
                }
            }
        }
    }

    // Minimal JSON field extractor — no external library needed
    // Handles: {"id":"worker-1","port":8081,"load":0.5}
    private String extractField(String json, String key) {
        String search = "\"" + key + "\"";
        int keyIdx = json.indexOf(search);
        if (keyIdx == -1) throw new IllegalArgumentException("Key not found: " + key);
        int colon = json.indexOf(':', keyIdx);
        String rest = json.substring(colon + 1).trim();
        if (rest.startsWith("\"")) {
            int start = rest.indexOf('"') + 1;
            int end = rest.indexOf('"', start);
            return rest.substring(start, end);
        } else {
            int end = rest.indexOf(',');
            if (end == -1) end = rest.indexOf('}');
            return rest.substring(0, end == -1 ? rest.length() : end).trim();
        }
    }
}
