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

package com.tencent.polaris.ratelimit.test.core;

import com.google.protobuf.Duration;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt32Value;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.client.util.Utils;
import com.tencent.polaris.ratelimit.api.core.LimitAPI;
import com.tencent.polaris.ratelimit.api.flow.LimitFlow;
import com.tencent.polaris.ratelimit.api.rpc.Argument;
import com.tencent.polaris.ratelimit.api.rpc.QuotaRequest;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResponse;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResultCode;
import com.tencent.polaris.ratelimit.client.api.DefaultLimitAPI;
import com.tencent.polaris.ratelimit.client.flow.DefaultLimitFlow;
import com.tencent.polaris.ratelimit.client.flow.QuotaFlow;
import com.tencent.polaris.ratelimit.client.flow.RateLimitWindow;
import com.tencent.polaris.ratelimit.client.flow.RateLimitWindow.WindowStatus;
import com.tencent.polaris.ratelimit.client.flow.RateLimitWindowSet;
import com.tencent.polaris.ratelimit.client.flow.WindowContainer;
import com.tencent.polaris.ratelimit.factory.LimitAPIFactory;
import com.tencent.polaris.specification.api.v1.model.ModelProto.MatchString;
import com.tencent.polaris.specification.api.v1.model.ModelProto.MatchString.MatchStringType;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto.Amount;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto.MatchArgument;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto.RateLimit;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto.Rule;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto.Rule.AmountMode;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto.Rule.Type;
import com.tencent.polaris.test.common.TestUtils;
import com.tencent.polaris.test.mock.discovery.NamingServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 集成测试：验证限流窗口过期和恢复机制
 * <p>
 * 测试完整链路：QuotaFlow → RateLimitWindowSet → WindowContainer → RateLimitWindow
 * 涵盖窗口创建、过期检测、清理以及重新创建恢复的端到端流程
 *
 * @author Haotian Zhang
 */
public class WindowExpireAndRecoverTest {

    private static int PORT;

    private static final String EXPIRE_TEST_SERVICE = "java_expire_test_service";

    /**
     * 限流规则：1秒内最多2个请求（小的限流值便于快速触发限流）
     * validDuration=1s → expireDurationMs = 1*1000 + 1000(EXPIRE_FACTOR_MS) = 2000ms
     */
    private static final int MAX_AMOUNT = 2;

    private static final int VALID_DURATION_SECONDS = 1;

    /**
     * 窗口过期时间 = MaxDuration(1s) + EXPIRE_FACTOR_MS(1000ms) = 2000ms
     */
    private static final long EXPECTED_EXPIRE_DURATION_MS = VALID_DURATION_SECONDS * 1000L + 1000L;

    private NamingServer namingServer;

