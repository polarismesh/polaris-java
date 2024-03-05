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

import com.tencent.polaris.api.plugin.lossless.LosslessActionProvider;
import com.tencent.polaris.api.plugin.lossless.RegisterStatusProvider;
import com.tencent.polaris.api.plugin.server.ReportServiceContractRequest;
import com.tencent.polaris.api.plugin.server.ReportServiceContractResponse;
import com.tencent.polaris.api.rpc.*;
import com.tencent.polaris.client.flow.AbstractFlow;

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

    default InstancesResponse unWatchInstances(UnWatchInstancesRequest request) {
        return null;
    }

    default ServiceRuleResponse getServiceRule(GetServiceRuleRequest req) {
        return null;
    }

    default ServiceRuleResponse getServiceContract(GetServiceContractRequest req) {
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

    default ReportServiceContractResponse reportServiceContract(ReportServiceContractRequest req) {
        return null;
    }

    /**
     * 设置无损上下线相关的动作提供器, 不设置则使用默认的动态提供器（基于北极星SDK注册和反注册）
     * @param losslessActionProvider
     */
    default void setLosslessActionProvider(LosslessActionProvider losslessActionProvider) {}

    /**
     * 实施无损上下线
     */
    default void losslessRegister() {}

    /**
     * 设置实例上线状态的提供器，如不设置则使用默认的提供器，基本北极星SDK的注册和反注册来实现
     * @param registerStatusProvider
     */
    default void setRegisterStatusProvider(RegisterStatusProvider registerStatusProvider) {}

    void destroy();

}
