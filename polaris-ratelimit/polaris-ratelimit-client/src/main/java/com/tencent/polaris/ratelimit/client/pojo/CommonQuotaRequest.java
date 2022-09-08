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

package com.tencent.polaris.ratelimit.client.pojo;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.plugin.ratelimiter.InitCriteria;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.pojo.ServiceEventKeysProvider;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.pojo.ServiceRule;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.flow.BaseFlow;
import com.tencent.polaris.client.flow.DefaultFlowControlParam;
import com.tencent.polaris.client.flow.FlowControlParam;
import com.tencent.polaris.client.pb.RateLimitProto.Rule;
import com.tencent.polaris.ratelimit.api.rpc.QuotaRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CommonQuotaRequest implements ServiceEventKeysProvider {

    private final ServiceEventKey svcEventKey;

    private final String method;

    private final Map<String, String> labels;

    private final int count;

    //服务规则
    private ServiceRule rateLimitRule;

    //实际命中的规则
    private final InitCriteria initCriteria;

    //规则中含有正则表达式扩散
    private boolean regexSpread;

    private final FlowControlParam flowControlParam;

    public CommonQuotaRequest(QuotaRequest quotaRequest, Configuration configuration) {
        svcEventKey = new ServiceEventKey(new ServiceKey(quotaRequest.getNamespace(), quotaRequest.getService()),
                EventType.RATE_LIMITING);
        if (null == quotaRequest.getLabels()) {
            labels = new HashMap<>();
        } else {
            labels = quotaRequest.getLabels();
        }
        method = quotaRequest.getMethod();
        if (StringUtils.isNotBlank(method)) {
            labels.put("method", method);
        }
        count = quotaRequest.getCount();
        initCriteria = new InitCriteria();
        this.flowControlParam = new DefaultFlowControlParam();
        BaseFlow.buildFlowControlParam(quotaRequest, configuration, flowControlParam);
    }

    public void setRateLimitRule(ServiceRule rateLimitRule) {
        this.rateLimitRule = rateLimitRule;
    }

    public void setRegexSpread(boolean regexSpread) {
        this.regexSpread = regexSpread;
    }

    public boolean isRegexSpread() {
        return regexSpread;
    }

    public void setTargetRule(Rule targetRule) {
        this.initCriteria.setRule(targetRule);
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

    public Map<String, String> getLabels() {
        return labels;
    }

    public ServiceRule getRateLimitRule() {
        return rateLimitRule;
    }

    public InitCriteria getInitCriteria() {
        return initCriteria;
    }

    public FlowControlParam getFlowControlParam() {
        return flowControlParam;
    }

    public int getCount() {
        return count;
    }

}
