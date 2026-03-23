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

package com.tencent.polaris.plugin.lossless.warmup;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.pojo.DefaultInstance;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.InstanceWeight;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.plugin.lossless.common.LosslessRuleDictionary;
import com.tencent.polaris.plugin.lossless.common.LosslessUtils;
import com.tencent.polaris.specification.api.v1.traffic.manage.LosslessProto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Test for {@link WarmupWeightAdjuster}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class WarmupWeightAdjusterTest {

    private static final String TEST_NAMESPACE = "Development";
    private static final String TEST_SERVICE = "test.warmup.service";
    private static final ServiceKey TEST_SERVICE_KEY = new ServiceKey(TEST_NAMESPACE, TEST_SERVICE);

    @Mock
    private Extensions extensions;

    @Mock
    private ServiceInstances serviceInstances;

    private WarmupWeightAdjuster warmupWeightAdjuster;

    private LosslessRuleDictionary losslessRuleDictionary;

    @Before
    public void setUp() throws Exception {
        warmupWeightAdjuster = new WarmupWeightAdjuster();
        losslessRuleDictionary = new LosslessRuleDictionary();

        // 通过反射注入私有字段
        setPrivateField(warmupWeightAdjuster, "extensions", extensions);
        setPrivateField(warmupWeightAdjuster, "losslessRuleDictionary", losslessRuleDictionary);

        when(serviceInstances.getNamespace()).thenReturn(TEST_NAMESPACE);
        when(serviceInstances.getService()).thenReturn(TEST_SERVICE);
        when(serviceInstances.getServiceKey()).thenReturn(TEST_SERVICE_KEY);
    }

    private static void setPrivateField(Object object, String fieldName, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(object, value);
    }

    @SuppressWarnings("unchecked")
    private static <T> T getPrivateField(Object object, String fieldName)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(object);
    }

    private DefaultInstance buildInstance(String instId, int weight, Long createTime) {
        DefaultInstance instance = new DefaultInstance();
        instance.setId(instId);
        instance.setHost("127.0.0.1");
        instance.setPort(8080);
        instance.setWeight(weight);
        instance.setNamespace(TEST_NAMESPACE);
        instance.setService(TEST_SERVICE);
        instance.setCreateTime(createTime);
        return instance;
    }

    private LosslessProto.Warmup buildWarmup(boolean enable, int intervalSecond, int curvature,
            boolean enableOverloadProtection, int overloadProtectionThreshold) {
        return LosslessProto.Warmup.newBuilder()
                .setEnable(enable)
                .setIntervalSecond(intervalSecond)
                .setCurvature(curvature)
                .setEnableOverloadProtection(enableOverloadProtection)
                .setOverloadProtectionThreshold(overloadProtectionThreshold)
                .build();
    }

    private LosslessProto.LosslessRule buildLosslessRule(LosslessProto.Warmup warmup) {
        return LosslessProto.LosslessRule.newBuilder()
                .setLosslessOnline(LosslessProto.LosslessOnline.newBuilder().setWarmup(warmup).build())
                .build();
    }

    private LosslessProto.LosslessRule buildLosslessRuleWithMetadata(LosslessProto.Warmup warmup,
            String metadataKey, String metadataValue) {
        return LosslessProto.LosslessRule.newBuilder()
                .setLosslessOnline(LosslessProto.LosslessOnline.newBuilder().setWarmup(warmup).build())
                .putMetadata(metadataKey, metadataValue)
                .build();
    }

    // ==================== 基础功能测试 ====================

    /**
     * 测试插件名称
     * 测试目的：验证 getName 返回正确的插件名称
     * 测试场景：调用 getName 方法
     * 验证内容：返回 "warmup"
     */
    @Test
    public void testGetName() {
        // Act & Assert
        assertThat(warmupWeightAdjuster.getName()).isEqualTo("warmup");
    }

    /**
     * 测试空实例列表
     * 测试目的：验证实例列表为空时返回空 map
     * 测试场景：ServiceInstances 的 getInstances 返回空列表
     * 验证内容：返回空集合
     */
    @Test
    public void testTimingAdjustDynamicWeight_EmptyInstances() {
        // Arrange
        when(serviceInstances.getInstances()).thenReturn(Collections.emptyList());

        // Act
        Map<String, InstanceWeight> result = warmupWeightAdjuster.timingAdjustDynamicWeight(
                new HashMap<>(), serviceInstances);

        // Assert
        assertThat(result).isEmpty();
    }

    // ==================== 整数除法修复验证 ====================

    /**
     * 测试过载保护百分比计算（无 metadata 路径）
     * 测试目的：验证过载保护的百分比计算正确，不存在整数除法截断问题
     * 测试场景：5 个实例中有 2 个正在预热，阈值设为 30%，实际百分比 40% 应触发保护
     * 验证内容：返回原始权重（即触发了过载保护）
     */
    @Test
    public void testOverloadProtection_IntegerDivision_LosslessRule() {
        // Arrange
        long currentTime = System.currentTimeMillis();
        LosslessProto.Warmup warmup = buildWarmup(true, 120, 2, true, 30);
        LosslessProto.LosslessRule losslessRule = buildLosslessRule(warmup);

        // 5 个实例，其中 2 个在预热期（40% > 30% 阈值，应触发保护）
        List<Instance> instanceList = new ArrayList<>();
        instanceList.add(buildInstance("inst-1", 100, currentTime - 30000));
        instanceList.add(buildInstance("inst-2", 100, currentTime - 30000));
        instanceList.add(buildInstance("inst-3", 100, currentTime - 300000));
        instanceList.add(buildInstance("inst-4", 100, currentTime - 300000));
        instanceList.add(buildInstance("inst-5", 100, currentTime - 300000));
        when(serviceInstances.getInstances()).thenReturn(instanceList);

        Map<String, InstanceWeight> originWeight = new HashMap<>();
        List<LosslessProto.LosslessRule> ruleList = new ArrayList<>();
        ruleList.add(losslessRule);

        // 初始化 metadata 规则缓存（无 metadata 的规则解析出空 map）
        losslessRuleDictionary.putMetadataLosslessRules(TEST_SERVICE_KEY, ruleList);

        try (MockedStatic<LosslessUtils> mockedUtils = Mockito.mockStatic(LosslessUtils.class)) {
            mockedUtils.when(() -> LosslessUtils.getLosslessRules(eq(extensions), anyString(), anyString()))
                    .thenReturn(ruleList);

            // Act
            Map<String, InstanceWeight> result = warmupWeightAdjuster.timingAdjustDynamicWeight(
                    originWeight, serviceInstances);

            // Assert: 过载保护应触发，返回原始权重（空 map）
            assertThat(result).isEqualTo(originWeight);
        }
    }

    /**
     * 测试过载保护未触发的场景
     * 测试目的：验证预热实例比例未超过阈值时不触发过载保护
     * 测试场景：10 个实例中有 2 个正在预热（20%），阈值设为 30%
     * 验证内容：返回调整后的权重，包含预热权重
     */
    @Test
    public void testOverloadProtection_NotTriggered_LosslessRule() {
        // Arrange
        long currentTime = System.currentTimeMillis();
        LosslessProto.Warmup warmup = buildWarmup(true, 120, 2, true, 30);
        LosslessProto.LosslessRule losslessRule = buildLosslessRule(warmup);

        // 10 个实例，其中 2 个在预热期（20% < 30% 阈值，不应触发保护）
        List<Instance> instanceList = new ArrayList<>();
        instanceList.add(buildInstance("inst-1", 100, currentTime - 30000));
        instanceList.add(buildInstance("inst-2", 100, currentTime - 30000));
        for (int idx = 3; idx <= 10; idx++) {
            instanceList.add(buildInstance("inst-" + idx, 100, currentTime - 300000));
        }
        when(serviceInstances.getInstances()).thenReturn(instanceList);

        Map<String, InstanceWeight> originWeight = new HashMap<>();
        List<LosslessProto.LosslessRule> ruleList = new ArrayList<>();
        ruleList.add(losslessRule);

        // 初始化 metadata 规则缓存
        losslessRuleDictionary.putMetadataLosslessRules(TEST_SERVICE_KEY, ruleList);

        try (MockedStatic<LosslessUtils> mockedUtils = Mockito.mockStatic(LosslessUtils.class)) {
            mockedUtils.when(() -> LosslessUtils.getLosslessRules(eq(extensions), anyString(), anyString()))
                    .thenReturn(ruleList);

            // Act
            Map<String, InstanceWeight> result = warmupWeightAdjuster.timingAdjustDynamicWeight(
                    originWeight, serviceInstances);

            // Assert: 未触发过载保护，应有预热调整的权重
            assertThat(result).isNotEmpty();
            // 预热中的实例动态权重应小于基准权重
            assertThat(result.get("inst-1").getDynamicWeight()).isLessThan(100);
            assertThat(result.get("inst-2").getDynamicWeight()).isLessThan(100);
            // 已完成预热的实例权重应等于基准权重
            assertThat(result.get("inst-3").getDynamicWeight()).isEqualTo(100);
        }
    }

    /**
     * 测试过载保护百分比计算（metadata 路径）
     * 测试目的：验证 metadata 路径下的过载保护百分比计算正确
     * 测试场景：5 个实例中 2 个正在预热，阈值 30%，实际 40% 应触发保护
     * 验证内容：返回原始权重
     */
    @Test
    public void testOverloadProtection_IntegerDivision_MetadataRule() {
        // Arrange
        long currentTime = System.currentTimeMillis();
        LosslessProto.Warmup warmup = buildWarmup(true, 120, 2, true, 30);
        LosslessProto.LosslessRule losslessRule = buildLosslessRuleWithMetadata(warmup, "version", "v1");

        List<Instance> instanceList = new ArrayList<>();
        for (int idx = 1; idx <= 5; idx++) {
            DefaultInstance inst = buildInstance("inst-" + idx, 100,
                    idx <= 2 ? currentTime - 30000 : currentTime - 300000);
            Map<String, String> metadata = new HashMap<>();
            metadata.put("version", "v1");
            inst.setMetadata(metadata);
            instanceList.add(inst);
        }
        when(serviceInstances.getInstances()).thenReturn(instanceList);

        Map<String, InstanceWeight> originWeight = new HashMap<>();
        List<LosslessProto.LosslessRule> ruleList = new ArrayList<>();
        ruleList.add(losslessRule);

        // 放入 metadata 规则缓存
        losslessRuleDictionary.putMetadataLosslessRules(TEST_SERVICE_KEY, ruleList);

        try (MockedStatic<LosslessUtils> mockedUtils = Mockito.mockStatic(LosslessUtils.class)) {
            mockedUtils.when(() -> LosslessUtils.getLosslessRules(eq(extensions), anyString(), anyString()))
                    .thenReturn(ruleList);
            mockedUtils.when(() -> LosslessUtils.getMatchMetadataLosslessRule(
                    Mockito.any(Instance.class), Mockito.anyMap()))
                    .thenReturn(losslessRule);

            // Act
            Map<String, InstanceWeight> result = warmupWeightAdjuster.timingAdjustDynamicWeight(
                    originWeight, serviceInstances);

            // Assert: 过载保护应触发，返回原始权重
            assertThat(result).isEqualTo(originWeight);
        }
    }

    // ==================== getInstanceWeight 权重计算测试 ====================

    /**
     * 测试权重计算：实例无 createTime 时返回基准权重
     * 测试目的：验证 createTime 为 null 时不进行预热计算
     * 测试场景：实例的 createTime 为 null
     * 验证内容：dynamicWeight 等于 baseWeight
     */
    @Test
    public void testGetInstanceWeight_NullCreateTime() throws Exception {
        // Arrange
        LosslessProto.Warmup warmup = buildWarmup(true, 120, 2, false, 0);
        DefaultInstance instance = buildInstance("inst-1", 100, null);
        Map<String, InstanceWeight> dynamicWeight = new HashMap<>();

        // Act
        Method method = WarmupWeightAdjuster.class.getDeclaredMethod("getInstanceWeight",
                Map.class, Instance.class, LosslessProto.Warmup.class, long.class);
        method.setAccessible(true);
        InstanceWeight result = (InstanceWeight) method.invoke(warmupWeightAdjuster,
                dynamicWeight, instance, warmup, System.currentTimeMillis());

        // Assert
        assertThat(result.getDynamicWeight()).isEqualTo(100);
        assertThat(result.getBaseWeight()).isEqualTo(100);
    }

    /**
     * 测试权重计算：预热已完成时返回基准权重
     * 测试目的：验证实例运行时间超过预热区间后返回基准权重
     * 测试场景：实例已运行 300 秒，预热区间 120 秒
     * 验证内容：dynamicWeight 等于 baseWeight
     */
    @Test
    public void testGetInstanceWeight_WarmupCompleted() throws Exception {
        // Arrange
        long currentTime = System.currentTimeMillis();
        LosslessProto.Warmup warmup = buildWarmup(true, 120, 2, false, 0);
        DefaultInstance instance = buildInstance("inst-1", 100, currentTime - 300000);
        Map<String, InstanceWeight> dynamicWeight = new HashMap<>();

        // Act
        Method method = WarmupWeightAdjuster.class.getDeclaredMethod("getInstanceWeight",
                Map.class, Instance.class, LosslessProto.Warmup.class, long.class);
        method.setAccessible(true);
        InstanceWeight result = (InstanceWeight) method.invoke(warmupWeightAdjuster,
                dynamicWeight, instance, warmup, currentTime);

        // Assert
        assertThat(result.getDynamicWeight()).isEqualTo(100);
        assertThat(result.getBaseWeight()).isEqualTo(100);
    }

    /**
     * 测试权重计算：预热进行中时权重按曲线递增
     * 测试目的：验证预热期间权重按 pow(uptime/interval, curvature) * baseWeight 计算
     * 测试场景：实例已运行 60 秒，预热区间 120 秒，curvature=2，baseWeight=100
     * 验证内容：权重 = ceil((60/120)^2 * 100) = ceil(0.25 * 100) = 25
     */
    @Test
    public void testGetInstanceWeight_DuringWarmup() throws Exception {
        // Arrange
        long currentTime = System.currentTimeMillis();
        LosslessProto.Warmup warmup = buildWarmup(true, 120, 2, false, 0);
        // 运行 60 秒
        DefaultInstance instance = buildInstance("inst-1", 100, currentTime - 60000);
        Map<String, InstanceWeight> dynamicWeight = new HashMap<>();

        // Act
        Method method = WarmupWeightAdjuster.class.getDeclaredMethod("getInstanceWeight",
                Map.class, Instance.class, LosslessProto.Warmup.class, long.class);
        method.setAccessible(true);
        InstanceWeight result = (InstanceWeight) method.invoke(warmupWeightAdjuster,
                dynamicWeight, instance, warmup, currentTime);

        // Assert: (60/120)^2 * 100 = 0.25 * 100 = 25
        assertThat(result.getDynamicWeight()).isEqualTo(25);
        assertThat(result.getBaseWeight()).isEqualTo(100);
        assertThat(result.isDynamicWeightValid()).isTrue();
    }

    /**
     * 测试权重计算：权重不会超过基准权重
     * 测试目的：验证计算的动态权重不会超过 baseWeight
     * 测试场景：curvature 为 1（线性），uptime 接近 interval
     * 验证内容：dynamicWeight 不大于 baseWeight
     */
    @Test
    public void testGetInstanceWeight_NeverExceedsBaseWeight() throws Exception {
        // Arrange
        long currentTime = System.currentTimeMillis();
        LosslessProto.Warmup warmup = buildWarmup(true, 120, 1, false, 0);
        DefaultInstance instance = buildInstance("inst-1", 100, currentTime - 119000);
        Map<String, InstanceWeight> dynamicWeight = new HashMap<>();

        // Act
        Method method = WarmupWeightAdjuster.class.getDeclaredMethod("getInstanceWeight",
                Map.class, Instance.class, LosslessProto.Warmup.class, long.class);
        method.setAccessible(true);
        InstanceWeight result = (InstanceWeight) method.invoke(warmupWeightAdjuster,
                dynamicWeight, instance, warmup, currentTime);

        // Assert
        assertThat(result.getDynamicWeight()).isLessThanOrEqualTo(100);
    }

    // ==================== countNeedWarmupInstances 测试 ====================

    /**
     * 测试统计需要预热的实例数
     * 测试目的：验证 countNeedWarmupInstances 正确统计预热中的实例
     * 测试场景：5 个实例中 2 个在预热期，2 个已完成，1 个无 createTime
     * 验证内容：返回 2
     */
    @Test
    public void testCountNeedWarmupInstances() throws Exception {
        // Arrange
        long currentTime = System.currentTimeMillis();
        LosslessProto.Warmup warmup = buildWarmup(true, 120, 2, false, 0);

        List<Instance> instanceList = new ArrayList<>();
        instanceList.add(buildInstance("inst-1", 100, currentTime - 30000));
        instanceList.add(buildInstance("inst-2", 100, currentTime - 60000));
        instanceList.add(buildInstance("inst-3", 100, currentTime - 300000));
        instanceList.add(buildInstance("inst-4", 100, currentTime - 300000));
        instanceList.add(buildInstance("inst-5", 100, null));

        // Act
        Method method = WarmupWeightAdjuster.class.getDeclaredMethod("countNeedWarmupInstances",
                List.class, LosslessProto.Warmup.class, long.class);
        method.setAccessible(true);
        int result = (int) method.invoke(warmupWeightAdjuster, instanceList, warmup, currentTime);

        // Assert
        assertThat(result).isEqualTo(2);
    }

    // ==================== 预热禁用场景 ====================

    /**
     * 测试预热功能关闭时返回原始权重
     * 测试目的：验证 warmup.enable=false 时不做任何调整
     * 测试场景：Warmup 配置 enable=false
     * 验证内容：返回原始权重
     */
    @Test
    public void testWarmupDisabled_ReturnsOriginalWeight() {
        // Arrange
        LosslessProto.Warmup warmup = buildWarmup(false, 120, 2, false, 0);
        LosslessProto.LosslessRule losslessRule = buildLosslessRule(warmup);

        List<Instance> instanceList = new ArrayList<>();
        instanceList.add(buildInstance("inst-1", 100, System.currentTimeMillis() - 30000));
        when(serviceInstances.getInstances()).thenReturn(instanceList);

        Map<String, InstanceWeight> originWeight = new HashMap<>();
        List<LosslessProto.LosslessRule> ruleList = new ArrayList<>();
        ruleList.add(losslessRule);

        // 初始化 metadata 规则缓存
        losslessRuleDictionary.putMetadataLosslessRules(TEST_SERVICE_KEY, ruleList);

        try (MockedStatic<LosslessUtils> mockedUtils = Mockito.mockStatic(LosslessUtils.class)) {
            mockedUtils.when(() -> LosslessUtils.getLosslessRules(eq(extensions), anyString(), anyString()))
                    .thenReturn(ruleList);

            // Act
            Map<String, InstanceWeight> result = warmupWeightAdjuster.timingAdjustDynamicWeight(
                    originWeight, serviceInstances);

            // Assert
            assertThat(result).isEqualTo(originWeight);
        }
    }

    // ==================== 无实例需要预热场景 ====================

    /**
     * 测试所有实例都已完成预热
     * 测试目的：验证所有实例运行时间超过预热区间时返回原始权重
     * 测试场景：所有实例已运行超过 120 秒
     * 验证内容：返回原始权重
     */
    @Test
    public void testAllInstancesWarmupCompleted() {
        // Arrange
        long currentTime = System.currentTimeMillis();
        LosslessProto.Warmup warmup = buildWarmup(true, 120, 2, false, 0);
        LosslessProto.LosslessRule losslessRule = buildLosslessRule(warmup);

        List<Instance> instanceList = new ArrayList<>();
        instanceList.add(buildInstance("inst-1", 100, currentTime - 300000));
        instanceList.add(buildInstance("inst-2", 100, currentTime - 300000));
        when(serviceInstances.getInstances()).thenReturn(instanceList);

        Map<String, InstanceWeight> originWeight = new HashMap<>();
        List<LosslessProto.LosslessRule> ruleList = new ArrayList<>();
        ruleList.add(losslessRule);

        // 初始化 metadata 规则缓存
        losslessRuleDictionary.putMetadataLosslessRules(TEST_SERVICE_KEY, ruleList);

        try (MockedStatic<LosslessUtils> mockedUtils = Mockito.mockStatic(LosslessUtils.class)) {
            mockedUtils.when(() -> LosslessUtils.getLosslessRules(eq(extensions), anyString(), anyString()))
                    .thenReturn(ruleList);

            // Act
            Map<String, InstanceWeight> result = warmupWeightAdjuster.timingAdjustDynamicWeight(
                    originWeight, serviceInstances);

            // Assert
            assertThat(result).isEqualTo(originWeight);
        }
    }

    /**
     * 测试 realTimeAdjustDynamicWeight 返回 false
     * 测试目的：验证实时调整接口返回 false
     * 测试场景：调用 realTimeAdjustDynamicWeight
     * 验证内容：返回 false
     */
    @Test
    public void testRealTimeAdjustDynamicWeight_ReturnsFalse() {
        // Act & Assert
        assertThat(warmupWeightAdjuster.realTimeAdjustDynamicWeight(null)).isFalse();
    }

    /**
     * 测试无 lossless 规则时返回空集合
     * 测试目的：验证无规则时不做任何调整
     * 测试场景：getLosslessRules 返回空列表
     * 验证内容：返回空 map
     */
    @Test
    public void testNoLosslessRules_ReturnsEmptyMap() {
        // Arrange
        List<Instance> instanceList = new ArrayList<>();
        instanceList.add(buildInstance("inst-1", 100, System.currentTimeMillis()));
        when(serviceInstances.getInstances()).thenReturn(instanceList);

        try (MockedStatic<LosslessUtils> mockedUtils = Mockito.mockStatic(LosslessUtils.class)) {
            mockedUtils.when(() -> LosslessUtils.getLosslessRules(eq(extensions), anyString(), anyString()))
                    .thenReturn(Collections.emptyList());

            // Act
            Map<String, InstanceWeight> result = warmupWeightAdjuster.timingAdjustDynamicWeight(
                    new HashMap<>(), serviceInstances);

            // Assert
            assertThat(result).isEmpty();
        }
    }
}
