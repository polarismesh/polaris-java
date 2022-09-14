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
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.client.pb.ResponseProto.DiscoverResponse;
import com.tencent.polaris.client.pb.RoutingProto.Routing;
import com.tencent.polaris.client.pojo.ServiceRuleByProto;

public class RoutingCacheHandler extends AbstractCacheHandler {

    @Override
    public EventType getTargetEventType() {
        return EventType.ROUTING;
    }

    @Override
    public RegistryCacheValue messageToCacheValue(RegistryCacheValue oldValue, Object newValue, boolean isCacheLoaded) {
        DiscoverResponse discoverResponse = (DiscoverResponse) newValue;
        Routing routing = discoverResponse.getRouting();
        String revision = "";
        if (null != routing) {
            revision = routing.getRevision().getValue();
        }
        return new ServiceRuleByProto(routing, revision, isCacheLoaded, getTargetEventType());
    }

    @Override
    protected String getRevision(DiscoverResponse discoverResponse) {
        Routing routing = discoverResponse.getRouting();
        if (null == routing) {
            return "";
        }
        return routing.getRevision().getValue();
    }
}
