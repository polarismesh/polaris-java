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

package com.tencent.polaris.plugins.loadbalancer.roundrobin;

import com.tencent.polaris.api.config.consumer.LoadBalanceConfig;
import com.tencent.polaris.api.control.Destroyable;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 权重轮训负载均衡
 *
 * @author veteranchen
 * @date 2023/7/13
 */
public class WeightedRoundRobinBalance extends Destroyable implements LoadBalancer {

    private static final int RECYCLE_PERIOD = 60000; // 1分钟

    private final ConcurrentMap<String, ConcurrentMap<String, WeightedRoundRobin>> methodWeightMap = new ConcurrentHashMap<>();

    protected static class WeightedRoundRobin {
        private int weight;
        private final AtomicLong current = new AtomicLong(0);
        private long lastUpdate;

        public int getWeight() {
            return weight;
        }

        public void setWeight(int weight) {
            this.weight = weight;
            current.set(0);
        }

        public long increaseCurrent() {
            return current.addAndGet(weight);
        }

        public void sel(int total) {
            current.addAndGet(-1 * total);
        }

        public long getLastUpdate() {
            return lastUpdate;
        }

        public void setLastUpdate(long lastUpdate) {
            this.lastUpdate = lastUpdate;
        }
    }

    @Override
    public Instance chooseInstance(Criteria criteria, ServiceInstances svcInstances) throws PolarisException {
        if (svcInstances == null) {
            throw new PolarisException(ErrorCode.INSTANCE_NOT_FOUND,
                    "instances is null");
        }
        if (CollectionUtils.isEmpty(svcInstances.getInstances())) {
            throw new PolarisException(ErrorCode.INSTANCE_NOT_FOUND,
                    "no instance found, serviceKey: " + svcInstances.getService());
        }
        String key = generateRouteKey(svcInstances.getInstances());
        ConcurrentMap<String, WeightedRoundRobin> map = methodWeightMap.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
        int totalWeight = 0;
        long maxCurrent = Long.MIN_VALUE;
        long now = System.currentTimeMillis();
        Instance selectedInstance = null;
        WeightedRoundRobin selectedWRR = null;
        for (Instance instance : svcInstances.getInstances()) {
            String identifyString = instance.getId();
            int weight = getWeight(criteria.getDynamicWeight(), instance);
            WeightedRoundRobin weightedRoundRobin = map.computeIfAbsent(identifyString, k -> {
                WeightedRoundRobin wrr = new WeightedRoundRobin();
                wrr.setWeight(weight);
                return wrr;
            });

            if (weight != weightedRoundRobin.getWeight()) {
                weightedRoundRobin.setWeight(weight);
            }
            long cur = weightedRoundRobin.increaseCurrent();
            weightedRoundRobin.setLastUpdate(now);
            if (cur > maxCurrent) {
                maxCurrent = cur;
                selectedInstance = instance;
                selectedWRR = weightedRoundRobin;
            }
            totalWeight += weight;
        }
        // 如果实例有变化，超过1分钟后淘汰出缓存中
        if (svcInstances.getInstances().size() != map.size()) {
            map.entrySet().removeIf(item -> now - item.getValue().getLastUpdate() > RECYCLE_PERIOD);
        }
        if (selectedInstance != null) {
            selectedWRR.sel(totalWeight);
            return selectedInstance;
        }
        return svcInstances.getInstances().get(0);
    }

    /**
     * 根据实例列表生成路由键
     * 相同实例列表（不同顺序）将生成相同的路由键
     *
     * @param instances 实例列表
     * @return 路由键
     */
    private String generateRouteKey(List<Instance> instances) {
        if (CollectionUtils.isEmpty(instances)) {
            return "empty";
        }
        
        // 提取实例标识
        List<String> identifiers = new ArrayList<>();
        for (Instance instance : instances) {
            String identifier = instance.getId();
            if (identifier == null || identifier.isEmpty()) {
                // 如果 instanceId 为空，则使用 host:port 组合
                identifier = instance.getHost() + ":" + instance.getPort();
            }
            identifiers.add(identifier);
        }
        
        // 字典排序
        Collections.sort(identifiers);
        
        // 使用 # 分隔符拼接
        String joined = String.join("#", identifiers);
        
        // 计算 hashCode 作为路由键
        return String.valueOf(joined.hashCode());
    }

    private int getWeight(Map<String, InstanceWeight> dynamicWeights, Instance instance) {
        if (CollectionUtils.isEmpty(dynamicWeights)) {
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
        return LoadBalanceConfig.LOAD_BALANCE_WEIGHTED_ROUND_ROBIN;
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