    @Before
    public void setUp() {
        try {
            namingServer = NamingServer.startNamingServer(-1);
            PORT = namingServer.getPort();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ServiceKey serviceKey = new ServiceKey(Consts.NAMESPACE_TEST, EXPIRE_TEST_SERVICE);
        namingServer.getNamingService().addService(serviceKey);

        // 构建限流规则：local 模式，1秒2个请求
        RateLimit.Builder rateLimitBuilder = RateLimit.newBuilder();
        Rule.Builder ruleBuilder = Rule.newBuilder();
        ruleBuilder.setType(Type.LOCAL);
        ruleBuilder.setPriority(UInt32Value.newBuilder().setValue(0).build());
        ruleBuilder.setAction(StringValue.newBuilder().setValue("reject").build());
        ruleBuilder.setAmountMode(AmountMode.GLOBAL_TOTAL);
        ruleBuilder.addArguments(
                MatchArgument.newBuilder()
                        .setType(MatchArgument.Type.CUSTOM)
                        .setKey(Consts.LABEL_METHOD)
                        .setValue(MatchString.newBuilder()
                                .setType(MatchStringType.EXACT)
                                .setValue(StringValue.newBuilder().setValue(Consts.METHOD_PAY).build())
                                .build()));
        ruleBuilder.addAmounts(
                Amount.newBuilder()
                        .setMaxAmount(UInt32Value.newBuilder().setValue(MAX_AMOUNT).build())
                        .setValidDuration(Duration.newBuilder().setSeconds(VALID_DURATION_SECONDS).build()));
        ruleBuilder.setRevision(StringValue.newBuilder().setValue("expire-test-rev-001").build());
        rateLimitBuilder.addRules(ruleBuilder.build());
        rateLimitBuilder.setRevision(StringValue.newBuilder().setValue("expire-test-global-rev").build());
        namingServer.getNamingService().setRateLimit(serviceKey, rateLimitBuilder.build());
    }

    @After
    public void tearDown() {
        if (null != namingServer) {
            namingServer.terminate();
        }
    }

    /**
     * 测试窗口过期后配额恢复
     * 测试目的：验证限流窗口在长时间不使用后过期被清理，再次请求时创建新窗口并恢复配额
     * 测试场景：
     *   1. 发起请求消耗配额直到被限流
     *   2. 通过反射修改 lastAccessTimeMs 模拟过期
     *   3. 手动触发过期清理
     *   4. 再次发起请求，验证配额已恢复
     * 验证内容：
     *   1. 首次请求后窗口正常创建，状态为 INITIALIZED
     *   2. 过期清理后窗口被移除
     *   3. 再次请求后新窗口被创建，配额正常可用
     */
    @Test
    public void testWindowExpireAndRecover() throws Exception {
        Configuration configuration = TestUtils.createSimpleConfiguration(PORT);
        try (LimitAPI limitAPI = LimitAPIFactory.createLimitAPIByConfig(configuration)) {
            RateLimitUtils.adjustTime();

            // === 阶段1：首次请求，消耗配额并验证限流生效 ===
            boolean hasLimited = false;
            boolean hasPassed = false;
            for (int i = 0; i < MAX_AMOUNT + 2; i++) {
                QuotaResponse response = acquireQuota(limitAPI, Consts.METHOD_PAY);
                if (response.getCode() == QuotaResultCode.QuotaResultOk) {
                    hasPassed = true;
                } else if (response.getCode() == QuotaResultCode.QuotaResultLimited) {
                    hasLimited = true;
                }
            }
            assertThat(hasPassed).as("应该有请求通过").isTrue();
            assertThat(hasLimited).as("应该有请求被限流").isTrue();

            // === 阶段2：获取内部的窗口对象，验证窗口已创建 ===
            QuotaFlow quotaFlow = extractQuotaFlow(limitAPI);
            Map<ServiceKey, RateLimitWindowSet> svcToWindowSet = getPrivateField(quotaFlow, "svcToWindowSet");
            ServiceKey serviceKey = new ServiceKey(Consts.NAMESPACE_TEST, EXPIRE_TEST_SERVICE);
            RateLimitWindowSet windowSet = svcToWindowSet.get(serviceKey);
            assertThat(windowSet).as("窗口集合应该存在").isNotNull();

            Map<String, WindowContainer> windowByRule = getPrivateField(windowSet, "windowByRule");
            assertThat(windowByRule).as("规则窗口容器应该存在").isNotEmpty();

            // 获取第一个窗口容器中的 mainWindow
            WindowContainer container = windowByRule.values().iterator().next();
            RateLimitWindow mainWindow = container.getMainWindow();
            assertThat(mainWindow).as("限流窗口应该已创建").isNotNull();
            assertThat(mainWindow.getStatus()).as("窗口状态应为 INITIALIZED").isEqualTo(WindowStatus.INITIALIZED);

            // === 阶段3：通过反射修改 lastAccessTimeMs，模拟窗口过期 ===
            AtomicLong lastAccessTimeMs = getPrivateField(mainWindow, "lastAccessTimeMs");
            long originalAccessTime = lastAccessTimeMs.get();
            // 将 lastAccessTimeMs 设置为很久以前，使得 currentTime - lastAccessTimeMs > expireDurationMs
            lastAccessTimeMs.set(System.currentTimeMillis() - EXPECTED_EXPIRE_DURATION_MS - 5000);

            // 验证 isExpired 返回 true
            assertThat(mainWindow.isExpired()).as("修改访问时间后窗口应该过期").isTrue();

            // === 阶段4：手动触发过期清理 ===
            windowSet.cleanupContainers();

            // 验证窗口容器已被清理
            assertThat(windowByRule).as("过期窗口容器应该被移除").isEmpty();
            assertThat(mainWindow.getStatus()).as("窗口状态应变为 DELETED").isEqualTo(WindowStatus.DELETED);

            // === 阶段5：再次请求，验证新窗口被创建且配额恢复 ===
            RateLimitUtils.adjustTime();
            hasPassed = false;
            for (int i = 0; i < MAX_AMOUNT; i++) {
                QuotaResponse response = acquireQuota(limitAPI, Consts.METHOD_PAY);
                if (response.getCode() == QuotaResultCode.QuotaResultOk) {
                    hasPassed = true;
                }
            }
            assertThat(hasPassed).as("过期清理后再次请求应该通过（配额恢复）").isTrue();

            // 验证新的窗口已被创建
            assertThat(windowByRule).as("应该创建了新的窗口容器").isNotEmpty();
            WindowContainer newContainer = windowByRule.values().iterator().next();
            RateLimitWindow newWindow = newContainer.getMainWindow();
            assertThat(newWindow).as("新限流窗口应该存在").isNotNull();
            assertThat(newWindow).as("新窗口应该是不同的实例").isNotSameAs(mainWindow);
            assertThat(newWindow.getStatus()).as("新窗口状态应为 INITIALIZED").isEqualTo(WindowStatus.INITIALIZED);

            // 等待内部线程完成
            Utils.sleepUninterrupted(2000);
        }
    }

    /**
     * 测试窗口未过期时不应被清理
     * 测试目的：验证最近刚访问过的窗口不会被误清理
     * 测试场景：发起请求后立即执行过期清理
     * 验证内容：
     *   1. 窗口仍然存在于容器中
     *   2. 窗口状态保持 INITIALIZED
     *   3. isExpired 返回 false
     */
    @Test
    public void testWindowNotExpiredShouldNotBeCleanedUp() throws Exception {
        Configuration configuration = TestUtils.createSimpleConfiguration(PORT);
        try (LimitAPI limitAPI = LimitAPIFactory.createLimitAPIByConfig(configuration)) {
            RateLimitUtils.adjustTime();

            // 发起请求，创建窗口
            QuotaResponse response = acquireQuota(limitAPI, Consts.METHOD_PAY);
            assertThat(response.getCode()).as("首次请求应该通过").isEqualTo(QuotaResultCode.QuotaResultOk);

            // 获取内部窗口对象
            QuotaFlow quotaFlow = extractQuotaFlow(limitAPI);
            Map<ServiceKey, RateLimitWindowSet> svcToWindowSet = getPrivateField(quotaFlow, "svcToWindowSet");
            ServiceKey serviceKey = new ServiceKey(Consts.NAMESPACE_TEST, EXPIRE_TEST_SERVICE);
            RateLimitWindowSet windowSet = svcToWindowSet.get(serviceKey);
            Map<String, WindowContainer> windowByRule = getPrivateField(windowSet, "windowByRule");
            WindowContainer container = windowByRule.values().iterator().next();
            RateLimitWindow mainWindow = container.getMainWindow();

            // 验证窗口未过期
            assertThat(mainWindow.isExpired()).as("刚访问过的窗口不应过期").isFalse();

            // 执行过期清理
            windowSet.cleanupContainers();

            // 验证窗口仍然存在
            assertThat(windowByRule).as("窗口容器不应被移除").isNotEmpty();
            assertThat(mainWindow.getStatus()).as("窗口状态应保持 INITIALIZED").isEqualTo(WindowStatus.INITIALIZED);

            Utils.sleepUninterrupted(2000);
        }
    }

    /**
     * 测试窗口过期后再次限流恢复正常
     * 测试目的：验证过期恢复后新窗口的限流逻辑完整可用
     * 测试场景：
     *   1. 消耗全部配额至限流
     *   2. 模拟过期并清理
     *   3. 重新请求验证限流恢复并再次触发限流
     * 验证内容：新窗口的限流规则与原窗口一致（MAX_AMOUNT 个请求通过后触发限流）
     */
    @Test
    public void testExpiredWindowRecoverAndRelimit() throws Exception {
        Configuration configuration = TestUtils.createSimpleConfiguration(PORT);
        try (LimitAPI limitAPI = LimitAPIFactory.createLimitAPIByConfig(configuration)) {
            RateLimitUtils.adjustTime();

            // 阶段1：消耗配额
            for (int i = 0; i < MAX_AMOUNT + 2; i++) {
                acquireQuota(limitAPI, Consts.METHOD_PAY);
            }

            // 阶段2：获取窗口并模拟过期
            QuotaFlow quotaFlow = extractQuotaFlow(limitAPI);
            Map<ServiceKey, RateLimitWindowSet> svcToWindowSet = getPrivateField(quotaFlow, "svcToWindowSet");
            ServiceKey serviceKey = new ServiceKey(Consts.NAMESPACE_TEST, EXPIRE_TEST_SERVICE);
            RateLimitWindowSet windowSet = svcToWindowSet.get(serviceKey);
            Map<String, WindowContainer> windowByRule = getPrivateField(windowSet, "windowByRule");
            WindowContainer container = windowByRule.values().iterator().next();
            RateLimitWindow mainWindow = container.getMainWindow();

            // 模拟过期
            AtomicLong lastAccessTimeMs = getPrivateField(mainWindow, "lastAccessTimeMs");
            lastAccessTimeMs.set(System.currentTimeMillis() - EXPECTED_EXPIRE_DURATION_MS - 5000);
            windowSet.cleanupContainers();
            assertThat(windowByRule).as("窗口容器应被清理").isEmpty();

            // 阶段3：等待新的时间窗口开始
            RateLimitUtils.adjustTime();

            // 阶段4：再次发起请求，验证限流规则恢复
            boolean hasPassed = false;
            boolean hasLimited = false;
            for (int i = 0; i < MAX_AMOUNT + 2; i++) {
                QuotaResponse response = acquireQuota(limitAPI, Consts.METHOD_PAY);
                if (response.getCode() == QuotaResultCode.QuotaResultOk) {
                    hasPassed = true;
                } else if (response.getCode() == QuotaResultCode.QuotaResultLimited) {
                    hasLimited = true;
                }
            }
            assertThat(hasPassed).as("恢复后应该有请求通过").isTrue();
            assertThat(hasLimited).as("恢复后限流规则应该生效，请求应被限流").isTrue();

            Utils.sleepUninterrupted(2000);
        }
    }

    /**
     * 测试多次过期恢复循环
     * 测试目的：验证窗口可以多次经历过期-恢复循环，每次都能正常工作
     * 测试场景：执行3轮过期-恢复循环
     * 验证内容：每轮恢复后配额均正常可用
     */
    @Test
    public void testMultipleExpireRecoverCycles() throws Exception {
        Configuration configuration = TestUtils.createSimpleConfiguration(PORT);
        try (LimitAPI limitAPI = LimitAPIFactory.createLimitAPIByConfig(configuration)) {
            QuotaFlow quotaFlow = extractQuotaFlow(limitAPI);
            Map<ServiceKey, RateLimitWindowSet> svcToWindowSet = getPrivateField(quotaFlow, "svcToWindowSet");
            ServiceKey serviceKey = new ServiceKey(Consts.NAMESPACE_TEST, EXPIRE_TEST_SERVICE);

            for (int cycle = 0; cycle < 3; cycle++) {
                RateLimitUtils.adjustTime();

                // 发起请求创建/使用窗口
                boolean hasPassed = false;
                boolean hasLimited = false;
                for (int i = 0; i < MAX_AMOUNT + 2; i++) {
                    QuotaResponse response = acquireQuota(limitAPI, Consts.METHOD_PAY);
                    if (response.getCode() == QuotaResultCode.QuotaResultOk) {
                        hasPassed = true;
                    } else if (response.getCode() == QuotaResultCode.QuotaResultLimited) {
                        hasLimited = true;
                    }
                }
                assertThat(hasPassed).as("第 %d 轮：应该有请求通过", cycle + 1).isTrue();
                assertThat(hasLimited).as("第 %d 轮：应该有请求被限流", cycle + 1).isTrue();

                // 模拟过期
                RateLimitWindowSet windowSet = svcToWindowSet.get(serviceKey);
                assertThat(windowSet).as("第 %d 轮：窗口集合应该存在", cycle + 1).isNotNull();
                Map<String, WindowContainer> windowByRule = getPrivateField(windowSet, "windowByRule");
                WindowContainer container = windowByRule.values().iterator().next();
                RateLimitWindow window = container.getMainWindow();
                AtomicLong lastAccessTimeMs = getPrivateField(window, "lastAccessTimeMs");
                lastAccessTimeMs.set(System.currentTimeMillis() - EXPECTED_EXPIRE_DURATION_MS - 5000);

                // 清理
                windowSet.cleanupContainers();
                assertThat(windowByRule).as("第 %d 轮：窗口容器应被清理", cycle + 1).isEmpty();
            }

            Utils.sleepUninterrupted(2000);
        }
    }

    /**
     * 测试并发请求与过期清理同时进行时的线程安全性
     * 测试目的：验证多个请求线程并发调用 getQuota() 的同时，过期清理线程执行 cleanupContainers()，
     *          不会抛出 ConcurrentModificationException 或其他并发异常
     * 测试场景：
     *   1. 创建窗口并消耗配额
     *   2. 通过反射模拟窗口过期
     *   3. 启动多个请求线程并发调用 getQuota()，同时另一个线程执行 cleanupContainers()
     *   4. 等待所有线程完成
     * 验证内容：
     *   1. 没有线程抛出异常
     *   2. 所有请求线程都成功返回了结果（QuotaResultOk 或 QuotaResultLimited）
     *   3. 执行完毕后窗口状态一致
     */
    @Test
    public void testConcurrentRequestsWithExpireCleanup() throws Exception {
        Configuration configuration = TestUtils.createSimpleConfiguration(PORT);
        try (LimitAPI limitAPI = LimitAPIFactory.createLimitAPIByConfig(configuration)) {
            RateLimitUtils.adjustTime();

            // 阶段1：创建窗口
            QuotaResponse initialResponse = acquireQuota(limitAPI, Consts.METHOD_PAY);
            assertThat(initialResponse.getCode()).as("首次请求应通过").isEqualTo(QuotaResultCode.QuotaResultOk);

            // 获取内部对象
            QuotaFlow quotaFlow = extractQuotaFlow(limitAPI);
            Map<ServiceKey, RateLimitWindowSet> svcToWindowSet = getPrivateField(quotaFlow, "svcToWindowSet");
            ServiceKey serviceKey = new ServiceKey(Consts.NAMESPACE_TEST, EXPIRE_TEST_SERVICE);
            RateLimitWindowSet windowSet = svcToWindowSet.get(serviceKey);
            Map<String, WindowContainer> windowByRule = getPrivateField(windowSet, "windowByRule");
            WindowContainer container = windowByRule.values().iterator().next();
            RateLimitWindow mainWindow = container.getMainWindow();

            // 阶段2：模拟窗口过期
            AtomicLong lastAccessTimeMs = getPrivateField(mainWindow, "lastAccessTimeMs");
            lastAccessTimeMs.set(System.currentTimeMillis() - EXPECTED_EXPIRE_DURATION_MS - 5000);

            // 阶段3：并发执行请求和清理
            int requestThreadCount = 10;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(requestThreadCount + 1);
            AtomicInteger exceptionCount = new AtomicInteger(0);
            AtomicInteger successResponseCount = new AtomicInteger(0);
            List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<>());

            // 启动请求线程
            for (int i = 0; i < requestThreadCount; i++) {
                new Thread(() -> {
                    try {
                        startLatch.await();
                        QuotaResponse response = acquireQuota(limitAPI, Consts.METHOD_PAY);
                        if (response.getCode() == QuotaResultCode.QuotaResultOk
                                || response.getCode() == QuotaResultCode.QuotaResultLimited) {
                            successResponseCount.incrementAndGet();
                        }
                    } catch (Throwable t) {
                        exceptionCount.incrementAndGet();
                        exceptions.add(t);
                    } finally {
                        doneLatch.countDown();
                    }
                }, "request-thread-" + i).start();
            }

            // 启动清理线程
            new Thread(() -> {
                try {
                    startLatch.await();
                    windowSet.cleanupContainers();
                } catch (Throwable t) {
                    exceptionCount.incrementAndGet();
                    exceptions.add(t);
                } finally {
                    doneLatch.countDown();
                }
            }, "cleanup-thread").start();

            // 同时释放所有线程
            startLatch.countDown();
            boolean allDone = doneLatch.await(10, TimeUnit.SECONDS);

            // 验证
            assertThat(allDone).as("所有线程应在超时前完成").isTrue();
            assertThat(exceptionCount.get())
                    .as("不应有线程抛出异常，但发现异常: %s",
                            exceptions.isEmpty() ? "无" : exceptions.get(0).toString())
                    .isEqualTo(0);
            assertThat(successResponseCount.get())
                    .as("所有请求线程应成功返回结果").isEqualTo(requestThreadCount);

            Utils.sleepUninterrupted(2000);
        }
    }

