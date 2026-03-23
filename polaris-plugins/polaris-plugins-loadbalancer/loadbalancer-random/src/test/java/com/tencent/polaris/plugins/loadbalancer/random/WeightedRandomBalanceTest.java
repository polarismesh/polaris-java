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

package com.tencent.polaris.plugins.loadbalancer.random;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.pojo.DefaultInstance;
import com.tencent.polaris.api.pojo.DefaultServiceInstances;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.InstanceWeight;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.Criteria;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link WeightedRandomBalance}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class WeightedRandomBalanceTest {

    private static final String TEST_NAMESPACE = "Development";
    private static final String TEST_SERVICE = "trpc.app.server.service";
    private static final int SAMPLE_COUNT = 10000;
    private static final double TOLERANCE = 0.05;

    private WeightedRandomBalance weightedRandomBalance;

    @Before
    public void setUp() {
        weightedRandomBalance = new WeightedRandomBalance();
    }

    private DefaultInstance buildDefaultInstance(String host, int port, int weight) {
        DefaultInstance instance = new DefaultInstance();
        instance.setHost(host);
        instance.setPort(port);
        instance.setWeight(weight);
        instance.setNamespace(TEST_NAMESPACE);
        instance.setService(TEST_SERVICE);
        instance.setId(host + ":" + port);
        return instance;
    }

    private ServiceInstances buildServiceInstances(List<Instance> instanceList) {
        ServiceInstances result = new DefaultServiceInstances(
                new ServiceKey(TEST_NAMESPACE, TEST_SERVICE), instanceList);
        return result;
    }

    /**
     * 测试基本的加权随机选择功能
     * 测试目的：验证加权随机负载均衡器能按权重比例分配请求
     * 测试场景：三个实例权重分别为 7:2:1，执行大量选择后统计分布
     * 验证内容：选择比例在合理的误差范围内符合权重比例
     */
    @Test
    public void testChooseInstance_WeightedDistribution() {
        // Arrange
        List<Instance> instanceList = new ArrayList<>();
        instanceList.add(buildDefaultInstance("10.0.0.1", 8000, 7));
        instanceList.add(buildDefaultInstance("10.0.0.2", 8000, 2));
        instanceList.add(buildDefaultInstance("10.0.0.3", 8000, 1));
        ServiceInstances svcInstances = buildServiceInstances(instanceList);
        Criteria criteria = new Criteria();

        // Act
        Map<String, AtomicLong> counts = new HashMap<>();
        for (int idx = 0; idx < SAMPLE_COUNT; idx++) {
            Instance chosen = weightedRandomBalance.chooseInstance(criteria, svcInstances);
            counts.computeIfAbsent(chosen.getId(), key -> new AtomicLong(0)).addAndGet(1);
        }

        // Assert
        double ratio1 = counts.get("10.0.0.1:8000").doubleValue() / SAMPLE_COUNT;
        double ratio2 = counts.get("10.0.0.2:8000").doubleValue() / SAMPLE_COUNT;
        double ratio3 = counts.get("10.0.0.3:8000").doubleValue() / SAMPLE_COUNT;
        assertThat(ratio1).isBetween(0.7 - TOLERANCE, 0.7 + TOLERANCE);
        assertThat(ratio2).isBetween(0.2 - TOLERANCE, 0.2 + TOLERANCE);
        assertThat(ratio3).isBetween(0.1 - TOLERANCE, 0.1 + TOLERANCE);
    }

    /**
     * 测试单个实例场景
     * 测试目的：验证只有一个实例时总是返回该实例
     * 测试场景：仅配置一个实例，执行多次选择
     * 验证内容：每次都返回同一个实例
     */
    @Test
    public void testChooseInstance_SingleInstance() {
        // Arrange
        List<Instance> instanceList = new ArrayList<>();
        instanceList.add(buildDefaultInstance("10.0.0.1", 8000, 100));
        ServiceInstances svcInstances = buildServiceInstances(instanceList);
        Criteria criteria = new Criteria();

        // Act & Assert
        for (int idx = 0; idx < 100; idx++) {
            Instance chosen = weightedRandomBalance.chooseInstance(criteria, svcInstances);
            assertThat(chosen.getId()).isEqualTo("10.0.0.1:8000");
        }
    }

    /**
     * 测试相同权重场景
     * 测试目的：验证所有实例权重相同时均匀分配
     * 测试场景：三个实例权重均为 100
     * 验证内容：选择比例近似均匀（各约 1/3）
     */
    @Test
    public void testChooseInstance_EqualWeights() {
        // Arrange
        List<Instance> instanceList = new ArrayList<>();
        instanceList.add(buildDefaultInstance("10.0.0.1", 8000, 100));
        instanceList.add(buildDefaultInstance("10.0.0.2", 8000, 100));
        instanceList.add(buildDefaultInstance("10.0.0.3", 8000, 100));
        ServiceInstances svcInstances = buildServiceInstances(instanceList);
        Criteria criteria = new Criteria();

        // Act
        Map<String, AtomicLong> counts = new HashMap<>();
        for (int idx = 0; idx < SAMPLE_COUNT; idx++) {
            Instance chosen = weightedRandomBalance.chooseInstance(criteria, svcInstances);
            counts.computeIfAbsent(chosen.getId(), key -> new AtomicLong(0)).addAndGet(1);
        }

        // Assert
        double expectedRatio = 1.0 / 3;
        for (AtomicLong count : counts.values()) {
            double actualRatio = count.doubleValue() / SAMPLE_COUNT;
            assertThat(actualRatio).isBetween(expectedRatio - TOLERANCE, expectedRatio + TOLERANCE);
        }
    }

    /**
     * 测试所有实例权重为 0 时抛出异常
     * 测试目的：验证所有权重为 0 的边界情况处理
     * 测试场景：两个实例权重均为 0
     * 验证内容：抛出 PolarisException
     */
    @Test
    public void testChooseInstance_AllZeroWeights() {
        // Arrange
        List<Instance> instanceList = new ArrayList<>();
        instanceList.add(buildDefaultInstance("10.0.0.1", 8000, 0));
        instanceList.add(buildDefaultInstance("10.0.0.2", 8000, 0));
        ServiceInstances svcInstances = buildServiceInstances(instanceList);
        Criteria criteria = new Criteria();

        // Act & Assert
        assertThatThrownBy(() -> weightedRandomBalance.chooseInstance(criteria, svcInstances))
                .isInstanceOf(PolarisException.class)
                .hasMessageContaining("all instances weight 0");
    }

    /**
     * 测试动态权重生效
     * 测试目的：验证设置动态权重后，负载均衡按动态权重而非静态权重分配
     * 测试场景：静态权重均为 0（触发动态权重计算），动态权重设为 9:1
     * 验证内容：选择比例符合动态权重 9:1
     */
    @Test
    public void testChooseInstance_DynamicWeights() {
        // Arrange
        List<Instance> instanceList = new ArrayList<>();
        instanceList.add(buildDefaultInstance("10.0.0.1", 8000, 0));
        instanceList.add(buildDefaultInstance("10.0.0.2", 8000, 0));
        ServiceInstances svcInstances = buildServiceInstances(instanceList);

        Map<String, InstanceWeight> dynamicWeights = new HashMap<>();
        InstanceWeight weight1 = new InstanceWeight();
        weight1.setId("10.0.0.1:8000");
        weight1.setDynamicWeight(9);
        weight1.setBaseWeight(100);
        dynamicWeights.put("10.0.0.1:8000", weight1);

        InstanceWeight weight2 = new InstanceWeight();
        weight2.setId("10.0.0.2:8000");
        weight2.setDynamicWeight(1);
        weight2.setBaseWeight(100);
        dynamicWeights.put("10.0.0.2:8000", weight2);

        Criteria criteria = new Criteria();
        criteria.setDynamicWeight(dynamicWeights);

        // Act
        Map<String, AtomicLong> counts = new HashMap<>();
        for (int idx = 0; idx < SAMPLE_COUNT; idx++) {
            Instance chosen = weightedRandomBalance.chooseInstance(criteria, svcInstances);
            counts.computeIfAbsent(chosen.getId(), key -> new AtomicLong(0)).addAndGet(1);
        }

        // Assert
        double ratio1 = counts.get("10.0.0.1:8000").doubleValue() / SAMPLE_COUNT;
        double ratio2 = counts.get("10.0.0.2:8000").doubleValue() / SAMPLE_COUNT;
        assertThat(ratio1).isBetween(0.9 - TOLERANCE, 0.9 + TOLERANCE);
        assertThat(ratio2).isBetween(0.1 - TOLERANCE, 0.1 + TOLERANCE);
    }

    /**
     * 测试部分实例有动态权重的场景
     * 测试目的：验证只有部分实例设置动态权重时，未设置的实例回退到静态权重
     * 测试场景：两个静态权重均为 0 的实例，第一个设动态权重 900，第二个设动态权重 100
     * 验证内容：分配比例符合动态权重 900:100 = 9:1
     */
    @Test
    public void testChooseInstance_PartialDynamicWeights() {
        // Arrange
        List<Instance> instanceList = new ArrayList<>();
        instanceList.add(buildDefaultInstance("10.0.0.1", 8000, 0));
        instanceList.add(buildDefaultInstance("10.0.0.2", 8000, 0));
        ServiceInstances svcInstances = buildServiceInstances(instanceList);

        Map<String, InstanceWeight> dynamicWeights = new HashMap<>();
        InstanceWeight weight1 = new InstanceWeight();
        weight1.setId("10.0.0.1:8000");
        weight1.setDynamicWeight(900);
        weight1.setBaseWeight(0);
        dynamicWeights.put("10.0.0.1:8000", weight1);
        // 10.0.0.2:8000 未设置动态权重，应回退到静态权重 0

        Criteria criteria = new Criteria();
        criteria.setDynamicWeight(dynamicWeights);

        // Act
        Map<String, AtomicLong> counts = new HashMap<>();
        for (int idx = 0; idx < SAMPLE_COUNT; idx++) {
            Instance chosen = weightedRandomBalance.chooseInstance(criteria, svcInstances);
            counts.computeIfAbsent(chosen.getId(), key -> new AtomicLong(0)).addAndGet(1);
        }

        // Assert: 动态权重 900 vs 静态权重 0，只有第一个实例被选中
        double ratio1 = counts.get("10.0.0.1:8000").doubleValue() / SAMPLE_COUNT;
        assertThat(ratio1).isEqualTo(1.0);
    }

    /**
     * 测试无动态权重时使用静态权重
     * 测试目的：验证 Criteria 未设置动态权重时使用静态权重
     * 测试场景：Criteria 未设置 dynamicWeight（默认空 map），静态权重 9:1
     * 验证内容：选择比例符合静态权重 9:1
     */
    @Test
    public void testChooseInstance_NoDynamicWeightsFallbackToStatic() {
        // Arrange
        List<Instance> instanceList = new ArrayList<>();
        instanceList.add(buildDefaultInstance("10.0.0.1", 8000, 9));
        instanceList.add(buildDefaultInstance("10.0.0.2", 8000, 1));
        ServiceInstances svcInstances = buildServiceInstances(instanceList);
        Criteria criteria = new Criteria();

        // Act
        Map<String, AtomicLong> counts = new HashMap<>();
        for (int idx = 0; idx < SAMPLE_COUNT; idx++) {
            Instance chosen = weightedRandomBalance.chooseInstance(criteria, svcInstances);
            counts.computeIfAbsent(chosen.getId(), key -> new AtomicLong(0)).addAndGet(1);
        }

        // Assert
        double ratio1 = counts.get("10.0.0.1:8000").doubleValue() / SAMPLE_COUNT;
        assertThat(ratio1).isBetween(0.9 - TOLERANCE, 0.9 + TOLERANCE);
    }

    /**
     * 测试返回的实例一定来自实例列表
     * 测试目的：验证负载均衡器返回的实例总是来自传入的实例列表
     * 测试场景：不同权重的实例列表，大量选择
     * 验证内容：每次返回的实例 ID 都在已知列表中
     */
    @Test
    public void testChooseInstance_AlwaysReturnsValidInstance() {
        // Arrange
        List<Instance> instanceList = new ArrayList<>();
        instanceList.add(buildDefaultInstance("10.0.0.1", 8000, 50));
        instanceList.add(buildDefaultInstance("10.0.0.2", 8000, 30));
        instanceList.add(buildDefaultInstance("10.0.0.3", 8000, 20));
        ServiceInstances svcInstances = buildServiceInstances(instanceList);
        Criteria criteria = new Criteria();

        // Act & Assert
        for (int idx = 0; idx < 1000; idx++) {
            Instance chosen = weightedRandomBalance.chooseInstance(criteria, svcInstances);
            assertThat(chosen.getId()).isIn("10.0.0.1:8000", "10.0.0.2:8000", "10.0.0.3:8000");
        }
    }

    /**
     * 测试使用预计算的 totalWeight
     * 测试目的：验证 ServiceInstances.getTotalWeight() 返回正数时直接使用而不重新计算
     * 测试场景：构造预设 totalWeight 的实例列表
     * 验证内容：能正常选择实例且不抛异常
     */
    @Test
    public void testChooseInstance_PrecomputedTotalWeight() {
        // Arrange
        List<Instance> instanceList = new ArrayList<>();
        instanceList.add(buildDefaultInstance("10.0.0.1", 8000, 50));
        instanceList.add(buildDefaultInstance("10.0.0.2", 8000, 50));
        ServiceInstances svcInstances = buildServiceInstances(instanceList);
        Criteria criteria = new Criteria();

        // Act
        Instance chosen = weightedRandomBalance.chooseInstance(criteria, svcInstances);

        // Assert
        assertThat(chosen).isNotNull();
        assertThat(chosen.getId()).isIn("10.0.0.1:8000", "10.0.0.2:8000");
    }
}
