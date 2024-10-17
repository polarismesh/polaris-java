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

package com.tencent.polaris.plugins.loadbalancer.random;

import com.tencent.polaris.api.config.consumer.LoadBalanceConfig;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.loadbalance.LoadBalancer;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.InstanceWeight;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.rpc.Criteria;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.control.Destroyable;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 权重随机复杂均衡
 *
 * @author andrewshan
 * @date 2019/8/24
 */
public class WeightedRandomBalance extends Destroyable implements LoadBalancer {

    private final Random random = new Random();

    @Override
    public Instance chooseInstance(Criteria criteria, ServiceInstances svcInstances) throws PolarisException {
        int totalWeight = svcInstances.getTotalWeight();
        if (totalWeight <= 0) {
            totalWeight = sumTotalWeight(criteria.getDynamicWeight(), svcInstances);
        }
        if (totalWeight == 0) {
            throw new PolarisException(ErrorCode.INSTANCE_NOT_FOUND,
                    String.format("all instances weight 0 for %s:%s", svcInstances.getNamespace(),
                            svcInstances.getService()));
        }
        //进行权重区间分配
        List<Instance> instances = svcInstances.getInstances();
        int randomValue = Math.abs(random.nextInt() % totalWeight);
        int start = 0;
        int end = 0;
        for (Instance instance : instances) {
            end = end + getWeight(criteria.getDynamicWeight(), instance);
            if (randomValue >= start && randomValue < end) {
                return instance;
            }
            start = end;
        }
        //全都分配不到，则随机获取一个
        return instances.get(totalWeight % instances.size());
    }

    private int sumTotalWeight(Map<String, InstanceWeight> dynamicWeights, ServiceInstances svcInstances) {
        List<Instance> instances = svcInstances.getInstances();
        int totalWeight = 0;
        if (CollectionUtils.isNotEmpty(instances)) {
            for (Instance instance : instances) {
                totalWeight += getWeight(dynamicWeights, instance);
            }
        }
        return totalWeight;
    }

    private int getWeight(Map<String, InstanceWeight> dynamicWeights, Instance instance) {
        if (CollectionUtils.isNotEmpty(dynamicWeights)) {
            return instance.getWeight();
        }

        if (dynamicWeights.containsKey(instance.getId())) {
            return dynamicWeights.get(instance.getId()).getDynamicWeight();
        } else {
            return instance.getWeight();
        }
    }

    @Override
    public String getName() {
        return LoadBalanceConfig.LOAD_BALANCE_WEIGHTED_RANDOM;
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

    }

}
