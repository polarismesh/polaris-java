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

package com.tencent.polaris.circuitbreak.client.api;

import com.tencent.polaris.api.config.verify.DefaultValues;
import com.tencent.polaris.api.plugin.circuitbreaker.ResourceStat;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.InstanceResource;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.Resource;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.pojo.InstanceGauge;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.client.api.ServiceCallResultListener;

public class InstanceCircuitBreakerReporter implements ServiceCallResultListener {

    private Extensions extensions;

    @Override
    public void init(SDKContext context) {
        extensions = context.getExtensions();
    }

    @Override
    public void onServiceCallResult(InstanceGauge result) {
        if (!needReportStat(result)) {
            return;
        }
        ServiceKey sourceService = null;
        if (null != result.getCallerService()) {
            sourceService = new ServiceKey(result.getCallerService().getNamespace(),
                    result.getCallerService().getService());
        }
        ServiceKey serviceKey = new ServiceKey(result.getNamespace(), result.getService());
        Resource resource = new InstanceResource(serviceKey, result.getHost(), result.getPort(), sourceService);
        int retCode = 0;
        if (null != result.getRetCode()) {
            retCode = result.getRetCode();
        }
        long delay = 0;
        if (null != result.getDelay()) {
            delay = result.getDelay();
        }
        RetStatus retStatus = RetStatus.RetUnknown;
        if (null != result.getRetStatus()) {
            retStatus = result.getRetStatus();
        }
        ResourceStat resourceStat = new ResourceStat(resource, retCode, delay, retStatus);
        DefaultCircuitBreakAPI.report(resourceStat, extensions);
    }

    private static boolean needReportStat(InstanceGauge result) {
        if (StringUtils.isBlank(result.getService()) || StringUtils.isBlank(result.getNamespace())) {
            return false;
        }
        if (StringUtils.equals(result.getNamespace(), DefaultValues.DEFAULT_SYSTEM_NAMESPACE)
                && StringUtils.equals(result.getNamespace(), DefaultValues.DEFAULT_BUILTIN_DISCOVER)) {
            return false;
        }
        return true;
    }

    @Override
    public void destroy() {

    }
}
