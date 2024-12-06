/*
 * Tencent is pleased to support the open source community by making Polaris available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

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

    private long initStartTimeMilli;

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

    public long getInitStartTimeMilli() {
        return initStartTimeMilli;
    }

    public void setInitStartTimeMilli(long initStartTimeMilli) {
        this.initStartTimeMilli = initStartTimeMilli;
    }
}
