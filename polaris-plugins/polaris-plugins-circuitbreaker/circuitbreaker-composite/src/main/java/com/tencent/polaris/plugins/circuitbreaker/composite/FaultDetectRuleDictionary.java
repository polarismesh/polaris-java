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

package com.tencent.polaris.plugins.circuitbreaker.composite;

import com.tencent.polaris.api.plugin.circuitbreaker.entity.Resource;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.pojo.ServiceRule;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.CompareUtils;
import com.tencent.polaris.specification.api.v1.fault.tolerance.FaultDetectorProto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FaultDetectRuleDictionary {

    // key is target service, value is list FaultDetectRule
    private final Map<ServiceKey, List<FaultDetectorProto.FaultDetectRule>> serviceRules = new ConcurrentHashMap<>();

    private final Object updateLock = new Object();

    public List<FaultDetectorProto.FaultDetectRule> lookupFaultDetectRule(Resource resource) {
        ServiceKey targetService = resource.getService();
        return serviceRules.get(targetService);
    }

    /**
     * rule on server has been changed, clear all caches to make it pull again
     *
     * @param svcKey target service
     */
    public void onFaultDetectRuleChanged(ServiceKey svcKey, FaultDetectorProto.FaultDetector faultDetector) {
        synchronized (updateLock) {
            putServiceRule(svcKey, faultDetector);
        }
    }

    void onFaultDetectRuleDeleted(ServiceKey svcKey) {
        synchronized (updateLock) {
            serviceRules.remove(svcKey);
        }
    }

    public void putServiceRule(ServiceKey serviceKey, ServiceRule serviceRule) {
        synchronized (updateLock) {
            if (null == serviceRule) {
                serviceRules.remove(serviceKey);
                return;
            }
            putServiceRule(serviceKey, (FaultDetectorProto.FaultDetector) serviceRule.getRule());
        }
    }

    public void putServiceRule(ServiceKey serviceKey, FaultDetectorProto.FaultDetector faultDetector) {
        if (null == faultDetector) {
            serviceRules.remove(serviceKey);
            return;
        }
        List<FaultDetectorProto.FaultDetectRule> rules = faultDetector.getRulesList();
        if (CollectionUtils.isNotEmpty(rules)) {
            rules = sortFaultDetectRules(rules);
        }
        serviceRules.put(serviceKey, rules);
    }

    private static List<FaultDetectorProto.FaultDetectRule> sortFaultDetectRules(List<FaultDetectorProto.FaultDetectRule> rules) {
        List<FaultDetectorProto.FaultDetectRule> outRules = new ArrayList<>(rules);
        outRules.sort(new Comparator<FaultDetectorProto.FaultDetectRule>() {
            @Override
            public int compare(FaultDetectorProto.FaultDetectRule rule1, FaultDetectorProto.FaultDetectRule rule2) {
                // 1. compare destination service
                FaultDetectorProto.FaultDetectRule.DestinationService targetService1 = rule1.getTargetService();
                String destNamespace1 = targetService1.getNamespace();
                String destService1 = targetService1.getService();
                String destMethod1 = targetService1.getMethod().getValue().getValue();

                FaultDetectorProto.FaultDetectRule.DestinationService targetService2 = rule2.getTargetService();
                String destNamespace2 = targetService2.getNamespace();
                String destService2 = targetService2.getService();
                String destMethod2 = targetService2.getMethod().getValue().getValue();

                int svcResult = CompareUtils.compareService(destNamespace1, destService1, destNamespace2, destService2);
                if (svcResult != 0) {
                    return svcResult;
                }
                return CompareUtils.compareSingleValue(destMethod1, destMethod2);
            }
        });
        return outRules;
    }
}
