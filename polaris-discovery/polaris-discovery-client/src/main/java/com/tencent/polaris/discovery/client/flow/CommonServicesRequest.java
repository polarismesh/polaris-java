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

package com.tencent.polaris.discovery.client.flow;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.pojo.ServiceEventKeysProvider;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.GetServicesRequest;
import com.tencent.polaris.client.flow.BaseFlow;
import com.tencent.polaris.client.flow.FlowControlParam;

import java.util.Collections;
import java.util.Set;

/**
 * 批量获取服务的封装请求
 */
public class CommonServicesRequest implements ServiceEventKeysProvider, FlowControlParam {

    private final EventType eventType = EventType.SERVICE;

    private long timeoutMs;

    private int maxRetry;

    private long retryIntervalMs;

    private GetServicesRequest request;

    public CommonServicesRequest(GetServicesRequest request, Configuration configuration) {
        this.request = request;
        BaseFlow.buildFlowControlParam(request, configuration, this);
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

    public EventType getEventType() {
        return eventType;
    }

    public GetServicesRequest getRequest() {
        return request;
    }

    public void setRequest(GetServicesRequest request) {
        this.request = request;
    }

    @Override
    public boolean isUseCache() {
        return false;
    }

    @Override
    public Set<ServiceEventKey> getSvcEventKeys() {
        return Collections.singleton(getSvcEventKey());
    }

    @Override
    public ServiceEventKey getSvcEventKey() {
        return new ServiceEventKey(new ServiceKey(request.getNamespace(), ""), EventType.SERVICE);
    }
}
