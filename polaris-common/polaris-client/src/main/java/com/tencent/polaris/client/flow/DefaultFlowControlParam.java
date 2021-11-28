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

package com.tencent.polaris.client.flow;

import com.tencent.polaris.api.config.global.APIConfig;

public class DefaultFlowControlParam implements FlowControlParam {

    private long timeoutMs;

    private long retryIntervalMs;

    private int maxRetry;

    public DefaultFlowControlParam() {
    }

    public DefaultFlowControlParam(APIConfig apiConfig) {
        this.timeoutMs = apiConfig.getTimeout();
        this.retryIntervalMs = apiConfig.getRetryInterval();
        this.maxRetry = apiConfig.getMaxRetryTimes();
    }

    @Override
    public long getTimeoutMs() {
        return timeoutMs;
    }

    @Override
    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    @Override
    public long getRetryIntervalMs() {
        return retryIntervalMs;
    }

    @Override
    public void setRetryIntervalMs(long retryIntervalMs) {
        this.retryIntervalMs = retryIntervalMs;
    }

    @Override
    public int getMaxRetry() {
        return maxRetry;
    }

    @Override
    public void setMaxRetry(int maxRetry) {
        this.maxRetry = maxRetry;
    }
}
