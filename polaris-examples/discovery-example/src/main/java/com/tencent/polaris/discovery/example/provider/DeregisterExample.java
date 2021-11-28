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

package com.tencent.polaris.discovery.example.provider;

import com.tencent.polaris.api.core.ProviderAPI;
import com.tencent.polaris.api.rpc.InstanceDeregisterRequest;
import com.tencent.polaris.discovery.example.utils.ExampleUtils;
import com.tencent.polaris.discovery.example.utils.ExampleUtils.InitResult;

public class DeregisterExample {

    public static void main(String[] args) throws Exception {
        InitResult initResult = ExampleUtils.initProviderConfiguration(args);
        String namespace = initResult.getNamespace();
        String service = initResult.getService();
        String host = initResult.getHost();
        int port = initResult.getPort();
        String token = initResult.getToken();
        try (ProviderAPI providerAPI = ExampleUtils.createProviderAPI(initResult.getConfig())) {
            InstanceDeregisterRequest instanceDeregisterRequest = new InstanceDeregisterRequest();
            instanceDeregisterRequest.setNamespace(namespace);
            instanceDeregisterRequest.setService(service);
            instanceDeregisterRequest.setHost(host);
            instanceDeregisterRequest.setPort(port);
            instanceDeregisterRequest.setToken(token);
            providerAPI.deRegister(instanceDeregisterRequest);
            System.out.println("deregister for service successfully: " + service);

        }
    }
}
