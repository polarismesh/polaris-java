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

package com.tencent.polaris.discovery.example.provider;

import com.tencent.polaris.api.core.ProviderAPI;
import com.tencent.polaris.api.rpc.InstanceRegisterRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterResponse;
import com.tencent.polaris.discovery.example.utils.ExampleUtils;
import com.tencent.polaris.discovery.example.utils.ExampleUtils.InitResult;

public class RegisterExample {

    public static void main(String[] args) throws Exception {
        InitResult initResult = ExampleUtils.initProviderConfiguration(args);
        String namespace = initResult.getNamespace();
        String service = initResult.getService();
        String host = initResult.getHost();
        int port = initResult.getPort();
        String token = initResult.getToken();
        try (ProviderAPI providerAPI = ExampleUtils.createProviderAPI(initResult.getConfig())) {
            InstanceRegisterRequest instanceRegisterRequest = new InstanceRegisterRequest();
            instanceRegisterRequest.setNamespace(namespace);
            instanceRegisterRequest.setService(service);
            instanceRegisterRequest.setHost(host);
            instanceRegisterRequest.setPort(port);
            instanceRegisterRequest.setToken(token);
            if (initResult.getTtl() > 0) {
                instanceRegisterRequest.setTtl(initResult.getTtl());
            }
            InstanceRegisterResponse instanceRegisterResponse = providerAPI.register(instanceRegisterRequest);
            System.out.println("response after register is " + instanceRegisterResponse);
        }
    }
}
