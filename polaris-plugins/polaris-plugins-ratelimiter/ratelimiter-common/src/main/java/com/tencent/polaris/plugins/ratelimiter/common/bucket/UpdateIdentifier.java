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

package com.tencent.polaris.plugins.ratelimiter.common.bucket;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 令牌桶是否进行更新的凭证
 */
public class UpdateIdentifier {

    private final AtomicLong stageStartMs = new AtomicLong(0);

    private final AtomicLong lastRemoteClientUpdateMs = new AtomicLong(0);

    private final AtomicLong lastRemoteUpdateMs = new AtomicLong(0);

    public AtomicLong getStageStartMs() {
        return stageStartMs;
    }

    public AtomicLong getLastRemoteClientUpdateMs() {
        return lastRemoteClientUpdateMs;
    }

    public AtomicLong getLastRemoteUpdateMs() {
        return lastRemoteUpdateMs;
    }
}
