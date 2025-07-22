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

package com.tencent.polaris.plugins.connector.grpc.codec;

import com.tencent.polaris.api.plugin.cache.FlowCache;
import com.tencent.polaris.api.plugin.cache.FlowCacheUtils;
import com.tencent.polaris.api.plugin.registry.AbstractCacheHandler;
import com.tencent.polaris.api.pojo.RegistryCacheValue;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.client.pojo.ServiceRuleByProto;
import com.tencent.polaris.specification.api.v1.model.ModelProto;
import com.tencent.polaris.specification.api.v1.service.manage.ResponseProto.DiscoverResponse;
import com.tencent.polaris.specification.api.v1.service.manage.ServiceProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.FaultInjectionProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.RoutingProto;

import java.util.List;

public class FaultCacheHandler extends AbstractCacheHandler {

    @Override
    public EventType getTargetEventType() {
        return EventType.FAULT_INJECTION;
    }

    @Override
    public RegistryCacheValue messageToCacheValue(RegistryCacheValue oldValue, Object newValue, boolean isCacheLoaded, FlowCache flowCache) {
        DiscoverResponse discoverResponse = (DiscoverResponse) newValue;
        List<FaultInjectionProto.FaultInjection> faultInjectionList = discoverResponse.getFaultInjectionList();
        String revision = discoverResponse.getService().getRevision().getValue();
        if (CollectionUtils.isNotEmpty(faultInjectionList)) {
            // 缓存 inbounds 中的 api 树
            for (FaultInjectionProto.FaultInjection faultInjection : faultInjectionList) {
                List<RoutingProto.Source> sources = faultInjection.getSourcesList();
                for (RoutingProto.Source source : sources) {
                    if (source.containsMetadata("$path")) {
                        ModelProto.MatchString matchString = source.getMetadataOrDefault("$path", ModelProto.MatchString.getDefaultInstance());
                        FlowCacheUtils.saveApiTrie(matchString, flowCache);
                    }
                }
            }

        }
        return new ServiceRuleByProto(discoverResponse, revision, isCacheLoaded, getTargetEventType());
    }

    @Override
    protected String getRevision(DiscoverResponse discoverResponse) {
        ServiceProto.Service service = discoverResponse.getService();
        return service.getRevision().getValue();
    }
}
