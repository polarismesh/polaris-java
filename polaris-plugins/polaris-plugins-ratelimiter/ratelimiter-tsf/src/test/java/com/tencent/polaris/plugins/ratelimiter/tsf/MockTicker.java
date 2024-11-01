package com.tencent.polaris.plugins.ratelimiter.tsf;

import com.google.common.base.Ticker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Haotian Zhang
 */
public class MockTicker extends Ticker {
    private final List<Long> timestamps = new ArrayList<>();

    private final AtomicInteger index = new AtomicInteger(0);

    public void addTimestamp(long timestamp) {
        this.timestamps.add(timestamp);
    }

    @Override
    public long read() {
        int next = index.getAndIncrement();
        return timestamps.get(next);
    }

    public void clear() {
        timestamps.clear();
    }
}
