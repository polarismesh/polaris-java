package com.tencent.polaris.ratelimit.client.flow;

/**
 * duration 对应的Window
 */
public class DurationBaseCallback {

    /**
     * duration
     */
    private final int duration;

    /**
     * 限流窗口
     */
    private final RateLimitWindow rateLimitWindow;


    public DurationBaseCallback(int duration, RateLimitWindow rateLimitWindow) {
        this.duration = duration;
        this.rateLimitWindow = rateLimitWindow;
    }

    public int getDuration() {
        return duration;
    }

    public RateLimitWindow getRateLimitWindow() {
        return rateLimitWindow;
    }
}
