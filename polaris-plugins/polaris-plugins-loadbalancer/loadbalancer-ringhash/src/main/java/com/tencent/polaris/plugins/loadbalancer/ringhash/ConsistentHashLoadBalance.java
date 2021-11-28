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

package com.tencent.polaris.plugins.loadbalancer.ringhash;

import com.tencent.polaris.api.config.consumer.LoadBalanceConfig;
import com.tencent.polaris.api.control.Destroyable;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.IdAwarePlugin;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.cache.FlowCache;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.loadbalance.LoadBalancer;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.rpc.Criteria;
import com.tencent.polaris.api.utils.CollectionUtils;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

public class ConsistentHashLoadBalance extends Destroyable implements LoadBalancer, IdAwarePlugin {

    /**
     * 虚拟环倍数
     */
    private static final int VIRTUAL_NODE_SIZE = 5;
    private static final String VIRTUAL_NODE_SUFFIX = "&&";

    private int id;

    /**
     * 计算字符串hash的策略
     */
    private HashStrategy hashStrategy;

    private FlowCache flowCache;

    @Override
    public Instance chooseInstance(Criteria criteria, ServiceInstances instances) throws PolarisException {
        if (instances == null || CollectionUtils.isEmpty(instances.getInstances())) {
            return null;
        }
        TreeMap<Integer, Instance> ring = flowCache
                .loadPluginCacheObject(getId(), instances, new Function<Object, TreeMap<Integer, Instance>>() {
                    @Override
                    public TreeMap<Integer, Instance> apply(Object obj) {
                        return buildConsistentHashRing((ServiceInstances) obj);
                    }
                });
        int invocationHashCode = hashStrategy.getHashCode(criteria.getHashKey());
        return lookup(ring, invocationHashCode);
    }

    @Override
    public String getName() {
        return LoadBalanceConfig.LOAD_BALANCE_RING_HASH;
    }

    @Override
    public PluginType getType() {
        return PluginTypes.LOAD_BALANCER.getBaseType();
    }

    @Override
    public void init(InitContext ctx) throws PolarisException {
        hashStrategy = new MurmurHash();
    }

    @Override
    public void postContextInit(Extensions ctx) throws PolarisException {
        this.flowCache = ctx.getFlowCache();
    }

    /**
     * 从环中获取一个实例
     *
     * @param ring 环
     * @param invocationHashCode hashcode
     * @return 实例
     */
    private Instance lookup(TreeMap<Integer, Instance> ring, int invocationHashCode) {
        // 向右找到第一个 key
        Map.Entry<Integer, Instance> locateEntry = ring.ceilingEntry(invocationHashCode);
        if (locateEntry == null) {
            // 超过尾部则取第一个 key
            locateEntry = ring.firstEntry();
        }
        return locateEntry.getValue();
    }

    /**
     * 构建一致性hash环
     *
     * @param serviceInstances 服务实例
     * @return 一致性hash环
     */
    private TreeMap<Integer, Instance> buildConsistentHashRing(ServiceInstances serviceInstances) {
        List<Instance> instances = serviceInstances.getInstances();
        TreeMap<Integer, Instance> virtualNodeRing = new TreeMap<>();
        for (Instance instance : instances) {
            for (int i = 0; i < VIRTUAL_NODE_SIZE; i++) {
                // 新增虚拟节点
                virtualNodeRing.put(hashStrategy
                                .getHashCode(instance.getHost() + ":" + instance.getPort() + VIRTUAL_NODE_SUFFIX + i),
                        instance);
            }
        }
        return virtualNodeRing;
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }
}
