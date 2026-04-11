package com.loadbalancer.proxy;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class CircuitBreaker {

    private static final int FAILURE_THRESHOLD = 3;

    private final ConcurrentHashMap<String, AtomicInteger> failureCounts = new ConcurrentHashMap<>();
    private final Set<String> openBreakers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public void recordTimeout(String workerId) {
        AtomicInteger count = failureCounts.computeIfAbsent(workerId, k -> new AtomicInteger(0));
        int failures = count.incrementAndGet();
        if (failures >= FAILURE_THRESHOLD) {
            openBreakers.add(workerId);
            System.out.println("[CircuitBreaker] OPEN for " + workerId + " after " + failures + " timeouts");
        }
    }

    public void recordSuccess(String workerId) {
        failureCounts.remove(workerId);
        openBreakers.remove(workerId);
    }

    public boolean isOpen(String workerId) {
        return openBreakers.contains(workerId);
    }
}
