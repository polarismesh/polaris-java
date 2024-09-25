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

package com.tencent.polaris.plugins.connector.grpc.codec;

import com.tencent.polaris.api.plugin.cache.FlowCache;
import com.tencent.polaris.api.plugin.registry.AbstractCacheHandler;
import com.tencent.polaris.api.pojo.RegistryCacheValue;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.client.pojo.ServiceRuleByProto;
import com.tencent.polaris.specification.api.v1.fault.tolerance.FaultDetectorProto;
import com.tencent.polaris.specification.api.v1.fault.tolerance.FaultDetectorProto.FaultDetector;
import com.tencent.polaris.specification.api.v1.model.ModelProto;
import com.tencent.polaris.specification.api.v1.service.manage.ResponseProto.DiscoverResponse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FaultDetectCacheHandler extends AbstractCacheHandler {

    @Override
    protected String getRevision(DiscoverResponse discoverResponse) {
        FaultDetector faultDetector = discoverResponse.getFaultDetector();
        if (null == faultDetector) {
            return "";
        }
        return faultDetector.getRevision();
    }

    @Override
    public EventType getTargetEventType() {
        return EventType.FAULT_DETECTING;
    }

    @Override
    public RegistryCacheValue messageToCacheValue(RegistryCacheValue oldValue, Object newValue, boolean isCacheLoaded, FlowCache flowCache) {
        DiscoverResponse discoverResponse = (DiscoverResponse) newValue;
        FaultDetector faultDetector = discoverResponse.getFaultDetector();
        String revision = "";
        if (null != faultDetector) {
            revision = faultDetector.getRevision();
        }

        FaultDetector.Builder newFaultDetectorBuilder = FaultDetector.newBuilder().mergeFrom(faultDetector);
        List<FaultDetectorProto.FaultDetectRule> faultDetectRuleList = faultDetector.getRulesList();
        if (CollectionUtils.isNotEmpty(faultDetectRuleList)) {
            List<FaultDetectorProto.FaultDetectRule> newFaultDetectRuleList = new ArrayList<>();
            for (FaultDetectorProto.FaultDetectRule rule : faultDetectRuleList) {
                // 兼容老版本服务端
                if (rule.hasTargetService() && rule.getTargetService().hasMethod()) {
                    FaultDetectorProto.FaultDetectRule.DestinationService.Builder newDestBuilder
                            = FaultDetectorProto.FaultDetectRule.DestinationService.newBuilder().mergeFrom(rule.getTargetService());
                    newDestBuilder.setApi(ModelProto.API.newBuilder().setPath(rule.getTargetService().getMethod()).build());
                    FaultDetectorProto.FaultDetectRule.Builder newFaultDetectRuleBuilder
                            = FaultDetectorProto.FaultDetectRule.newBuilder().mergeFrom(rule);
                    newFaultDetectRuleBuilder.setTargetService(newDestBuilder.build());
                    newFaultDetectRuleList.add(newFaultDetectRuleBuilder.build());
                } else {
                    newFaultDetectRuleList.add(rule);
                }
            }

            // 排序
            newFaultDetectRuleList.sort(Comparator.comparingInt(FaultDetectorProto.FaultDetectRule::getPriority));

            newFaultDetectorBuilder.clearRules();
            newFaultDetectorBuilder.addAllRules(newFaultDetectRuleList);
            faultDetector = newFaultDetectorBuilder.build();
        }

        return new ServiceRuleByProto(faultDetector, revision, isCacheLoaded, getTargetEventType());
    }
}
