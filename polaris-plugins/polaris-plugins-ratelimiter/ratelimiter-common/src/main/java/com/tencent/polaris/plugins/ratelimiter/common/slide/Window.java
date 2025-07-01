/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
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

package com.tencent.polaris.plugins.ratelimiter.common.slide;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 单个滑窗
 */
public class Window {

    private final AtomicLong windowStartMs;

    private final AtomicLong passedValue;

    private final AtomicLong limitedValue;

    public Window(long windStartMs, long passed, long limited) {
        this.windowStartMs = new AtomicLong(windStartMs);
        this.passedValue = new AtomicLong(passed);
        this.limitedValue = new AtomicLong(limited);
    }

    public Window() {
        this(0, 0, 0);
    }


    /**
     * 重置滑窗
     *
     * @param oldWindowStartMs 淘汰的时间起始点
     * @param curWindowStartMs 新的时间起始点
     * @return 旧的滑窗
     */
    public Window reset(long oldWindowStartMs, long curWindowStartMs) {
        if (windowStartMs.compareAndSet(oldWindowStartMs, curWindowStartMs)) {
            long passed = swapPassed();
            long limited = swapLimited();
            return new Window(oldWindowStartMs, passed, limited);
        }
        return null;
    }

    /**
     * 获取当前的滑窗起始时间
     *
     * @return 滑窗起始时间
     */
    public long getCurrentWindowStartMs() {
        return windowStartMs.get();
    }

    /**
     * 原子增加通过数
     *
     * @param value 通过数
     * @return 增加后数量
     */
    public long addAndGetPassed(long value) {
        return passedValue.addAndGet(value);
    }

    /**
     * 原子增加被限流数
     *
     * @param value 被限流数
     * @return 增加后数量
     */
    public long addAndGetLimited(long value) {
        return limitedValue.addAndGet(value);
    }

    /**
     * 原子获取通过数
     *
     * @return 通过数
     */
    public long swapPassed() {
        return passedValue.getAndSet(0);
    }

    /**
     * 原子获取限流数
     *
     * @return 限流数
     */
    public long swapLimited() {
        return limitedValue.getAndSet(0);
    }

    /**
     * 获取通过数
     *
     * @return 通过数
     */
    public long getPassedValue() {
        return passedValue.get();
    }
}
