package com.tencent.polaris.configuration.client.internal;

import java.util.concurrent.TimeUnit;

/**
 * @author lepdou 2022-03-04
 */
public class ExponentialRetryPolicy implements RetryPolicy {

    private final long delayMinTime;
    private final long delayMaxTime;

    private long currentDelayTime;

    public ExponentialRetryPolicy(long delayMinTime, long delayMaxTime) {
        this.delayMinTime = delayMinTime;
        this.delayMaxTime = delayMaxTime;
    }

    @Override
    public void success() {
        currentDelayTime = 0;
    }

    @Override
    public void fail() {
        long delayTime = currentDelayTime;

        if (delayTime == 0) {
            delayTime = delayMinTime;
        } else {
            delayTime = Math.min(currentDelayTime << 1, delayMaxTime);
        }

        currentDelayTime = delayTime;
    }

    @Override
    public long getCurrentDelayTime() {
        return currentDelayTime;
    }

    @Override
    public void executeDelay() {
        try {
            TimeUnit.SECONDS.sleep(currentDelayTime);
        } catch (InterruptedException e) {
            //ignore
        }
    }
}
