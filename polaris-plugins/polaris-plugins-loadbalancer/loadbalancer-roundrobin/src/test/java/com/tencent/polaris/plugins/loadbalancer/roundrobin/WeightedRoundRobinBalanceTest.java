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

package com.tencent.polaris.plugins.loadbalancer.roundrobin;

import com.tencent.polaris.api.plugin.loadbalance.LoadBalancer;
import com.tencent.polaris.api.pojo.*;
import com.tencent.polaris.api.rpc.Criteria;
import com.tencent.polaris.logging.LoggerFactory;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 权重轮训负载均衡
 *
 * @author veteranchen
 * @date 2023/7/13
 */
public class WeightedRoundRobinBalanceTest {
    private static final Logger LOG = LoggerFactory.getLogger(WeightedRoundRobinBalanceTest.class);

    private final LoadBalancer weightedRoundRobinBalancer = new WeightedRoundRobinBalance();

    private ServiceInstances buildServiceInstances() {
        List<Instance> instanceList = new ArrayList<>();
        instanceList.add(buildDefaultInstance("Development", "trpc.app.server.service", "127.0.0.1", 8000, 7));
        instanceList.add(buildDefaultInstance("Development", "trpc.app.server.service", "127.0.0.2", 8000, 2));
        instanceList.add(buildDefaultInstance("Development", "trpc.app.server.service", "127.0.0.3", 8000, 1));

        ServiceInstances instances = new DefaultServiceInstances(new ServiceKey("Development", "trpc.app.server.service"), instanceList);
        return instances;
    }

    private DefaultInstance buildDefaultInstance(String ns, String service, String host, int port, int weight) {
        DefaultInstance instance = new DefaultInstance();
        instance.setHost(host);
        instance.setPort(port);
        instance.setWeight(weight);
        instance.setNamespace(ns);
        instance.setService(service);
        instance.setId(host + ":" + port);
        return instance;
    }

    @Test
    public void testChooseInstance() {
        Instance ins = weightedRoundRobinBalancer.chooseInstance(new Criteria(), buildServiceInstances());
        LOG.info(ins.toString());
        Assert.assertNotNull(ins);

        Map<String, AtomicLong> insCounts = new HashMap<>(8);
        for (int i = 0; i < 100; i++) {
            ins = weightedRoundRobinBalancer.chooseInstance(new Criteria(), buildServiceInstances());
            insCounts.computeIfAbsent(ins.getId(), k -> new AtomicLong(0)).addAndGet(1);
        }
        Assert.assertEquals(70, insCounts.get("127.0.0.1:8000").longValue());
        Assert.assertEquals(20, insCounts.get("127.0.0.2:8000").longValue());
        Assert.assertEquals(10, insCounts.get("127.0.0.3:8000").longValue());
    }

}
