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

package com.tencent.polaris.discovery.example.consumer;

import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.rpc.GetAllInstancesRequest;
import com.tencent.polaris.api.rpc.GetHealthyInstancesRequest;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.discovery.example.utils.ExampleUtils;
import com.tencent.polaris.discovery.example.utils.ExampleUtils.InitResult;

public class GetInstancesExample {

    public static void main(String[] args) throws Exception {
        InitResult initResult = ExampleUtils.initConsumerConfiguration(args, false);
        String namespace = initResult.getNamespace();
        String service = initResult.getService();

        try (ConsumerAPI consumerAPI = ExampleUtils.createConsumerAPI(initResult.getConfig())) {
            System.out.println("namespace " + namespace + ", service " + service);

            // Get all instance
            GetAllInstancesRequest allInstancesRequest = new GetAllInstancesRequest();
            allInstancesRequest.setNamespace(namespace);
            allInstancesRequest.setService(service);
            InstancesResponse instancesResponse = consumerAPI.getAllInstance(allInstancesRequest);
            System.out.println("all instances count is " + instancesResponse.getInstances().length);

            // Get healthy instance
            GetHealthyInstancesRequest getHealthyInstancesRequest = new GetHealthyInstancesRequest();
            getHealthyInstancesRequest.setNamespace(namespace);
            getHealthyInstancesRequest.setService(service);
            InstancesResponse healthyInstancesResponse = consumerAPI.getHealthyInstances(getHealthyInstancesRequest);
            System.out.println("healthy instances count is " + healthyInstancesResponse.getInstances().length);
        }
    }
}
