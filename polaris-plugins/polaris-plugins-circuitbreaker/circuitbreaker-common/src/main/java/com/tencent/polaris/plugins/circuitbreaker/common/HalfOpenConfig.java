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

package com.tencent.polaris.plugins.circuitbreaker.common;

import com.tencent.polaris.api.config.consumer.CircuitBreakerConfig;
import com.tencent.polaris.api.config.consumer.OutlierDetectionConfig;
import com.tencent.polaris.api.config.consumer.OutlierDetectionConfig.When;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto.RecoverConfig;

public class HalfOpenConfig {

    private final int halfOpenMaxReqCount;

    private final int halfOpenSuccessCount;

    private final int halfOpenFailCount;

    private final long sleepWindowMs;

    private final OutlierDetectionConfig.When whenToDetect;

    public HalfOpenConfig(CircuitBreakerConfig circuitBreakerConfig, OutlierDetectionConfig outlierDetectionConfig) {
        halfOpenMaxReqCount = circuitBreakerConfig.getRequestCountAfterHalfOpen();
        int successCountAfterHalfOpen = circuitBreakerConfig.getSuccessCountAfterHalfOpen();
        if (successCountAfterHalfOpen > halfOpenMaxReqCount) {
            successCountAfterHalfOpen = halfOpenMaxReqCount;
        }
        int halfOpenFailCount = halfOpenMaxReqCount - successCountAfterHalfOpen + 1;
        if (halfOpenFailCount > halfOpenMaxReqCount) {
            halfOpenFailCount = halfOpenMaxReqCount;
        }
        this.sleepWindowMs = circuitBreakerConfig.getSleepWindow();
        this.halfOpenSuccessCount = successCountAfterHalfOpen;
        this.halfOpenFailCount = halfOpenFailCount;
        this.whenToDetect = outlierDetectionConfig.getWhen();
    }

    public HalfOpenConfig(HalfOpenConfig halfOpenConfig, RecoverConfig recoverConfig) {
        this.halfOpenMaxReqCount = halfOpenConfig.getHalfOpenMaxReqCount();
        this.sleepWindowMs = halfOpenConfig.getSleepWindowMs();
        this.halfOpenSuccessCount = halfOpenConfig.getHalfOpenSuccessCount();
        this.halfOpenFailCount = halfOpenConfig.getHalfOpenFailCount();
        if (null != recoverConfig) {
            this.whenToDetect = OutlierDetectionConfig.When.values()[recoverConfig.getOutlierDetectWhen().getNumber()];
        } else {
            this.whenToDetect = halfOpenConfig.getWhenToDetect();
        }
    }

    public int getHalfOpenMaxReqCount() {
        return halfOpenMaxReqCount;
    }

    public int getHalfOpenSuccessCount() {
        return halfOpenSuccessCount;
    }

    public int getHalfOpenFailCount() {
        return halfOpenFailCount;
    }

    public long getSleepWindowMs() {
        return sleepWindowMs;
    }

    public When getWhenToDetect() {
        return whenToDetect;
    }

}
