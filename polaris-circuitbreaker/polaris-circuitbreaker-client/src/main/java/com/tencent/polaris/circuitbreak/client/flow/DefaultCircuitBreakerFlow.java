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

package com.tencent.polaris.circuitbreak.client.flow;

import com.tencent.polaris.api.config.consumer.CircuitBreakerConfig;
import com.tencent.polaris.api.config.global.FlowConfig;
import com.tencent.polaris.api.plugin.circuitbreaker.CircuitBreaker;
import com.tencent.polaris.api.plugin.circuitbreaker.ResourceStat;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.Resource;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.pojo.CircuitBreakerStatus;
import com.tencent.polaris.api.pojo.CircuitBreakerStatus.Status;
import com.tencent.polaris.circuitbreak.api.flow.CircuitBreakerFlow;
import com.tencent.polaris.circuitbreak.api.pojo.CheckResult;
import com.tencent.polaris.client.api.SDKContext;

public class DefaultCircuitBreakerFlow implements CircuitBreakerFlow {

    private SDKContext sdkContext;

    @Override
    public String getName() {
        return FlowConfig.DEFAULT_FLOW_NAME;
    }

    @Override
    public void setSDKContext(SDKContext sdkContext) {
        this.sdkContext = sdkContext;
    }

    @Override
    public CheckResult check(Resource resource) {
        return check(resource, sdkContext.getExtensions());
    }

    private static CheckResult circuitBreakerStatusToResult(CircuitBreakerStatus circuitBreakerStatus) {
        Status status = circuitBreakerStatus.getStatus();
        if (status == Status.OPEN) {
            return new CheckResult(false, circuitBreakerStatus.getCircuitBreaker(),
                    circuitBreakerStatus.getFallbackInfo());
        }
        return new CheckResult(true, circuitBreakerStatus.getCircuitBreaker(),
                circuitBreakerStatus.getFallbackInfo());
    }

    private static CheckResult check(Resource resource, Extensions extensions) {
        CircuitBreaker circuitBreaker = extensions.getResourceBreaker();
        if (null == circuitBreaker) {
            return new CheckResult(true, "", null);
        }
        CircuitBreakerStatus circuitBreakerStatus = circuitBreaker.checkResource(resource);
        if (null != circuitBreakerStatus) {
            return circuitBreakerStatusToResult(circuitBreakerStatus);
        }
        return new CheckResult(true, "", null);
    }

    private static void report(ResourceStat reportStat, Extensions extensions) {
        CircuitBreaker circuitBreaker = extensions.getResourceBreaker();
        if (null == circuitBreaker) {
            return;
        }
        circuitBreaker.report(reportStat);
    }


    @Override
    public void report(ResourceStat reportStat) {
        report(reportStat, sdkContext.getExtensions());
    }
}
