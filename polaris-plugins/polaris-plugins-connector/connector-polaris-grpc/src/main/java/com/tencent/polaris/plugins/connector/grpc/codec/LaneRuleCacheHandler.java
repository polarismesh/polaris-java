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

import com.tencent.polaris.api.plugin.registry.AbstractCacheHandler;
import com.tencent.polaris.api.pojo.RegistryCacheValue;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.client.pojo.ServiceRuleByProto;
import com.tencent.polaris.specification.api.v1.service.manage.ResponseProto;
import com.tencent.polaris.specification.api.v1.service.manage.ServiceProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.LaneProto;

import java.util.List;

public class LaneRuleCacheHandler extends AbstractCacheHandler {
    @Override
    protected String getRevision(ResponseProto.DiscoverResponse discoverResponse) {
        ServiceProto.Service service = discoverResponse.getService();
        return service.getRevision().getValue();
    }

    @Override
    public ServiceEventKey.EventType getTargetEventType() {
        return ServiceEventKey.EventType.LANE_RULE;
    }

    @Override
    public RegistryCacheValue messageToCacheValue(RegistryCacheValue oldValue, Object newValue, boolean isCacheLoaded) {
        ResponseProto.DiscoverResponse discoverResponse = (ResponseProto.DiscoverResponse) newValue;
        List<LaneProto.LaneGroup> rules = discoverResponse.getLanesList();
        String revision = discoverResponse.getService().getRevision().getValue();
        return new ServiceRuleByProto(rules, revision, isCacheLoaded, getTargetEventType());
    }
}
