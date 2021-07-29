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

package com.tencent.polaris.plugins.circuitbreaker.common.stat;

/**
 * 时间段
 *
 * @author andrewshan
 * @date 2019/8/25
 */
public class TimeRange {

    private final long start;

    private final long end;

    public TimeRange(long start, long end) {
        this.start = start;
        this.end = end;
    }

    /**
     * 判断时间点是否在范围中
     *
     * @param inputTime 输出时间
     * @return 时间点位置
     */
    public TimePosition isTimeInBucket(long inputTime) {
        if (inputTime < start) {
            return TimePosition.before;
        }
        if (inputTime >= start && inputTime < end) {
            return TimePosition.inside;
        }
        return TimePosition.after;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public String toString() {
        return "TimeRange{" +
                "start=" + start +
                ", end=" + end +
                '}';
    }
}
