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
import com.tencent.polaris.api.core.ProviderAPI;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.rpc.InstanceRegisterRequest;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.discovery.example.utils.ExampleUtils;
import com.tencent.polaris.discovery.example.utils.ExampleUtils.InitResult;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class ServiceCallResultReport {

    private static final int TOTAL_SERVICE = 20;

    private static final int PER_INSTANCE_COUNT = 1000;

    public static void main(String[] args) throws Exception {
        InitResult initResult = ExampleUtils.initConsumerConfiguration(args, true);
        SDKContext context = ExampleUtils.createSDKContext(initResult.getConfig());
        ProviderAPI providerAPI = DiscoveryAPIFactory.createProviderAPIByContext(context);
        ConsumerAPI consumerAPI = DiscoveryAPIFactory.createConsumerAPIByContext(context);

//        mockService(providerAPI);

        TimeUnit.SECONDS.sleep(5);
        System.out.println("start mock report metrics");
        for (;;) {
            mockReportServiceCallResult(consumerAPI);
            TimeUnit.SECONDS.sleep(1);
        }
    }

//    private static void mockService(ProviderAPI providerAPI) throws Exception {
//        Random random = new Random();
//        String servicePrefix = "MOCK_SERVICE_";
//        for (int i = 0; i < TOTAL_SERVICE; i++) {
//            String service = servicePrefix + i;
//            String namespace = "MOCK_NAMESPACE_" + random.nextInt(3);
//            for (int j = 0; j < PER_INSTANCE_COUNT; j++) {
//                InstanceRegisterRequest request = new InstanceRegisterRequest();
//                request.setNamespace(namespace);
//                request.setService(service);
//                request.setHost((i + 1) + ".0.0." + (i + 1));
//                request.setPort(8000 + i);
//                providerAPI.registerInstance(request);
//            }
//        }
//    }

    private static void mockReportServiceCallResult(ConsumerAPI consumerAPI) {
        int[] errCodes = new int[]{200000, 200100, 300001, 404210, 403001, 429000, 500000, 500010};
        Map<String, String[]> methods = new HashMap<>();
        String servicePrefix = "MOCK_SERVICE_";
        RetStatus[] statuses = RetStatus.values();
        Random random = new Random();

        for (int i = 0; i < random.nextInt(TOTAL_SERVICE); i++) {
            String service = servicePrefix + i;
            String namespace = "MOCK_NAMESPACE_" + random.nextInt(3);
            methods.put(namespace + "." + service, new String[]{
                    "/api/v1/user/" + namespace + "/" + service,
                    "/api/v1/car/" + namespace + "/" + service,
                    "/api/v1/dog/" + namespace + "/" + service,
                    "/api/v1/cloud/" + namespace + "/" + service,
            });
            for (int j = 0; j < PER_INSTANCE_COUNT; j++) {
                String[] methodList = methods.get(namespace + "." + service);
                ServiceCallResult result = new ServiceCallResult();
                result.setNamespace(namespace);
                result.setService(service);
                result.setHost((i + 1) + ".0.0." + (i + 1));
                result.setPort(8000 + i);
                result.setMethod(methodList[Math.max(0, random.nextInt(methodList.length) - 1)]);
                result.setDelay(Math.max(3, random.nextInt(500)));
                result.setRetCode(errCodes[Math.max(0, random.nextInt(errCodes.length) - 1)]);
                result.setRetStatus(statuses[Math.max(0, random.nextInt(statuses.length) - 1)]);
                consumerAPI.updateServiceCallResult(result);
            }
        }
    }

}
