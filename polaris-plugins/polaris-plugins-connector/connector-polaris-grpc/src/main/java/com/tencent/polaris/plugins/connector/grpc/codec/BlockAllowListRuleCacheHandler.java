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

package com.tencent.polaris.plugins.connector.grpc.codec;

import com.tencent.polaris.api.plugin.cache.FlowCache;
import com.tencent.polaris.api.plugin.registry.AbstractCacheHandler;
import com.tencent.polaris.api.pojo.RegistryCacheValue;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.utils.CompareUtils;
import com.tencent.polaris.client.pojo.ServiceRuleByProto;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.specification.api.v1.security.BlockAllowListProto;
import com.tencent.polaris.specification.api.v1.service.manage.ResponseProto;
import com.tencent.polaris.specification.api.v1.service.manage.ServiceProto;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class BlockAllowListRuleCacheHandler extends AbstractCacheHandler {

    private static final Logger LOG = LoggerFactory.getLogger(BlockAllowListRuleCacheHandler.class);

    @Override
    protected String getRevision(ResponseProto.DiscoverResponse discoverResponse) {
        ServiceProto.Service service = discoverResponse.getService();
        return service.getRevision().getValue();
    }

    @Override
    public ServiceEventKey.EventType getTargetEventType() {
        return ServiceEventKey.EventType.BLOCK_ALLOW_RULE;
    }

    @Override
    public RegistryCacheValue messageToCacheValue(RegistryCacheValue oldValue, Object newValue, boolean isCacheLoaded, FlowCache flowCache) {
        ResponseProto.DiscoverResponse discoverResponse = (ResponseProto.DiscoverResponse) newValue;
        String revision = discoverResponse.getService().getRevision().getValue();

        // 排序规则
        ResponseProto.DiscoverResponse.Builder newDiscoverResponseBuilder = ResponseProto.DiscoverResponse.newBuilder()
                .mergeFrom(discoverResponse);
        List<BlockAllowListProto.BlockAllowListRule> unmodifiableList = discoverResponse.getBlockAllowListRuleList();
        List<BlockAllowListProto.BlockAllowListRule> blockAllowListRuleList = sortNearbyRouteRules(unmodifiableList);
        newDiscoverResponseBuilder.clearBlockAllowListRule();
        newDiscoverResponseBuilder.addAllBlockAllowListRule(blockAllowListRuleList);

        return new ServiceRuleByProto(newDiscoverResponseBuilder.build(), revision, isCacheLoaded, getTargetEventType());
    }

    private List<BlockAllowListProto.BlockAllowListRule> sortNearbyRouteRules(List<BlockAllowListProto.BlockAllowListRule> rules) {
        List<BlockAllowListProto.BlockAllowListRule> sorted = new ArrayList<>(rules);
        sorted.sort((o1, o2) -> {
            // 比较优先级，数字越小，规则优先级越大
            int priorityResult = o1.getPriority() - o2.getPriority();
            if (priorityResult != 0) {
                return priorityResult;
            }

            // 比较目标服务
            String destNamespace1 = o1.getNamespace();
            String destService1 = o1.getService();
            String destNamespace2 = o2.getNamespace();
            String destService2 = o2.getService();
            int serviceKeyResult = CompareUtils.compareService(destNamespace1, destService1, destNamespace2, destService2);
            if (serviceKeyResult != 0) {
                return serviceKeyResult;
            }

            // 比较规则ID
            String ruleId1 = o1.getId();
            String ruleId2 = o1.getId();
            return CompareUtils.compareSingleValue(ruleId1, ruleId2);
        });
        return sorted;
    }
}
