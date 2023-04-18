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

package com.tencent.polaris.api.flow;

import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.rpc.GetAllInstancesRequest;
import com.tencent.polaris.api.rpc.GetHealthyInstancesRequest;
import com.tencent.polaris.api.rpc.GetServiceRuleRequest;
import com.tencent.polaris.api.rpc.GetServicesRequest;
import com.tencent.polaris.api.rpc.InstanceDeregisterRequest;
import com.tencent.polaris.api.rpc.InstanceHeartbeatRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterResponse;
import com.tencent.polaris.api.rpc.InstancesFuture;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.api.rpc.ServiceRuleResponse;
import com.tencent.polaris.api.rpc.ServicesResponse;
import com.tencent.polaris.api.rpc.WatchInstancesRequest;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.flow.AbstractFlow;
import java.util.ServiceLoader;

public interface DiscoveryFlow extends AbstractFlow {

    default InstancesResponse getAllInstances(GetAllInstancesRequest request) {
        return null;
    }

    default InstancesFuture asyncGetAllInstances(GetAllInstancesRequest req) {
        return null;
    }

    default InstancesResponse getHealthyInstances(GetHealthyInstancesRequest request) {
        return null;
    }

    default InstancesResponse watchInstances(WatchInstancesRequest request) {
        return null;
    }

    default InstancesResponse unWatchInstances(WatchInstancesRequest request) {
        return null;
    }

    default ServiceRuleResponse getServiceRule(GetServiceRuleRequest req) {
        return null;
    }

    default ServicesResponse getServices(GetServicesRequest req) {
        return null;
    }

    default InstanceRegisterResponse register(InstanceRegisterRequest req) {
        return null;
    }

    default void deRegister(InstanceDeregisterRequest req) {

    }

    default void heartbeat(InstanceHeartbeatRequest req) {

    }

    static DiscoveryFlow loadDiscoveryFlow(String name) {
        ServiceLoader<DiscoveryFlow> discoveryFlows = ServiceLoader.load(DiscoveryFlow.class);
        for (DiscoveryFlow discoveryFlow : discoveryFlows) {
            if (StringUtils.equals(discoveryFlow.getName(), name)) {
                return discoveryFlow;
            }
        }
       throw new PolarisException(ErrorCode.INVALID_CONFIG,
               String.format("unknown flow name %s, type %s", name, DiscoveryFlow.class.getCanonicalName()));
    }
}
