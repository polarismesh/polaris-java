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

package com.tencent.polaris.discovery.client.flow;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.pojo.ServiceEventKeysProvider;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.GetServicesRequest;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.flow.BaseFlow;
import com.tencent.polaris.client.flow.FlowControlParam;

import java.util.Collections;
import java.util.Set;

/**
 * 批量获取服务的封装请求
 */
public class CommonServicesRequest implements ServiceEventKeysProvider, FlowControlParam {

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
        return getSvcEventKey().getEventType();
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
        return buildSvcEventKey(request);
    }

    /**
     * Builds the discover key. A named service uses INSTANCE so revision and
     * extended_metadata stay bound to that service; otherwise SERVICES lists the namespace.
     *
     * @param request the get-services request
     * @return discover cache key
     */
    static ServiceEventKey buildSvcEventKey(GetServicesRequest request) {
        String namespace = request.getNamespace();
        String service = request.getService();
        ServiceEventKey eventKey;
        if (StringUtils.isNotBlank(service)) {
            eventKey = new ServiceEventKey(new ServiceKey(namespace, service), EventType.INSTANCE);
        } else {
            eventKey = new ServiceEventKey(new ServiceKey(namespace, ""), EventType.SERVICE);
        }
        return eventKey;
    }
}
