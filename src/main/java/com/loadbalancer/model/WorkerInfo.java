package com.loadbalancer.model;

public class WorkerInfo {

    public final String id;
    public final String host;
    public final int port;
    public volatile double load;
    public volatile long lastSeenMs;

    public WorkerInfo(String id, String host, int port) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.lastSeenMs = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return id + "@" + host + ":" + port + " (load=" + load + ")";
    }
}
