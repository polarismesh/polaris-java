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

package com.tencent.polaris.router.example;

import com.tencent.polaris.api.config.consumer.LoadBalanceConfig;
import com.tencent.polaris.api.config.consumer.ServiceRouterConfig;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.rpc.GetAllInstancesRequest;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.router.example.ExampleUtils.InitResult;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import com.tencent.polaris.factory.api.RouterAPIFactory;
import com.tencent.polaris.router.api.core.RouterAPI;
import com.tencent.polaris.router.api.rpc.ProcessLoadBalanceRequest;
import com.tencent.polaris.router.api.rpc.ProcessLoadBalanceResponse;
import com.tencent.polaris.router.api.rpc.ProcessRoutersRequest;
import com.tencent.polaris.router.api.rpc.ProcessRoutersRequest.RouterNamesGroup;
import com.tencent.polaris.router.api.rpc.ProcessRoutersResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 获取资源的样例
 */
public class RouterExample {

    public static void main(String[] args) throws Exception {
        InitResult initResult = ExampleUtils.initConsumerConfiguration(args);
        String namespace = initResult.getNamespace();
        String service = initResult.getService();
        try (SDKContext sdkContext = SDKContext.initContext()) {
            ConsumerAPI consumerAPI = DiscoveryAPIFactory.createConsumerAPIByContext(sdkContext);
            RouterAPI routerAPI = RouterAPIFactory.createRouterAPIByContext(sdkContext);
            //1. 拉取全量服务实例
            GetAllInstancesRequest getAllInstancesRequest = new GetAllInstancesRequest();
            getAllInstancesRequest.setNamespace(namespace);
            getAllInstancesRequest.setService(service);
            InstancesResponse allInstanceResp = consumerAPI.getAllInstance(getAllInstancesRequest);
            ServiceInstances dstInstances = allInstanceResp.toServiceInstances();
            //2. 执行服务路由
            ProcessRoutersRequest processRoutersRequest = new ProcessRoutersRequest();
            //被调服务
            System.out.printf("instances count before routing is %s%n", dstInstances.getInstances().size());
            //主调方信息
            ServiceInfo srcSourceInfo = new ServiceInfo();
            Map<String, String> labels = new HashMap<>();
            labels.put("env", "test");
            srcSourceInfo.setMetadata(labels);
            RouterNamesGroup routerNamesGroup = new RouterNamesGroup();
            List<String> coreRouters = new ArrayList<>();
            coreRouters.add(ServiceRouterConfig.DEFAULT_ROUTER_RULE);
            coreRouters.add(ServiceRouterConfig.DEFAULT_ROUTER_METADATA);
            coreRouters.add(ServiceRouterConfig.DEFAULT_ROUTER_NEARBY);
            //设置走规则路由
            routerNamesGroup.setCoreRouters(coreRouters);
            processRoutersRequest.setDstInstances(dstInstances);
            processRoutersRequest.setSourceService(srcSourceInfo);
            processRoutersRequest.setRouters(routerNamesGroup);
            ProcessRoutersResponse processRoutersResponse = routerAPI.processRouters(processRoutersRequest);
            System.out.printf("instances count after routing is %s%n",
                    processRoutersResponse.getServiceInstances().getInstances().size());

            //3. 执行负载均衡
            ProcessLoadBalanceRequest processLoadBalanceRequest = new ProcessLoadBalanceRequest();
            processLoadBalanceRequest.setDstInstances(processRoutersResponse.getServiceInstances());
            processLoadBalanceRequest.setLbPolicy(LoadBalanceConfig.LOAD_BALANCE_WEIGHTED_RANDOM);
            ProcessLoadBalanceResponse processLoadBalanceResponse = routerAPI
                    .processLoadBalance(processLoadBalanceRequest);
            System.out.printf("instances after lb is %s%n", processLoadBalanceResponse.getTargetInstance());
        }
    }
}
