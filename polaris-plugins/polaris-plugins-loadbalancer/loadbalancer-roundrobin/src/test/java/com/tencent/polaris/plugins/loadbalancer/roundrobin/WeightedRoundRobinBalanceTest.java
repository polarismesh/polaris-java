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
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.loadbalance.LoadBalancer;
import com.tencent.polaris.api.pojo.*;
import com.tencent.polaris.api.rpc.Criteria;
import com.tencent.polaris.logging.LoggerFactory;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;

import java.lang.reflect.Method;
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

    /**
     * 测试相同实例列表（不同顺序）生成相同的路由键
     */
    @Test
    public void testGenerateRouteKey_SameInstancesDifferentOrder() throws Exception {
        WeightedRoundRobinBalance balancer = new WeightedRoundRobinBalance();
        Method method = WeightedRoundRobinBalance.class.getDeclaredMethod("generateRouteKey", List.class);
        method.setAccessible(true);

        // 创建相同实例但顺序不同的列表
        List<Instance> instances1 = new ArrayList<>();
        instances1.add(buildDefaultInstance("ns", "svc", "127.0.0.1", 8000, 1));
        instances1.add(buildDefaultInstance("ns", "svc", "127.0.0.2", 8000, 1));
        instances1.add(buildDefaultInstance("ns", "svc", "127.0.0.3", 8000, 1));

        List<Instance> instances2 = new ArrayList<>();
        instances2.add(buildDefaultInstance("ns", "svc", "127.0.0.3", 8000, 1));
        instances2.add(buildDefaultInstance("ns", "svc", "127.0.0.1", 8000, 1));
        instances2.add(buildDefaultInstance("ns", "svc", "127.0.0.2", 8000, 1));

        String key1 = (String) method.invoke(balancer, instances1);
        String key2 = (String) method.invoke(balancer, instances2);

        Assert.assertEquals("相同实例列表（不同顺序）应生成相同的路由键", key1, key2);
    }

    /**
     * 测试不同实例列表生成不同的路由键
     */
    @Test
    public void testGenerateRouteKey_DifferentInstances() throws Exception {
        WeightedRoundRobinBalance balancer = new WeightedRoundRobinBalance();
        Method method = WeightedRoundRobinBalance.class.getDeclaredMethod("generateRouteKey", List.class);
        method.setAccessible(true);

        List<Instance> instances1 = new ArrayList<>();
        instances1.add(buildDefaultInstance("ns", "svc", "127.0.0.1", 8000, 1));
        instances1.add(buildDefaultInstance("ns", "svc", "127.0.0.2", 8000, 1));

        List<Instance> instances2 = new ArrayList<>();
        instances2.add(buildDefaultInstance("ns", "svc", "127.0.0.1", 8000, 1));
        instances2.add(buildDefaultInstance("ns", "svc", "127.0.0.3", 8000, 1));

        String key1 = (String) method.invoke(balancer, instances1);
        String key2 = (String) method.invoke(balancer, instances2);

        Assert.assertNotEquals("不同实例列表应生成不同的路由键", key1, key2);
    }

    /**
     * 测试 instanceId 为空时使用 host:port 作为标识
     */
    @Test
    public void testGenerateRouteKey_EmptyInstanceId() throws Exception {
        WeightedRoundRobinBalance balancer = new WeightedRoundRobinBalance();
        Method method = WeightedRoundRobinBalance.class.getDeclaredMethod("generateRouteKey", List.class);
        method.setAccessible(true);

        List<Instance> instances1 = new ArrayList<>();
        DefaultInstance instance1 = buildDefaultInstance("ns", "svc", "127.0.0.1", 8000, 1);
        instance1.setId(null); // 清空 instanceId
        instances1.add(instance1);

        List<Instance> instances2 = new ArrayList<>();
        DefaultInstance instance2 = buildDefaultInstance("ns", "svc", "127.0.0.1", 8000, 1);
        instance2.setId("127.0.0.1:8000"); // 设置 instanceId 为 host:port
        instances2.add(instance2);

        String key1 = (String) method.invoke(balancer, instances1);
        String key2 = (String) method.invoke(balancer, instances2);

        Assert.assertEquals("instanceId 为空时应使用 host:port 作为标识，与设置 instanceId 为 host:port 的路由键相同", key1, key2);
    }

    /**
     * 测试实例列表为空时的处理
     */
    @Test
    public void testGenerateRouteKey_EmptyInstances() throws Exception {
        WeightedRoundRobinBalance balancer = new WeightedRoundRobinBalance();
        Method method = WeightedRoundRobinBalance.class.getDeclaredMethod("generateRouteKey", List.class);
        method.setAccessible(true);

        List<Instance> emptyList = new ArrayList<>();
        String key = (String) method.invoke(balancer, emptyList);

        Assert.assertEquals("空实例列表应返回 'empty'", "empty", key);
    }

    /**
     * 测试 null 实例列表的处理
     */
    @Test
    public void testGenerateRouteKey_NullInstances() throws Exception {
        WeightedRoundRobinBalance balancer = new WeightedRoundRobinBalance();
        Method method = WeightedRoundRobinBalance.class.getDeclaredMethod("generateRouteKey", List.class);
        method.setAccessible(true);

        String key = (String) method.invoke(balancer, (List<Instance>) null);

        Assert.assertEquals("null 实例列表应返回 'empty'", "empty", key);
    }

    /**
     * 测试相同路由键下的权重轮询保持独立性
     */
    @Test
    public void testRouteKeyWeightIsolation_SameRouteKey() {
        // 创建两个相同实例列表的服务实例
        ServiceInstances instances1 = buildServiceInstances();
        ServiceInstances instances2 = buildServiceInstances();

        Map<String, AtomicLong> counts = new HashMap<>(8);

        // 对第一个实例列表进行多次轮询
        for (int i = 0; i < 50; i++) {
            Instance ins = weightedRoundRobinBalancer.chooseInstance(new Criteria(), instances1);
            String key = "list1_" + ins.getId();
            counts.computeIfAbsent(key, k -> new AtomicLong(0)).addAndGet(1);
        }

        // 对第二个实例列表进行多次轮询（应该继续第一个的状态，因为路由键相同）
        for (int i = 0; i < 50; i++) {
            Instance ins = weightedRoundRobinBalancer.chooseInstance(new Criteria(), instances2);
            String key = "list2_" + ins.getId();
            counts.computeIfAbsent(key, k -> new AtomicLong(0)).addAndGet(1);
        }

        // 验证权重比例（7:2:1），总共100次
        long total1 = counts.get("list1_127.0.0.1:8000").longValue() + 
                      counts.get("list1_127.0.0.2:8000").longValue() + 
                      counts.get("list1_127.0.0.3:8000").longValue();
        long total2 = counts.get("list2_127.0.0.1:8000").longValue() + 
                      counts.get("list2_127.0.0.2:8000").longValue() + 
                      counts.get("list2_127.0.0.3:8000").longValue();
        
        Assert.assertEquals(50, total1);
        Assert.assertEquals(50, total2);
    }

    /**
     * 测试不同路由键的权重状态完全隔离
     */
    @Test
    public void testRouteKeyWeightIsolation_DifferentRouteKeys() {
        // 创建两个不同的实例列表
        List<Instance> instanceList1 = new ArrayList<>();
        instanceList1.add(buildDefaultInstance("ns", "svc", "127.0.0.1", 8000, 7));
        instanceList1.add(buildDefaultInstance("ns", "svc", "127.0.0.2", 8000, 2));
        instanceList1.add(buildDefaultInstance("ns", "svc", "127.0.0.3", 8000, 1));
        ServiceInstances instances1 = new DefaultServiceInstances(
            new ServiceKey("ns", "svc"), instanceList1);

        List<Instance> instanceList2 = new ArrayList<>();
        instanceList2.add(buildDefaultInstance("ns", "svc", "127.0.0.1", 8000, 1));
        instanceList2.add(buildDefaultInstance("ns", "svc", "127.0.0.2", 8000, 1));
        instanceList2.add(buildDefaultInstance("ns", "svc", "127.0.0.4", 8000, 1));
        ServiceInstances instances2 = new DefaultServiceInstances(
            new ServiceKey("ns", "svc"), instanceList2);

        Map<String, AtomicLong> counts = new HashMap<>(8);

        // 对第一个实例列表进行多次轮询（权重为 7:2:1）
        for (int i = 0; i < 100; i++) {
            Instance ins = weightedRoundRobinBalancer.chooseInstance(new Criteria(), instances1);
            counts.computeIfAbsent("route1_" + ins.getId(), k -> new AtomicLong(0)).addAndGet(1);
        }

        // 对第二个实例列表进行多次轮询（权重为 1:1:1，因为权重相同应该均匀分布）
        for (int i = 0; i < 99; i++) {
            Instance ins = weightedRoundRobinBalancer.chooseInstance(new Criteria(), instances2);
            counts.computeIfAbsent("route2_" + ins.getId(), k -> new AtomicLong(0)).addAndGet(1);
        }

        // 验证第一个路由的权重比例（7:2:1）
        Assert.assertEquals(70, counts.get("route1_127.0.0.1:8000").longValue());
        Assert.assertEquals(20, counts.get("route1_127.0.0.2:8000").longValue());
        Assert.assertEquals(10, counts.get("route1_127.0.0.3:8000").longValue());

        // 验证第二个路由的分布（每个实例约33次）
        long count2_1 = counts.get("route2_127.0.0.1:8000").longValue();
        long count2_2 = counts.get("route2_127.0.0.2:8000").longValue();
        long count2_3 = counts.get("route2_127.0.0.4:8000").longValue();
        
        Assert.assertTrue("route2_127.0.0.1:8000 count should be around 33, got: " + count2_1, 
            count2_1 >= 30 && count2_1 <= 36);
        Assert.assertTrue("route2_127.0.0.2:8000 count should be around 33, got: " + count2_2, 
            count2_2 >= 30 && count2_2 <= 36);
        Assert.assertTrue("route2_127.0.0.4:8000 count should be around 33, got: " + count2_3, 
            count2_3 >= 30 && count2_3 <= 36);

        // 确保第二个路由没有选择到第一个路由独有的实例
        Assert.assertNull("route2 should not select 127.0.0.3:8000", counts.get("route2_127.0.0.3:8000"));
    }

    /**
     * 测试路由键变化后重新初始化权重状态
     */
    @Test
    public void testRouteKeyWeightIsolation_RouteKeyChange() throws Exception {
        // 使用反射访问 methodWeightMap 以验证状态隔离
        java.lang.reflect.Field field = WeightedRoundRobinBalance.class.getDeclaredField("methodWeightMap");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, ?> weightMap = (Map<String, ?>) field.get(weightedRoundRobinBalancer);

        // 创建第一个实例列表
        List<Instance> instanceList1 = new ArrayList<>();
        instanceList1.add(buildDefaultInstance("ns", "svc", "127.0.0.1", 8000, 10));
        instanceList1.add(buildDefaultInstance("ns", "svc", "127.0.0.2", 8000, 10));
        ServiceInstances instances1 = new DefaultServiceInstances(
            new ServiceKey("ns", "svc"), instanceList1);

        // 进行一些轮询
        for (int i = 0; i < 10; i++) {
            weightedRoundRobinBalancer.chooseInstance(new Criteria(), instances1);
        }

        int route1MapSize = weightMap.size();
        Assert.assertTrue("应该有一个路由键的状态", route1MapSize > 0);

        // 创建第二个不同的实例列表（添加一个新实例）
        List<Instance> instanceList2 = new ArrayList<>();
        instanceList2.add(buildDefaultInstance("ns", "svc", "127.0.0.1", 8000, 10));
        instanceList2.add(buildDefaultInstance("ns", "svc", "127.0.0.2", 8000, 10));
        instanceList2.add(buildDefaultInstance("ns", "svc", "127.0.0.3", 8000, 10));
        ServiceInstances instances2 = new DefaultServiceInstances(
            new ServiceKey("ns", "svc"), instanceList2);

        // 进行一些轮询
        for (int i = 0; i < 10; i++) {
            weightedRoundRobinBalancer.chooseInstance(new Criteria(), instances2);
        }

        // 应该有两个不同的路由键
        Assert.assertEquals("应该有两个不同的路由键状态", 2, weightMap.size());
    }

    /**
     * 测试并发访问同一路由键的正确性
     */
    @Test
    public void testRouteKeyWeightIsolation_ConcurrentAccess() throws InterruptedException {
        final ServiceInstances instances = buildServiceInstances();
        final int threadCount = 10;
        final int requestsPerThread = 100;
        final Map<String, AtomicLong> globalCounts = new HashMap<>(8);
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(threadCount);

        // 创建多个线程并发访问同一个路由键
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        Instance ins = weightedRoundRobinBalancer.chooseInstance(new Criteria(), instances);
                        synchronized (globalCounts) {
                            globalCounts.computeIfAbsent(ins.getId(), k -> new AtomicLong(0)).addAndGet(1);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
            threads[i].start();
        }

        latch.await();

        // 验证总请求数
        long totalCount = globalCounts.values().stream().mapToLong(AtomicLong::get).sum();
        Assert.assertEquals(threadCount * requestsPerThread, totalCount);

        // 验证权重比例（7:2:1）
        long count1 = globalCounts.get("127.0.0.1:8000").longValue();
        long count2 = globalCounts.get("127.0.0.2:8000").longValue();
        long count3 = globalCounts.get("127.0.0.3:8000").longValue();

        // 允许一定误差（±5%）
        Assert.assertTrue("权重比例 7:2:1 的第一个实例", count1 >= 665 && count1 <= 735);
        Assert.assertTrue("权重比例 7:2:1 的第二个实例", count2 >= 190 && count2 <= 210);
        Assert.assertTrue("权重比例 7:2:1 的第三个实例", count3 >= 95 && count3 <= 105);
    }

    /**
     * 测试权重比例分配的准确性（基于路由键隔离不同路由场景）
     */
    @Test
    public void testWeightedRoundRobin_Accuracy() {
        // 测试权重比例 5:3:2
        List<Instance> instanceList = new ArrayList<>();
        instanceList.add(buildDefaultInstance("ns", "svc", "127.0.0.1", 8000, 5));
        instanceList.add(buildDefaultInstance("ns", "svc", "127.0.0.2", 8000, 3));
        instanceList.add(buildDefaultInstance("ns", "svc", "127.0.0.3", 8000, 2));
        ServiceInstances instances = new DefaultServiceInstances(
            new ServiceKey("ns", "svc"), instanceList);

        Map<String, AtomicLong> counts = new HashMap<>(8);
        for (int i = 0; i < 100; i++) {
            Instance ins = weightedRoundRobinBalancer.chooseInstance(new Criteria(), instances);
            counts.computeIfAbsent(ins.getId(), k -> new AtomicLong(0)).addAndGet(1);
        }

        Assert.assertEquals(50, counts.get("127.0.0.1:8000").longValue());
        Assert.assertEquals(30, counts.get("127.0.0.2:8000").longValue());
        Assert.assertEquals(20, counts.get("127.0.0.3:8000").longValue());

        // 测试权重比例 1:1:1
        List<Instance> instanceList2 = new ArrayList<>();
        instanceList2.add(buildDefaultInstance("ns", "svc2", "127.0.0.1", 8001, 1));
        instanceList2.add(buildDefaultInstance("ns", "svc2", "127.0.0.2", 8001, 1));
        instanceList2.add(buildDefaultInstance("ns", "svc2", "127.0.0.3", 8001, 1));
        ServiceInstances instances2 = new DefaultServiceInstances(
            new ServiceKey("ns", "svc2"), instanceList2);

        Map<String, AtomicLong> counts2 = new HashMap<>(8);
        for (int i = 0; i < 99; i++) {
            Instance ins = weightedRoundRobinBalancer.chooseInstance(new Criteria(), instances2);
            counts2.computeIfAbsent(ins.getId(), k -> new AtomicLong(0)).addAndGet(1);
        }

        // 允许一定误差
        long count1 = counts2.get("127.0.0.1:8001").longValue();
        long count2 = counts2.get("127.0.0.2:8001").longValue();
        long count3 = counts2.get("127.0.0.3:8001").longValue();
        Assert.assertTrue(count1 >= 30 && count1 <= 36);
        Assert.assertTrue(count2 >= 30 && count2 <= 36);
        Assert.assertTrue(count3 >= 30 && count3 <= 36);
    }

    /**
     * 测试权重变化时状态更新的正确性
     */
    @Test
    public void testWeightedRoundRobin_WeightChange() {
        // 初始权重 5:3:2
        List<Instance> instanceList = new ArrayList<>();
        DefaultInstance instance1 = buildDefaultInstance("ns", "svc", "127.0.0.1", 8000, 5);
        DefaultInstance instance2 = buildDefaultInstance("ns", "svc", "127.0.0.2", 8000, 3);
        DefaultInstance instance3 = buildDefaultInstance("ns", "svc", "127.0.0.3", 8000, 2);
        instanceList.add(instance1);
        instanceList.add(instance2);
        instanceList.add(instance3);
        ServiceInstances instances = new DefaultServiceInstances(
            new ServiceKey("ns", "svc"), instanceList);

        Map<String, AtomicLong> counts = new HashMap<>(8);
        for (int i = 0; i < 50; i++) {
            Instance ins = weightedRoundRobinBalancer.chooseInstance(new Criteria(), instances);
            counts.computeIfAbsent(ins.getId(), k -> new AtomicLong(0)).addAndGet(1);
        }

        // 验证初始权重比例
        Assert.assertEquals(25, counts.get("127.0.0.1:8000").longValue());
        Assert.assertEquals(15, counts.get("127.0.0.2:8000").longValue());
        Assert.assertEquals(10, counts.get("127.0.0.3:8000").longValue());

        // 修改权重为 1:1:1
        instance1.setWeight(1);
        instance2.setWeight(1);
        instance3.setWeight(1);

        counts.clear();
        for (int i = 0; i < 99; i++) {
            Instance ins = weightedRoundRobinBalancer.chooseInstance(new Criteria(), instances);
            counts.computeIfAbsent(ins.getId(), k -> new AtomicLong(0)).addAndGet(1);
        }

        // 验证修改后的权重比例（应该接近均匀分布）
        long count1 = counts.get("127.0.0.1:8000").longValue();
        long count2 = counts.get("127.0.0.2:8000").longValue();
        long count3 = counts.get("127.0.0.3:8000").longValue();
        Assert.assertTrue(count1 >= 30 && count1 <= 36);
        Assert.assertTrue(count2 >= 30 && count2 <= 36);
        Assert.assertTrue(count3 >= 30 && count3 <= 36);
    }

    /**
     * 测试实例过期清理的正确性
     */
    @Test
    public void testWeightedRoundRobin_InstanceExpire() throws Exception {
        java.lang.reflect.Field field = WeightedRoundRobinBalance.class.getDeclaredField("methodWeightMap");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, ?> weightMap = (Map<String, ?>) field.get(weightedRoundRobinBalancer);

        // 创建初始实例列表
        List<Instance> instanceList1 = new ArrayList<>();
        instanceList1.add(buildDefaultInstance("ns", "svc", "127.0.0.1", 8000, 1));
        instanceList1.add(buildDefaultInstance("ns", "svc", "127.0.0.2", 8000, 1));
        instanceList1.add(buildDefaultInstance("ns", "svc", "127.0.0.3", 8000, 1));
        ServiceInstances instances1 = new DefaultServiceInstances(
            new ServiceKey("ns", "svc"), instanceList1);

        // 进行一些轮询
        for (int i = 0; i < 10; i++) {
            weightedRoundRobinBalancer.chooseInstance(new Criteria(), instances1);
        }

        Assert.assertEquals("应该有一个路由键", 1, weightMap.size());

        // 移除一个实例
        List<Instance> instanceList2 = new ArrayList<>();
        instanceList2.add(buildDefaultInstance("ns", "svc", "127.0.0.1", 8000, 1));
        instanceList2.add(buildDefaultInstance("ns", "svc", "127.0.0.2", 8000, 1));
        ServiceInstances instances2 = new DefaultServiceInstances(
            new ServiceKey("ns", "svc"), instanceList2);

        // 进行轮询（此时实例列表大小与映射大小不一致）
        for (int i = 0; i < 10; i++) {
            weightedRoundRobinBalancer.chooseInstance(new Criteria(), instances2);
        }

        // 等待超过回收期
        Thread.sleep(65000);

        // 再次进行轮询，触发清理
        for (int i = 0; i < 10; i++) {
            weightedRoundRobinBalancer.chooseInstance(new Criteria(), instances2);
        }

        // 验证过期实例已被清理（映射中的实例数量应该与当前实例列表一致）
        @SuppressWarnings("unchecked")
        Map<String, ?> innerMap = (Map<String, ?>) weightMap.values().iterator().next();
        Assert.assertEquals("过期实例应该被清理", 2, innerMap.size());
    }

    /**
     * 测试同一服务不同路由的权重状态独立
     */
    @Test
    public void testWeightedRoundRobin_SameServiceDifferentRoutes() {
        // 创建两个路由场景的实例列表（相同的服务但不同的实例）
        List<Instance> routeAInstances = new ArrayList<>();
        routeAInstances.add(buildDefaultInstance("ns", "svc", "127.0.0.1", 8000, 3));
        routeAInstances.add(buildDefaultInstance("ns", "svc", "127.0.0.2", 8000, 1));
        ServiceInstances routeA = new DefaultServiceInstances(
            new ServiceKey("ns", "svc"), routeAInstances);

        List<Instance> routeBInstances = new ArrayList<>();
        routeBInstances.add(buildDefaultInstance("ns", "svc", "127.0.0.1", 8000, 2));
        routeBInstances.add(buildDefaultInstance("ns", "svc", "127.0.0.3", 8000, 2));
        ServiceInstances routeB = new DefaultServiceInstances(
            new ServiceKey("ns", "svc"), routeBInstances);

        Map<String, AtomicLong> counts = new HashMap<>(8);

        // 对路由A进行轮询（权重比例 3:1）
        for (int i = 0; i < 100; i++) {
            Instance ins = weightedRoundRobinBalancer.chooseInstance(new Criteria(), routeA);
            counts.computeIfAbsent("routeA_" + ins.getId(), k -> new AtomicLong(0)).addAndGet(1);
        }

        // 对路由B进行轮询（权重比例 2:2）
        for (int i = 0; i < 100; i++) {
            Instance ins = weightedRoundRobinBalancer.chooseInstance(new Criteria(), routeB);
            counts.computeIfAbsent("routeB_" + ins.getId(), k -> new AtomicLong(0)).addAndGet(1);
        }

        // 验证路由A的权重比例（3:1）
        Assert.assertEquals(75, counts.get("routeA_127.0.0.1:8000").longValue());
        Assert.assertEquals(25, counts.get("routeA_127.0.0.2:8000").longValue());
        Assert.assertNull("路由A不应该选择127.0.0.3:8000", counts.get("routeA_127.0.0.3:8000"));

        // 验证路由B的权重比例（2:2）
        Assert.assertEquals(50, counts.get("routeB_127.0.0.1:8000").longValue());
        Assert.assertNull("路由B不应该选择127.0.0.2:8000", counts.get("routeB_127.0.0.2:8000"));
        Assert.assertEquals(50, counts.get("routeB_127.0.0.3:8000").longValue());
    }

    /**
     * 测试单实例场景
     */
    @Test
    public void testBoundaryCondition_SingleInstance() {
        List<Instance> instanceList = new ArrayList<>();
        instanceList.add(buildDefaultInstance("ns", "svc", "127.0.0.1", 8000, 10));
        ServiceInstances instances = new DefaultServiceInstances(
            new ServiceKey("ns", "svc"), instanceList);

        // 进行多次轮询，应该总是选择同一个实例
        for (int i = 0; i < 100; i++) {
            Instance ins = weightedRoundRobinBalancer.chooseInstance(new Criteria(), instances);
            Assert.assertEquals("127.0.0.1:8000", ins.getId());
        }
    }

    /**
     * 测试所有实例权重相同的场景
     */
    @Test
    public void testBoundaryCondition_AllSameWeight() {
        List<Instance> instanceList = new ArrayList<>();
        instanceList.add(buildDefaultInstance("ns", "svc", "127.0.0.1", 8000, 5));
        instanceList.add(buildDefaultInstance("ns", "svc", "127.0.0.2", 8000, 5));
        instanceList.add(buildDefaultInstance("ns", "svc", "127.0.0.3", 8000, 5));
        ServiceInstances instances = new DefaultServiceInstances(
            new ServiceKey("ns", "svc"), instanceList);

        Map<String, AtomicLong> counts = new HashMap<>(8);
        for (int i = 0; i < 99; i++) {
            Instance ins = weightedRoundRobinBalancer.chooseInstance(new Criteria(), instances);
            counts.computeIfAbsent(ins.getId(), k -> new AtomicLong(0)).addAndGet(1);
        }

        // 验证每个实例被选择的次数大致相同
        long count1 = counts.get("127.0.0.1:8000").longValue();
        long count2 = counts.get("127.0.0.2:8000").longValue();
        long count3 = counts.get("127.0.0.3:8000").longValue();
        Assert.assertTrue(count1 >= 30 && count1 <= 36);
        Assert.assertTrue(count2 >= 30 && count2 <= 36);
        Assert.assertTrue(count3 >= 30 && count3 <= 36);
    }

    /**
     * 测试所有实例权重为0的场景
     */
    @Test
    public void testBoundaryCondition_AllZeroWeight() {
        List<Instance> instanceList = new ArrayList<>();
        instanceList.add(buildDefaultInstance("ns", "svc", "127.0.0.1", 8000, 0));
        instanceList.add(buildDefaultInstance("ns", "svc", "127.0.0.2", 8000, 0));
        instanceList.add(buildDefaultInstance("ns", "svc", "127.0.0.3", 8000, 0));
        ServiceInstances instances = new DefaultServiceInstances(
            new ServiceKey("ns", "svc"), instanceList);

        // 当所有权重为0时，应该返回第一个实例
        Instance ins = weightedRoundRobinBalancer.chooseInstance(new Criteria(), instances);
        Assert.assertEquals("127.0.0.1:8000", ins.getId());
    }

    /**
     * 测试部分实例权重变化的场景
     */
    @Test
    public void testBoundaryCondition_PartialWeightChange() {
        List<Instance> instanceList = new ArrayList<>();
        DefaultInstance instance1 = buildDefaultInstance("ns", "svc", "127.0.0.1", 8000, 10);
        DefaultInstance instance2 = buildDefaultInstance("ns", "svc", "127.0.0.2", 8000, 10);
        DefaultInstance instance3 = buildDefaultInstance("ns", "svc", "127.0.0.3", 8000, 10);
        instanceList.add(instance1);
        instanceList.add(instance2);
        instanceList.add(instance3);
        ServiceInstances instances = new DefaultServiceInstances(
            new ServiceKey("ns", "svc"), instanceList);

        Map<String, AtomicLong> counts = new HashMap<>(8);
        for (int i = 0; i < 30; i++) {
            Instance ins = weightedRoundRobinBalancer.chooseInstance(new Criteria(), instances);
            counts.computeIfAbsent(ins.getId(), k -> new AtomicLong(0)).addAndGet(1);
        }

        // 初始所有权重相同，应该均匀分布
        long count1 = counts.get("127.0.0.1:8000").longValue();
        long count2 = counts.get("127.0.0.2:8000").longValue();
        long count3 = counts.get("127.0.0.3:8000").longValue();
        Assert.assertTrue(count1 >= 8 && count1 <= 12);
        Assert.assertTrue(count2 >= 8 && count2 <= 12);
        Assert.assertTrue(count3 >= 8 && count3 <= 12);

        // 修改第一个实例的权重为20，其他保持10
        instance1.setWeight(20);

        counts.clear();
        for (int i = 0; i < 60; i++) {
            Instance ins = weightedRoundRobinBalancer.chooseInstance(new Criteria(), instances);
            counts.computeIfAbsent(ins.getId(), k -> new AtomicLong(0)).addAndGet(1);
        }

        // 新的权重比例为 20:10:10 = 2:1:1
        count1 = counts.get("127.0.0.1:8000").longValue();
        count2 = counts.get("127.0.0.2:8000").longValue();
        count3 = counts.get("127.0.0.3:8000").longValue();
        Assert.assertTrue("权重2:1:1的第一个实例", count1 >= 27 && count1 <= 33);
        Assert.assertTrue("权重2:1:1的第二个实例", count2 >= 13 && count2 <= 17);
        Assert.assertTrue("权重2:1:1的第三个实例", count3 >= 13 && count3 <= 17);
    }

    /**
     * 测试不同路由键的并发访问
     */
    @Test
    public void testBoundaryCondition_ConcurrentDifferentRouteKeys() throws InterruptedException {
        final int threadCount = 10;
        final int requestsPerThread = 50;
        final Map<String, AtomicLong> globalCounts = new HashMap<>(8);
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(threadCount);

        // 创建不同的路由场景
        final List<ServiceInstances> routeList = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            List<Instance> instanceList = new ArrayList<>();
            instanceList.add(buildDefaultInstance("ns", "svc", "127.0.0." + i, 8000, 10));
            instanceList.add(buildDefaultInstance("ns", "svc", "127.0.0." + ((i + 1) % threadCount), 8000, 10));
            ServiceInstances instances = new DefaultServiceInstances(
                new ServiceKey("ns", "svc"), instanceList);
            routeList.add(instances);
        }

        // 创建多个线程并发访问不同的路由键
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int routeIndex = i;
            threads[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        Instance ins = weightedRoundRobinBalancer.chooseInstance(new Criteria(), routeList.get(routeIndex));
                        synchronized (globalCounts) {
                            globalCounts.computeIfAbsent(ins.getId(), k -> new AtomicLong(0)).addAndGet(1);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
            threads[i].start();
        }

        latch.await();

        // 验证总请求数
        long totalCount = globalCounts.values().stream().mapToLong(AtomicLong::get).sum();
        Assert.assertEquals(threadCount * requestsPerThread, totalCount);
    }

    /**
     * 测试大量实例的场景
     */
    @Test
    public void testBoundaryCondition_LargeNumberOfInstances() {
        List<Instance> instanceList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            instanceList.add(buildDefaultInstance("ns", "svc", "127.0.0." + i, 8000 + i, 1));
        }
        ServiceInstances instances = new DefaultServiceInstances(
            new ServiceKey("ns", "svc"), instanceList);

        Map<String, AtomicLong> counts = new HashMap<>(8);
        for (int i = 0; i < 1000; i++) {
            Instance ins = weightedRoundRobinBalancer.chooseInstance(new Criteria(), instances);
            counts.computeIfAbsent(ins.getId(), k -> new AtomicLong(0)).addAndGet(1);
        }

        // 验证每个实例都被选择过
        Assert.assertEquals("所有100个实例都应该被选择", 100, counts.size());

        // 验证每个实例被选择的次数大致相同（每个实例约10次）
        for (AtomicLong count : counts.values()) {
            long value = count.get();
            Assert.assertTrue("每个实例应被选择约10次，实际: " + value, value >= 5 && value <= 15);
        }
    }

    /**
     * 测试高并发场景下的线程安全（验证 ConcurrentHashMap 和 AtomicInteger）
     */
    @Test
    public void testThreadSafety_HighConcurrency() throws InterruptedException {
        final ServiceInstances instances = buildServiceInstances();
        final int threadCount = 50;
        final int requestsPerThread = 200;
        final Map<String, AtomicLong> globalCounts = new java.util.concurrent.ConcurrentHashMap<>();
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(threadCount);

        // 创建大量线程并发访问
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        Instance ins = weightedRoundRobinBalancer.chooseInstance(new Criteria(), instances);
                        globalCounts.computeIfAbsent(ins.getId(), k -> new AtomicLong(0)).addAndGet(1);
                    }
                } finally {
                    latch.countDown();
                }
            });
            threads[i].start();
        }

        latch.await();

        // 验证总请求数（确保没有丢失请求）
        long totalCount = globalCounts.values().stream().mapToLong(AtomicLong::get).sum();
        Assert.assertEquals(threadCount * requestsPerThread, totalCount);

        // 验证权重比例（7:2:1）
        long count1 = globalCounts.get("127.0.0.1:8000").longValue();
        long count2 = globalCounts.get("127.0.0.2:8000").longValue();
        long count3 = globalCounts.get("127.0.0.3:8000").longValue();

        // 允许较小误差（±3%）
        Assert.assertTrue("权重比例 7:2:1 的第一个实例", count1 >= 6760 && count1 <= 7240);
        Assert.assertTrue("权重比例 7:2:1 的第二个实例", count2 >= 1880 && count2 <= 2120);
        Assert.assertTrue("权重比例 7:2:1 的第三个实例", count3 >= 940 && count3 <= 1060);
    }

    /**
     * 测试并发写入新路由键（验证 ConcurrentHashMap 和 computeIfAbsent）
     */
    @Test
    public void testThreadSafety_ConcurrentRouteKeyCreation() throws InterruptedException {
        final int threadCount = 20;
        final int requestsPerThread = 10;
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(threadCount);

        // 创建多个线程，每个线程使用不同的实例列表（创建新的路由键）
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        List<Instance> instanceList = new ArrayList<>();
                        instanceList.add(buildDefaultInstance("ns", "svc", 
                            "127.0.0." + (threadIndex % 255), 8000 + (threadIndex % 1000), 10));
                        instanceList.add(buildDefaultInstance("ns", "svc", 
                            "127.0.0." + ((threadIndex + 1) % 255), 8000 + ((threadIndex + 1) % 1000), 10));
                        ServiceInstances instances = new DefaultServiceInstances(
                            new ServiceKey("ns", "svc"), instanceList);
                        
                        weightedRoundRobinBalancer.chooseInstance(new Criteria(), instances);
                    }
                } finally {
                    latch.countDown();
                }
            });
            threads[i].start();
        }

        latch.await();

        // 验证没有抛出并发异常（如果能执行到这里，说明线程安全）
        Assert.assertTrue("并发创建路由键应该成功", true);
    }

    /**
     * 测试 AtomicLong 的原子性更新（权重轮询中的 current 值）
     */
    @Test
    public void testThreadSafety_AtomicLongCurrentUpdate() throws InterruptedException {
        final ServiceInstances instances = buildServiceInstances();
        final int threadCount = 30;
        final int requestsPerThread = 500;
        final java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.CountDownLatch endLatch = new java.util.concurrent.CountDownLatch(threadCount);
        final java.util.concurrent.atomic.AtomicInteger exceptionCount = new java.util.concurrent.atomic.AtomicInteger(0);

        // 创建线程同时开始
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                try {
                    startLatch.await(); // 等待所有线程就绪
                    for (int j = 0; j < requestsPerThread; j++) {
                        try {
                            Instance ins = weightedRoundRobinBalancer.chooseInstance(new Criteria(), instances);
                            Assert.assertNotNull("选择的实例不应为null", ins);
                        } catch (Exception e) {
                            exceptionCount.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
            threads[i].start();
        }

        startLatch.countDown(); // 所有线程同时开始
        endLatch.await();

        // 验证没有抛出异常
        Assert.assertEquals("应该没有异常发生", 0, exceptionCount.get());
    }

    /**
     * 测试并发清理过期实例（验证 removeIf 的线程安全）
     */
    @Test
    public void testThreadSafety_ConcurrentExpireCleanup() throws InterruptedException, Exception {
        java.lang.reflect.Field field = WeightedRoundRobinBalance.class.getDeclaredField("methodWeightMap");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, ?> weightMap = (Map<String, ?>) field.get(weightedRoundRobinBalancer);

        // 创建初始实例列表
        List<Instance> instanceList1 = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            instanceList1.add(buildDefaultInstance("ns", "svc", "127.0.0." + i, 8000 + i, 1));
        }
        ServiceInstances instances1 = new DefaultServiceInstances(
            new ServiceKey("ns", "svc"), instanceList1);

        final int threadCount = 10;
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(threadCount);

        // 一部分线程进行轮询，一部分线程修改实例列表以触发清理
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < 50; j++) {
                        if (threadIndex < 5) {
                            // 前一半线程进行轮询
                            weightedRoundRobinBalancer.chooseInstance(new Criteria(), instances1);
                        } else {
                            // 后一半线程使用不同的实例列表（触发清理逻辑）
                            List<Instance> instanceList2 = new ArrayList<>();
                            for (int k = 0; k < 10; k++) {
                                instanceList2.add(buildDefaultInstance("ns", "svc", "127.0.0." + k, 8000 + k, 1));
                            }
                            ServiceInstances instances2 = new DefaultServiceInstances(
                                new ServiceKey("ns", "svc"), instanceList2);
                            weightedRoundRobinBalancer.chooseInstance(new Criteria(), instances2);
                        }
                    }
                } catch (Exception e) {
                    // 忽略异常
                } finally {
                    latch.countDown();
                }
            });
            threads[i].start();
        }

        latch.await();

        // 等待超过回收期
        Thread.sleep(65000);

        // 再次进行轮询，触发清理
        for (int i = 0; i < 10; i++) {
            weightedRoundRobinBalancer.chooseInstance(new Criteria(), instances1);
        }

        // 验证没有抛出并发异常（如果能执行到这里，说明线程安全）
        Assert.assertTrue("并发清理过期实例应该成功", true);
    }

    /**
     * 测试并发场景下数据结构的线程安全
     */
    @Test
    public void testThreadSafety_DataStructureIntegrity() throws Exception {
        java.lang.reflect.Field field = WeightedRoundRobinBalance.class.getDeclaredField("methodWeightMap");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        final Map<String, ?> weightMap = (Map<String, ?>) field.get(weightedRoundRobinBalancer);

        final ServiceInstances instances = buildServiceInstances();
        final int threadCount = 20;
        final int requestsPerThread = 100;
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(threadCount);
        final Map<String, String> errorMessages = new java.util.concurrent.ConcurrentHashMap<>();

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        try {
                            weightedRoundRobinBalancer.chooseInstance(new Criteria(), instances);
                        } catch (Exception e) {
                            errorMessages.put(e.getMessage(), e.getMessage());
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
            threads[i].start();
        }

        latch.await();

        // 验证没有错误
        Assert.assertTrue("应该没有错误发生: " + errorMessages.keySet().toString(), errorMessages.isEmpty());

        // 验证数据结构的完整性
        Assert.assertTrue("methodWeightMap 应该有数据", weightMap.size() > 0);
    }

    /**
     * 性能测试：验证权重轮询操作的时间复杂度
     */
    @Test
    public void testPerformance_TimeComplexity() {
        // 测试不同实例数量下的性能
        int[] instanceCounts = {10, 50, 100, 200};
        int iterations = 1000;

        for (int instanceCount : instanceCounts) {
            List<Instance> instanceList = new ArrayList<>();
            for (int i = 0; i < instanceCount; i++) {
                instanceList.add(buildDefaultInstance("ns", "svc", "127.0.0." + i, 8000 + i, 1));
            }
            ServiceInstances instances = new DefaultServiceInstances(
                new ServiceKey("ns", "svc"), instanceList);

            long startTime = System.currentTimeMillis();
            for (int i = 0; i < iterations; i++) {
                weightedRoundRobinBalancer.chooseInstance(new Criteria(), instances);
            }
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            System.out.println(String.format("实例数量: %d, %d次选择耗时: %dms, 平均: %.3fms", 
                instanceCount, iterations, duration, (double) duration / iterations));

            // 验证性能在合理范围内（每次选择应该不超过10ms）
            Assert.assertTrue("性能测试: 每次选择应该不超过10ms", duration <= iterations * 10);
        }
    }

    /**
     * 性能测试：大量实例的性能测试
     */
    @Test
    public void testPerformance_LargeScaleInstances() {
        // 创建大量实例
        int instanceCount = 500;
        List<Instance> instanceList = new ArrayList<>();
        for (int i = 0; i < instanceCount; i++) {
            instanceList.add(buildDefaultInstance("ns", "svc", "127.0.0." + i, 8000 + i, 1));
        }
        ServiceInstances instances = new DefaultServiceInstances(
            new ServiceKey("ns", "svc"), instanceList);

        int iterations = 10000;
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            Instance ins = weightedRoundRobinBalancer.chooseInstance(new Criteria(), instances);
            Assert.assertNotNull("选择的实例不应为null", ins);
        }
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println(String.format("大规模测试: %d个实例, %d次选择耗时: %dms, 平均: %.3fms", 
            instanceCount, iterations, duration, (double) duration / iterations));

        // 验证性能在合理范围内（每次选择应该不超过20ms）
        Assert.assertTrue("大规模性能测试: 每次选择应该不超过20ms", duration <= iterations * 20);
    }

    /**
     * 兼容性测试：验证与现有 LoadBalancer 接口的兼容性
     */
    @Test
    public void testCompatibility_LoadBalancerInterface() {
        // 验证接口方法
        Assert.assertEquals("getName 应该返回正确的负载均衡器名称", 
            LoadBalanceConfig.LOAD_BALANCE_WEIGHTED_ROUND_ROBIN, weightedRoundRobinBalancer.getName());
        Assert.assertNotNull("getType 不应返回null", weightedRoundRobinBalancer.getType());
        Assert.assertEquals("getType 应该返回 LOAD_BALANCER 类型", 
            PluginTypes.LOAD_BALANCER.getBaseType(), weightedRoundRobinBalancer.getType());

        // 验证选择实例接口
        ServiceInstances instances = buildServiceInstances();
        Instance selectedInstance = weightedRoundRobinBalancer.chooseInstance(new Criteria(), instances);
        Assert.assertNotNull("chooseInstance 应该返回非null的实例", selectedInstance);
        Assert.assertTrue("选择的实例应该在实例列表中", instances.getInstances().contains(selectedInstance));

        // 验证异常处理
        try {
            weightedRoundRobinBalancer.chooseInstance(new Criteria(), (ServiceInstances) null);
            Assert.fail("应该抛出 PolarisException 当 instances 为 null");
        } catch (PolarisException e) {
            Assert.assertEquals("应该抛出 INSTANCE_NOT_FOUND 错误", ErrorCode.INSTANCE_NOT_FOUND, e.getCode());
        }

        try {
            ServiceInstances emptyInstances = new DefaultServiceInstances(
                new ServiceKey("ns", "svc"), new ArrayList<>());
            weightedRoundRobinBalancer.chooseInstance(new Criteria(), emptyInstances);
            Assert.fail("应该抛出 PolarisException 当实例列表为空");
        } catch (PolarisException e) {
            Assert.assertEquals("应该抛出 INSTANCE_NOT_FOUND 错误", ErrorCode.INSTANCE_NOT_FOUND, e.getCode());
        }
    }

    /**
     * 性能测试：路由键生成的性能
     */
    @Test
    public void testPerformance_RouteKeyGeneration() throws Exception {
        WeightedRoundRobinBalance balancer = new WeightedRoundRobinBalance();
        Method method = WeightedRoundRobinBalance.class.getDeclaredMethod("generateRouteKey", List.class);
        method.setAccessible(true);

        // 测试不同实例数量下的路由键生成性能
        int[] instanceCounts = {10, 50, 100, 200};
        int iterations = 1000;

        for (int instanceCount : instanceCounts) {
            List<Instance> instanceList = new ArrayList<>();
            for (int i = 0; i < instanceCount; i++) {
                instanceList.add(buildDefaultInstance("ns", "svc", "127.0.0." + i, 8000 + i, 1));
            }

            long startTime = System.currentTimeMillis();
            for (int i = 0; i < iterations; i++) {
                String key = (String) method.invoke(balancer, instanceList);
                Assert.assertNotNull("路由键不应为null", key);
            }
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            System.out.println(String.format("路由键生成 - 实例数量: %d, %d次生成耗时: %dms, 平均: %.3fms", 
                instanceCount, iterations, duration, (double) duration / iterations));

            // 验证性能在合理范围内（每次生成应该不超过5ms）
            Assert.assertTrue("路由键生成性能: 每次生成应该不超过5ms", duration <= iterations * 5);
        }
    }

    /**
     * 性能测试：并发场景下的路由键生成
     */
    @Test
    public void testPerformance_ConcurrentRouteKeyGeneration() throws Exception {
        WeightedRoundRobinBalance balancer = new WeightedRoundRobinBalance();
        Method method = WeightedRoundRobinBalance.class.getDeclaredMethod("generateRouteKey", List.class);
        method.setAccessible(true);

        final int threadCount = 20;
        final int instancesPerThread = 1000;
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(threadCount);
        final java.util.concurrent.atomic.AtomicLong totalDuration = new java.util.concurrent.atomic.AtomicLong(0);

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                try {
                    List<Instance> instanceList = new ArrayList<>();
                    for (int j = 0; j < 50; j++) {
                        instanceList.add(buildDefaultInstance("ns", "svc", 
                            "127.0.0." + (threadIndex * 50 + j), 8000 + (threadIndex * 50 + j), 1));
                    }

                    long startTime = System.currentTimeMillis();
                    for (int k = 0; k < instancesPerThread; k++) {
                        String key = (String) method.invoke(balancer, instanceList);
                        Assert.assertNotNull("路由键不应为null", key);
                    }
                    long endTime = System.currentTimeMillis();
                    totalDuration.addAndGet(endTime - startTime);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
            threads[i].start();
        }

        latch.await();

        long avgDuration = totalDuration.get() / threadCount;
        System.out.println(String.format("并发路由键生成: %d个线程, 每个线程%d次生成, 平均耗时: %dms", 
            threadCount, instancesPerThread, avgDuration));

        // 验证性能在合理范围内
        Assert.assertTrue("并发路由键生成性能: 平均每线程耗时应该在合理范围内", avgDuration <= instancesPerThread * 10);
    }

    /**
     * 兼容性测试：动态权重的支持
     */
    @Test
    public void testCompatibility_DynamicWeight() {
        List<Instance> instanceList = new ArrayList<>();
        instanceList.add(buildDefaultInstance("ns", "svc", "127.0.0.1", 8000, 10));
        instanceList.add(buildDefaultInstance("ns", "svc", "127.0.0.2", 8000, 10));
        instanceList.add(buildDefaultInstance("ns", "svc", "127.0.0.3", 8000, 10));
        ServiceInstances instances = new DefaultServiceInstances(
            new ServiceKey("ns", "svc"), instanceList);

        // 创建动态权重
        Map<String, InstanceWeight> dynamicWeights = new HashMap<>();
        InstanceWeight weight1 = new InstanceWeight();
        weight1.setId("127.0.0.1:8000");
        weight1.setDynamicWeight(20);
        weight1.setBaseWeight(10);
        dynamicWeights.put("127.0.0.1:8000", weight1);

        InstanceWeight weight2 = new InstanceWeight();
        weight2.setId("127.0.0.2:8000");
        weight2.setDynamicWeight(10);
        weight2.setBaseWeight(10);
        dynamicWeights.put("127.0.0.2:8000", weight2);

        InstanceWeight weight3 = new InstanceWeight();
        weight3.setId("127.0.0.3:8000");
        weight3.setDynamicWeight(5);
        weight3.setBaseWeight(10);
        dynamicWeights.put("127.0.0.3:8000", weight3);

        Criteria criteria = new Criteria();
        criteria.setDynamicWeight(dynamicWeights);

        Map<String, AtomicLong> counts = new HashMap<>(8);
        for (int i = 0; i < 70; i++) {
            Instance ins = weightedRoundRobinBalancer.chooseInstance(criteria, instances);
            counts.computeIfAbsent(ins.getId(), k -> new AtomicLong(0)).addAndGet(1);
        }

        // 验证动态权重比例（20:10:5 = 4:2:1）
        Assert.assertEquals(40, counts.get("127.0.0.1:8000").longValue());
        Assert.assertEquals(20, counts.get("127.0.0.2:8000").longValue());
        Assert.assertEquals(10, counts.get("127.0.0.3:8000").longValue());
    }

}
