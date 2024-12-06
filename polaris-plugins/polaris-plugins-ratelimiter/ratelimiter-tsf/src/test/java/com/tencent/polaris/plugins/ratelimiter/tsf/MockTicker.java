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
