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

package com.tencent.polaris.plugins.loadbalancer.leastconnection;

import com.tencent.polaris.api.config.consumer.LoadBalanceConfig;
import com.tencent.polaris.api.control.Destroyable;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.loadbalance.LoadBalancer;
import com.tencent.polaris.api.plugin.registry.LocalRegistry;
import com.tencent.polaris.api.pojo.*;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.rpc.Criteria;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.client.flow.BaseFlow;
import com.tencent.polaris.client.flow.DefaultFlowControlParam;
import com.tencent.polaris.client.flow.ResourcesResponse;
import com.tencent.polaris.client.pojo.InstanceByProto;
import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;

import java.util.*;

/**
 * Least Connections Load Balancer
 *
 * @author Yuwei Fu
 */
public class LeastConnectionLoadBalance extends Destroyable implements LoadBalancer {

    private LocalRegistry localRegistry;

    private Extensions extensions;

    private static final Logger LOG = LoggerFactory.getLogger(LeastConnectionLoadBalance.class);

    @Override
    public Instance chooseInstance(Criteria criteria, ServiceInstances instances) throws PolarisException {
        if (instances == null || CollectionUtils.isEmpty(instances.getInstances())) {
            throw new PolarisException(ErrorCode.INSTANCE_NOT_FOUND, "request instances is empty");
        }
        ServiceKey serviceKey = instances.getServiceKey();
        List<Instance> requestInstanceList = instances.getInstances();
        Map<String, Instance> requestInstanceMap = new HashMap<>(requestInstanceList.size());
        for (Instance instance : requestInstanceList) {
            if (instance.getWeight() != 0) {
                requestInstanceMap.put(instance.getHost() + ":" + instance.getPort(), instance);
            }
        }
        ServiceEventKey serviceEventKey = new ServiceEventKey(serviceKey, EventType.INSTANCE);
        Set<ServiceEventKey> serviceEventKeySet = new HashSet<>();
        serviceEventKeySet.add(serviceEventKey);
        DefaultServiceEventKeysProvider svcKeysProvider = new DefaultServiceEventKeysProvider();
        svcKeysProvider.setSvcEventKeys(serviceEventKeySet);
        DefaultFlowControlParam engineFlowControlParam = new DefaultFlowControlParam();
        ResourcesResponse resourcesResponse = BaseFlow.syncGetResources(extensions, false, svcKeysProvider,
                engineFlowControlParam);
        ServiceInstances serviceInstances = resourcesResponse.getServiceInstances(serviceEventKey);

        List<Instance> localInstanceList = serviceInstances.getInstances();
        if (CollectionUtils.isEmpty(localInstanceList)) {
            throw new PolarisException(ErrorCode.INSTANCE_NOT_FOUND, "local instances is empty");
        }
        // intersection lookup
        List<Instance> instanceList = new ArrayList<>(localInstanceList.size());
        for (Instance instance : localInstanceList) {
            if (requestInstanceMap.containsKey(instance.getHost() + ":" + instance.getPort())) {
                instanceList.add(instance);
            }
        }
        if (instanceList.isEmpty()) {
            throw new PolarisException(ErrorCode.INSTANCE_NOT_FOUND,
                    "No instance found. serviceKey=" + serviceKey.toString());
        }

        int[] candidateIndexes = new int[instanceList.size()];
        int sameActiveCount = 0;
        long leastActive = Long.MAX_VALUE;
        long[] instanceActive = new long[instanceList.size()];
        for (int i = 0; i < instanceList.size(); i++) {
            InstanceByProto instance = (InstanceByProto) instanceList.get(i);
            long curActive = instance.getInstanceLocalValue().getInstanceStatistic().getActive();
            instanceActive[i] = curActive;
            if (curActive < leastActive) {
                leastActive = curActive;
                sameActiveCount = 0;
                candidateIndexes[sameActiveCount++] = i;
            } else if (curActive == leastActive) {
                candidateIndexes[sameActiveCount++] = i;
            }
        }
        // If there is only one instance with the same least active count,
        // return it directly and increase its active count.
        if (sameActiveCount == 1) {
            InstanceByProto targetInstance = (InstanceByProto) instanceList.get(candidateIndexes[0]);
            targetInstance.getInstanceLocalValue().getInstanceStatistic().incrementAndGetActive();
            LOG.debug("[LeastConnectionLoadBalance] instances active count: {}, choose instance: {}:{}", instanceActive,
                    targetInstance.getHost(), targetInstance.getPort());
            return requestInstanceMap.get(targetInstance.getHost() + ":" + targetInstance.getPort());
        }
        // If there are multiple instances with the same active connections,
        // randomly select one by weight and increase its active count.
        int[] candidatesWeights = new int[sameActiveCount];
        int totalWeight = 0;
        for (int i = 0; i < sameActiveCount; i++) {
            candidatesWeights[i] = (instanceList.get(candidateIndexes[i])).getWeight();
            totalWeight += candidatesWeights[i];
        }
        int randomWeight = (int) (Math.random() * totalWeight);
        for (int i = 0; i < sameActiveCount; i++) {
            randomWeight -= candidatesWeights[i];
            if (randomWeight < 0) {
                InstanceByProto targetInstance = (InstanceByProto) instanceList.get(candidateIndexes[i]);
                targetInstance.getInstanceLocalValue().getInstanceStatistic().incrementAndGetActive();
                LOG.debug("[LeastConnectionLoadBalance] instances active count: {}, choose instance: {}:{}",
                        instanceActive, targetInstance.getHost(), targetInstance.getPort());
                return requestInstanceMap.get(targetInstance.getHost() + ":" + targetInstance.getPort());
            }
        }
        throw new PolarisException(ErrorCode.INSTANCE_NOT_FOUND,
                "No instance selected. serviceKey=" + serviceKey.toString());
    }


    @Override
    public String getName() {
        return LoadBalanceConfig.LOAD_BALANCE_LEAST_CONNECTION;
    }

    @Override
    public PluginType getType() {
        return PluginTypes.LOAD_BALANCER.getBaseType();
    }

    @Override
    public void init(InitContext ctx) {
    }

    @Override
    public void postContextInit(Extensions extensions) throws PolarisException {
        this.extensions = extensions;
        localRegistry = extensions.getLocalRegistry();
    }

}
