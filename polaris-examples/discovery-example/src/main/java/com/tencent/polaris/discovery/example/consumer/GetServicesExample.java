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

package com.tencent.polaris.discovery.example.consumer;

import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.api.rpc.GetServicesRequest;
import com.tencent.polaris.api.rpc.ServicesResponse;
import com.tencent.polaris.discovery.example.utils.ExampleUtils;
import com.tencent.polaris.discovery.example.utils.ExampleUtils.InitResult;
import java.util.List;

public class GetServicesExample {

    public static void main(String[] args) throws Exception {
        InitResult initResult = ExampleUtils.initConsumerConfiguration(args, true);
        String namespace = initResult.getNamespace();
        try (ConsumerAPI consumerAPI = ExampleUtils.createConsumerAPI(initResult.getConfig())) {
            System.out.println("namespace " + namespace);
            GetServicesRequest request = new GetServicesRequest();
            request.setNamespace(namespace);
            ServicesResponse response = consumerAPI.getServices(request);
            List<ServiceInfo> services = response.getServices();
            System.out.println("services count is " + services.size());
            System.out.println("print all service " + services);

            request = new GetServicesRequest();
            request.setNamespace(namespace);
            response = consumerAPI.getServices(request);
            services = response.getServices();
            System.out.println("services count is " + services.size());
            System.out.println("print all service " + services);
        }
    }

}
