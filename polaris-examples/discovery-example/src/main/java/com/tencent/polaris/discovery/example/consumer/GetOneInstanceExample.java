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

package com.tencent.polaris.discovery.example.consumer;

import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.rpc.GetOneInstanceRequest;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.discovery.example.utils.ExampleUtils;
import com.tencent.polaris.discovery.example.utils.ExampleUtils.InitResult;

public class GetOneInstanceExample {

    public static void main(String[] args) throws Exception {
        InitResult initResult = ExampleUtils.initConsumerConfiguration(args, false);
        String namespace = initResult.getNamespace();
        String service = initResult.getService();
        try (ConsumerAPI consumerAPI = ExampleUtils.createConsumerAPI(initResult.getConfig())) {
            System.out.println("namespace " + namespace + ", service " + service);
            GetOneInstanceRequest getOneInstanceRequest = new GetOneInstanceRequest();
            getOneInstanceRequest.setNamespace(namespace);
            getOneInstanceRequest.setService(service);
            InstancesResponse oneInstance = consumerAPI.getOneInstance(getOneInstanceRequest);
            Instance[] instances = oneInstance.getInstances();
            System.out.println("instances count is " + instances.length);
            Instance targetInstance = instances[0];
            System.out.printf("target instance is %s:%d%n", targetInstance.getHost(), targetInstance.getPort());
        }
    }

}