    /**
     * 测试过期清理后多线程并发恢复窗口
     * 测试目的：验证窗口过期清理后，多个线程同时发起请求时，通过 ConcurrentHashMap.computeIfAbsent
     *          只创建一个新窗口实例，且所有线程都能正常获取到配额结果
     * 测试场景：
     *   1. 创建窗口并模拟过期，执行清理
     *   2. 确认窗口已被清理
     *   3. 多线程同时发起请求，触发新窗口创建
     * 验证内容：
     *   1. 所有线程都成功获取到配额结果，无异常
     *   2. 恢复后只创建了一个窗口容器（computeIfAbsent 的原子性保证）
     *   3. 所有请求线程中至少有一些通过了限流（配额恢复有效）
     */
    @Test
    public void testConcurrentRecoverAfterExpire() throws Exception {
        Configuration configuration = TestUtils.createSimpleConfiguration(PORT);
        try (LimitAPI limitAPI = LimitAPIFactory.createLimitAPIByConfig(configuration)) {
            RateLimitUtils.adjustTime();

            // 阶段1：创建窗口并消耗配额
            for (int i = 0; i < MAX_AMOUNT + 1; i++) {
                acquireQuota(limitAPI, Consts.METHOD_PAY);
            }

            // 获取内部对象
            QuotaFlow quotaFlow = extractQuotaFlow(limitAPI);
            Map<ServiceKey, RateLimitWindowSet> svcToWindowSet = getPrivateField(quotaFlow, "svcToWindowSet");
            ServiceKey serviceKey = new ServiceKey(Consts.NAMESPACE_TEST, EXPIRE_TEST_SERVICE);
            RateLimitWindowSet windowSet = svcToWindowSet.get(serviceKey);
            Map<String, WindowContainer> windowByRule = getPrivateField(windowSet, "windowByRule");

            // 阶段2：模拟过期并清理
            WindowContainer container = windowByRule.values().iterator().next();
            RateLimitWindow oldWindow = container.getMainWindow();
            AtomicLong lastAccessTimeMs = getPrivateField(oldWindow, "lastAccessTimeMs");
            lastAccessTimeMs.set(System.currentTimeMillis() - EXPECTED_EXPIRE_DURATION_MS - 5000);
            windowSet.cleanupContainers();
            assertThat(windowByRule).as("窗口容器应已被清理").isEmpty();

            // 阶段3：等待新的时间窗口开始
            RateLimitUtils.adjustTime();

            // 阶段4：多线程并发发起请求，触发新窗口创建
            int threadCount = 10;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            AtomicInteger exceptionCount = new AtomicInteger(0);
            AtomicInteger passedCount = new AtomicInteger(0);
            AtomicInteger limitedCount = new AtomicInteger(0);
            List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<>());

            for (int i = 0; i < threadCount; i++) {
                new Thread(() -> {
                    try {
                        startLatch.await();
                        QuotaResponse response = acquireQuota(limitAPI, Consts.METHOD_PAY);
                        if (response.getCode() == QuotaResultCode.QuotaResultOk) {
                            passedCount.incrementAndGet();
                        } else if (response.getCode() == QuotaResultCode.QuotaResultLimited) {
                            limitedCount.incrementAndGet();
                        }
                    } catch (Throwable t) {
                        exceptionCount.incrementAndGet();
                        exceptions.add(t);
                    } finally {
                        doneLatch.countDown();
                    }
                }, "recover-thread-" + i).start();
            }

            // 同时释放所有线程
            startLatch.countDown();
            boolean allDone = doneLatch.await(10, TimeUnit.SECONDS);

            // 验证
            assertThat(allDone).as("所有线程应在超时前完成").isTrue();
            assertThat(exceptionCount.get())
                    .as("不应有线程抛出异常，但发现异常: %s",
                            exceptions.isEmpty() ? "无" : exceptions.get(0).toString())
                    .isEqualTo(0);
            assertThat(passedCount.get() + limitedCount.get())
                    .as("所有线程应成功获取到配额结果").isEqualTo(threadCount);
            assertThat(passedCount.get())
                    .as("恢复后应有请求通过（配额已恢复）").isGreaterThan(0);

            // 验证只创建了一个窗口容器
            assertThat(windowByRule).as("应只创建一个窗口容器").hasSize(1);
            WindowContainer newContainer = windowByRule.values().iterator().next();
            RateLimitWindow newWindow = newContainer.getMainWindow();
            assertThat(newWindow).as("新限流窗口应存在").isNotNull();
            assertThat(newWindow).as("新窗口应与旧窗口不同").isNotSameAs(oldWindow);

            Utils.sleepUninterrupted(2000);
        }
    }

    /**
     * 发起配额请求
     */
    private QuotaResponse acquireQuota(LimitAPI limitAPI, String method) {
        QuotaRequest request = new QuotaRequest();
        request.setNamespace(Consts.NAMESPACE_TEST);
        request.setService(EXPIRE_TEST_SERVICE);
        Set<Argument> arguments = new HashSet<>();
        arguments.add(Argument.buildCustom(Consts.LABEL_METHOD, method));
        request.setArguments(arguments);
        return limitAPI.getQuota(request);
    }

    /**
     * 通过反射从 LimitAPI 中提取 QuotaFlow 对象
     * 调用链：DefaultLimitAPI → limitFlow(DefaultLimitFlow) → quotaFlow(QuotaFlow)
     */
    private QuotaFlow extractQuotaFlow(LimitAPI limitAPI) throws Exception {
        DefaultLimitAPI defaultLimitAPI = (DefaultLimitAPI) limitAPI;
        LimitFlow limitFlow = getPrivateField(defaultLimitAPI, "limitFlow");
        DefaultLimitFlow defaultLimitFlow = (DefaultLimitFlow) limitFlow;
        return getPrivateField(defaultLimitFlow, "quotaFlow");
    }

    @SuppressWarnings("unchecked")
    private static <T> T getPrivateField(Object object, String fieldName)
            throws NoSuchFieldException, IllegalAccessException {
        Class<?> clazz = object.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return (T) field.get(object);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}
