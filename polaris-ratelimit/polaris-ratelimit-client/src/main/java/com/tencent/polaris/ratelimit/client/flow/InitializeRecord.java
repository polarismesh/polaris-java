package com.tencent.polaris.ratelimit.client.flow;

import com.google.common.collect.Maps;
import java.util.Map;

/**
 * 初始化的记录
 */
public class InitializeRecord {

    /**
     * 限流窗口
     */
    private final RateLimitWindow rateLimitWindow;

    /**
     * duration 对应记录duration -> counterKey
     */
    private final Map<Integer, Integer> durationRecord = Maps.newConcurrentMap();

    public InitializeRecord(RateLimitWindow rateLimitWindow) {
        this.rateLimitWindow = rateLimitWindow;
    }

    /**
     * 获取duration对应关系
     *
     * @return duration对应关系
     */
    public Map<Integer, Integer> getDurationRecord() {
        return durationRecord;
    }

    public RateLimitWindow getRateLimitWindow() {
        return rateLimitWindow;
    }
}
