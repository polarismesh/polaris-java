/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.polaris.plugins.flow.cache.expired;

import java.util.concurrent.atomic.AtomicLong;

public class FlowCacheItem {

    private final AtomicLong lastAccessTimeMs = new AtomicLong();

    /**
     * 获取最近一次访问时间
     *
     * @return 访问时间
     */
    public long getLastAccessTimeMs() {
        return lastAccessTimeMs.get();
    }

    /**
     * 更新最近一次访问时间
     */
    public void updateLastAccessTimeMs() {
        long lastAccessTime = lastAccessTimeMs.get();
        lastAccessTimeMs.compareAndSet(lastAccessTime, System.currentTimeMillis());
    }
}
