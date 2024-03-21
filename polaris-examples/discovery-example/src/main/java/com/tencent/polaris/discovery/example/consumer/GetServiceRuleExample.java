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
import com.tencent.polaris.api.listener.ServiceListener;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceChangeEvent;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.rpc.GetServiceRuleRequest;
import com.tencent.polaris.api.rpc.ServiceRuleResponse;
import com.tencent.polaris.api.rpc.WatchServiceRequest;
import com.tencent.polaris.api.rpc.WatchServiceResponse;
import com.tencent.polaris.discovery.example.utils.ExampleUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

/**
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class GetServiceRuleExample {

    public static void main(String[] args) throws Exception {
        ExampleUtils.InitResult initResult = ExampleUtils.initConsumerConfiguration(args, true);
        try (ConsumerAPI consumerAPI = ExampleUtils.createConsumerAPI(initResult.getConfig())) {
            GetServiceRuleRequest request = new GetServiceRuleRequest();
            request.setRuleType(ServiceEventKey.EventType.LANE_RULE);
            request.setService("service-a");
            request.setNamespace("default");
            ServiceRuleResponse response = consumerAPI.getServiceRule(request);
            System.out.println(response);
            new CountDownLatch(1).await();
        }
    }

}
