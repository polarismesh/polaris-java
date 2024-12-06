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

package com.tencent.polaris.ratelimit.client.pojo;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.pojo.ServiceEventKeysProvider;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.pojo.ServiceRule;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.client.flow.BaseFlow;
import com.tencent.polaris.client.flow.DefaultFlowControlParam;
import com.tencent.polaris.client.flow.FlowControlParam;
import com.tencent.polaris.metadata.core.manager.MetadataContext;
import com.tencent.polaris.ratelimit.api.rpc.Argument;
import com.tencent.polaris.ratelimit.api.rpc.QuotaRequest;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CommonQuotaRequest implements ServiceEventKeysProvider {

    private final ServiceEventKey svcEventKey;

    private final String method;

    private final Map<Integer, Map<String, String>> arguments;

    private final int count;

    private final MetadataContext metadataContext;

    //服务规则
    private ServiceRule rateLimitRule;

    private final FlowControlParam flowControlParam;

    private final long currentTimestamp;

    public CommonQuotaRequest(QuotaRequest quotaRequest, Configuration configuration) {
        svcEventKey = new ServiceEventKey(new ServiceKey(quotaRequest.getNamespace(), quotaRequest.getService()),
                EventType.RATE_LIMITING);
        arguments = parseArguments(quotaRequest.getArguments());
        metadataContext = quotaRequest.getMetadataContext();
        method = quotaRequest.getMethod();
        count = quotaRequest.getCount();
        flowControlParam = new DefaultFlowControlParam();
        BaseFlow.buildFlowControlParam(quotaRequest, configuration, flowControlParam);
        currentTimestamp = System.currentTimeMillis();
    }

    private Map<Integer, Map<String, String>> parseArguments(Collection<Argument> arguments) {
        Map<Integer, Map<String, String>> argumentMap = new HashMap<>();
        if (CollectionUtils.isEmpty(arguments)) {
            return argumentMap;
        }
        for (Argument argument : arguments) {
            Map<String, String> stringMatchArgumentMap = argumentMap
                    .computeIfAbsent(argument.getType().ordinal(), k -> new HashMap<>());
            stringMatchArgumentMap.put(argument.getKey(), argument.getValue());
        }
        return argumentMap;
    }

    public void setRateLimitRule(ServiceRule rateLimitRule) {
        this.rateLimitRule = rateLimitRule;
    }

    @Override
    public boolean isUseCache() {
        return false;
    }

    @Override
    public Set<ServiceEventKey> getSvcEventKeys() {
        return null;
    }

    @Override
    public ServiceEventKey getSvcEventKey() {
        return svcEventKey;
    }

    public String getMethod() {
        return method;
    }

    @Deprecated
    public Map<Integer, Map<String, String>> getArguments() {
        return arguments;
    }

    public MetadataContext getMetadataContext() {
        return metadataContext;
    }

    public ServiceRule getRateLimitRule() {
        return rateLimitRule;
    }

    public FlowControlParam getFlowControlParam() {
        return flowControlParam;
    }

    public long getCurrentTimestamp() {
        return currentTimestamp;
    }

    public int getCount() {
        return count;
    }

}
