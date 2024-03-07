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

package com.tencent.polaris.discovery.test.core;

import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.core.ProviderAPI;
import com.tencent.polaris.api.plugin.lossless.InstanceProperties;
import com.tencent.polaris.api.plugin.lossless.LosslessActionProvider;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.InstanceRegisterRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterResponse;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.client.pojo.Node;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class DemoLosslessActionProvider implements LosslessActionProvider {

    private static final int MAX_CHECK_TIME = 3;

    private final ProviderAPI providerAPI;

    private final ServiceKey serviceKey;

    private final Node node;

    private final boolean enableHealthCheck;

    private final AtomicInteger checkCounter = new AtomicInteger(0);

    public DemoLosslessActionProvider(SDKContext sdkContext, ServiceKey serviceKey, Node node, boolean enableHealthCheck) {
        this.providerAPI = DiscoveryAPIFactory.createProviderAPIByContext(sdkContext);
        this.serviceKey = serviceKey;
        this.node = node;
        this.enableHealthCheck = enableHealthCheck;
    }

    @Override
    public String getName() {
        return "demo";
    }

    @Override
    public void doRegister(InstanceProperties instanceProperties) {
        InstanceRegisterRequest instanceRegisterRequest = new InstanceRegisterRequest();
        instanceRegisterRequest.setNamespace(serviceKey.getNamespace());
        instanceRegisterRequest.setService(serviceKey.getService());
        instanceRegisterRequest.setHost(node.getHost());
        instanceRegisterRequest.setPort(node.getPort());
        instanceRegisterRequest.setProtocol("http");
        instanceRegisterRequest.setTtl(5);
        instanceRegisterRequest.setAutoHeartbeat(true);
        InstanceRegisterResponse instanceRegisterResponse = providerAPI.registerInstance(instanceRegisterRequest);
        System.out.println("register succeed, instanceId: " + instanceRegisterResponse.getInstanceId());
    }

    @Override
    public void doDeregister() {

    }

    @Override
    public boolean isEnableHealthCheck() {
        return enableHealthCheck;
    }

    @Override
    public boolean doHealthCheck() {
        if (!enableHealthCheck) {
            return false;
        }
        return checkCounter.incrementAndGet() >= MAX_CHECK_TIME;
    }
}
