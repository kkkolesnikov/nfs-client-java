package com.emc.ecs.nfsclient.network;

import java.util.concurrent.atomic.AtomicLong;

public class CallMetric {
    private final AtomicLong latency = new AtomicLong(0);
    private final AtomicLong count = new AtomicLong(0);

    public void add(long nanos) {
        count.incrementAndGet();
        latency.addAndGet(nanos / 1000);
    }

    public long getAndReset() {
        long avg = latency.get() / count.get();
        latency.set(0);
        count.set(0);

        return avg;
    }
}
