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

package com.tencent.polaris.plugins.ratelimiter.common.slide;

import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;

public class SlidingWindow {

    private static final Logger LOG = LoggerFactory.getLogger(SlidingWindow.class);

    private final long windowLengthMs;

    private final Object lock = new Object();

    private final Window[] windowArray;

    private final int slideCount;

    public SlidingWindow(int slideCount, long intervalMs) {
        this.slideCount = slideCount;
        this.windowLengthMs = intervalMs / slideCount;
        this.windowArray = new Window[slideCount];
        for (int i = 0; i < slideCount; i++) {
            this.windowArray[i] = new Window();
        }
    }

    private int calculateTimeIdx(long curTimeMs) {
        long timeId = curTimeMs / windowLengthMs;
        return (int) (timeId % slideCount);
    }

    public static long calculateStartTimeMs(long curTimeMs, long intervalMs) {
        return curTimeMs - curTimeMs % intervalMs;
    }

    private long calculateWindowStartMs(long curTimeMs) {
        return calculateStartTimeMs(curTimeMs, this.windowLengthMs);
    }

    public Window currentWindow(long curTimeMs) {
        int idx = calculateTimeIdx(curTimeMs);
        long windowStartMs = calculateWindowStartMs(curTimeMs);
        Window curWindow = windowArray[idx];
        long oldWindowStartMs = curWindow.getCurrentWindowStartMs();
        if (oldWindowStartMs == windowStartMs) {
            return curWindow;
        }
        synchronized (lock) {
            curWindow.reset(oldWindowStartMs, windowStartMs);
            return curWindow;
        }
    }

    /**
     * 增加通过数
     *
     * @param curTimeMs 当前时间点
     * @param value 通过数
     */
    public void addAndGetCurrentPassed(long curTimeMs, long value) {
        Window curWindow = currentWindow(curTimeMs);
        curWindow.addAndGetPassed(value);
        LOG.info("add passed value: passed {}, curWindow {}, curTimeMs {}", value,
                curWindow.getCurrentWindowStartMs(), curTimeMs);
    }

    /**
     * 增加限流数
     *
     * @param curTimeMs 当前时间点
     * @param value 限流数
     */
    public void addAndGetCurrentLimited(long curTimeMs, long value) {
        Window curWindow = currentWindow(curTimeMs);
        curWindow.addAndGetLimited(value);
        LOG.info("add limited value: passed {}, curWindow {}, curTimeMs {}", value,
                curWindow.getCurrentWindowStartMs(), curTimeMs);
    }

    /**
     * 获取上报数据，并重置当前统计数据
     *
     * @param curTimeMs 当前时间点
     * @return 上报数据
     */
    public Result acquireCurrentValues(long curTimeMs) {
        Window curWindow = currentWindow(curTimeMs);
        long passed = curWindow.swapPassed();
        long limited = curWindow.swapLimited();
        LOG.info("acquire current value: passed {}, limited {}, curWindow {}, curTimeMs {}", passed, limited,
                curWindow.getCurrentWindowStartMs(), curTimeMs);
        return new Result(passed, limited);
    }

    /**
     * 直接获取通过数据，不重置
     *
     * @param curTimeMs 当前时间点
     * @return 通过数据
     */
    public long touchCurrentPassed(long curTimeMs) {
        Window curWindow = currentWindow(curTimeMs);
        return curWindow.getPassedValue();
    }


    public static class Result {

        private final long passed;

        private final long limited;

        public Result(long passed, long limited) {
            this.passed = passed;
            this.limited = limited;
        }

        public long getPassed() {
            return passed;
        }

        public long getLimited() {
            return limited;
        }
    }
}

