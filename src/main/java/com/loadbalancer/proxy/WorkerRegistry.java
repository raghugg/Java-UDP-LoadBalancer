package com.loadbalancer.proxy;

import com.loadbalancer.model.WorkerInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WorkerRegistry {

    private static final long PURGE_THRESHOLD_MS = 2000;

    private final ConcurrentHashMap<String, WorkerInfo> liveWorkers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public WorkerRegistry() {
        // Check every second for stale workers
        scheduler.scheduleAtFixedRate(this::purgeStale, 1, 1, TimeUnit.SECONDS);
    }

    public ConcurrentHashMap<String, WorkerInfo> getMap() {
        return liveWorkers;
    }

    public List<WorkerInfo> getLiveWorkers() {
        return new ArrayList<>(liveWorkers.values());
    }

    public void stop() {
        scheduler.shutdown();
    }

    private void purgeStale() {
        long now = System.currentTimeMillis();
        liveWorkers.entrySet().removeIf(entry -> {
            boolean stale = now - entry.getValue().lastSeenMs > PURGE_THRESHOLD_MS;
            if (stale) {
                System.out.println("[Proxy] Worker purged (no heartbeat): " + entry.getKey());
            }
            return stale;
        });
    }
}
