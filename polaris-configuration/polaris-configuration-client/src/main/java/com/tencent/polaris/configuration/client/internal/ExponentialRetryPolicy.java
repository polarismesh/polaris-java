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
