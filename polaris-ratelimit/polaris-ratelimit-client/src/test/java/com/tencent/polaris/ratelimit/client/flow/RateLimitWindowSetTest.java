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

package com.tencent.polaris.ratelimit.client.flow;

import com.google.protobuf.StringValue;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto.Rule;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link RateLimitWindowSet}：
 * 覆盖 add / cleanup / deleteRules 三方互斥与 H-1/M-3 等不变量。
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class RateLimitWindowSetTest {

    private static final String REVISION = "rev-A";

    @Mock
    private Extensions extensions;

    @Mock
    private RateLimitExtension rateLimitExtension;

    private RateLimitWindowSet windowSet;

    @Before
    public void setUp() {
        windowSet = new RateLimitWindowSet(new ServiceKey("ns", "svc"), extensions, rateLimitExtension, "client-1");
    }

    /**
     * 复现 race：cleanupContainers 的 removeIf 在 predicate 返回 true 与实际删除 entry 之间，
     * 不应允许其它线程通过 windowByRule.computeIfAbsent(sameRevision, ...) 拿到同一个
     * 已被判定为"应淘汰"的 container 并往里塞新窗口——否则新窗口会随 container 一起被丢弃。
     *
     * 测试目的：当 cleanup 决定淘汰 container A 时，
     * 若并发线程通过 computeIfAbsent 在 cleanup 完成前往 A 写入新窗口，
     * 该新窗口在 cleanup 完成后必须仍对外可见（要么仍在 A 中且 A 未被移除，
     * 要么被替换为新 container）。
     * 测试场景：
     * 1. 预置 windowByRule[REVISION] = expiredContainer，其 checkAndCleanExpiredWindows 返回 true
     * 2. 在 checkAndCleanExpiredWindows 被调用时，另一线程 T2 同步往同一 revision 注入新窗口
     * 3. cleanupContainers 完成后，windowByRule[REVISION] 应可被新一轮 getRateLimitWindow 命中
     * BUG：removeIf 在 predicate 之后用 CHM 的 setValue/remove 删除 entry，
     *      但 T2 已经通过 computeIfAbsent 命中并修改了 entry 的 value，
     *      cleanup 的删除会把 T2 的修改也一起抹掉。
     */
    @Test
    public void cleanupContainers_ConcurrentAddOnSameRevision_NewWindowNotLost() throws Exception {
        WindowContainer expiredContainer = new ExplodingExpiredContainer();
        windowSet.getWindowByRule().put(REVISION, expiredContainer);

        WindowContainer freshContainer = new WindowContainer(
                new ServiceKey("ns", "svc"), "labels", freshRateLimitWindow(), true);

        // T1 调用 cleanupContainers；ExplodingExpiredContainer 内部会 await 让 T2 先跑
        CountDownLatch cleanupEntered = ((ExplodingExpiredContainer) expiredContainer).cleanupEntered;
        CountDownLatch t2Done = ((ExplodingExpiredContainer) expiredContainer).t2Done;

        Thread t1 = new Thread(() -> windowSet.cleanupContainers(), "cleanup-thread");
        Thread t2 = new Thread(() -> {
            try {
                // 等 T1 进入 checkAndCleanExpiredWindows
                if (!cleanupEntered.await(2, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("t1 did not enter cleanup in time");
                }
                // 模拟 addRateLimitWindow 的行为：如果拿到的 container 已被标记过期则替换。
                // 该实现必须与 cleanupContainers 在同一 revision 上互斥，否则 T1 删除 entry
                // 会把 T2 新写入的 fresh window 也一并丢失。
                windowSet.getWindowByRule().compute(REVISION, (k, existing) -> {
                    if (existing != null && !existing.isExpired()) {
                        return existing;
                    }
                    return freshContainer;
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                t2Done.countDown();
            }
        }, "add-thread");

        t1.start();
        t2.start();
        t1.join(5000);
        t2.join(5000);

        Map<String, WindowContainer> map = windowSet.getWindowByRule();
        assertThat(map.get(REVISION))
                .as("cleanupContainers 不应在 predicate 判定后跨过 T2 注入的新 container 删除 entry。"
                        + "race window 内 T2 通过 computeIfAbsent 注入了 freshContainer，"
                        + "cleanup 的 removeIf 必须把它当成 \"新状态\" 而非 \"已过期 entry\"。"
                        + "期望 entry 存在并指向 freshContainer，实际 entry 被错误移除。")
                .isSameAs(freshContainer);
    }

    /**
     * WindowContainer.isRegexSpread() 应反映构造时传入的模式。
     * 这是 addRateLimitWindow 在 compute 时检测 regexSpread 不一致 / cleanup / deleteRules 的基础能力。
     */
    @Test
    public void windowContainer_isRegexSpread_ReflectsConstructorMode() {
        WindowContainer mainMode = new WindowContainer(
                new ServiceKey("ns", "svc"), "labels", mock(RateLimitWindow.class), false);
        WindowContainer spreadMode = new WindowContainer(
                new ServiceKey("ns", "svc"), "labels", mock(RateLimitWindow.class), true);

        assertThat(mainMode.isRegexSpread()).as("mainWindow 模式 isRegexSpread 应为 false").isFalse();
        assertThat(spreadMode.isRegexSpread()).as("regexSpread 模式 isRegexSpread 应为 true").isTrue();
    }

    /**
     * deleteRules 与 addRateLimitWindow 的 compute 必须严格互斥。
     *
     * 修复前 deleteRules 用 windowByRule.remove(K)，CHM 互斥保证了 remove 与 compute 不会
     * 同时持锁，但 add 出 compute 后到调 container.getLabelWindow / computeLabelWindow 之间
     * 不再持锁，这段非锁区间 deleteRules 可以抢入：旧 container 已经被删除，但 add 仍把
     * 新 window 写入它（regexSpread 模式），新 window 落在脱链 container 上 → unInit 永远
     * 不会被调用 → 泄漏。
     *
     * 修复方式：
     *   1. addLabelToRevision 已经把 label put 拉进 compute lambda 内（见 Bug 7-续修复）
     *   2. 本测试针对的修复：deleteRules 改用 compute + markExpired + stopSyncTasks，
     *      让 deleteRules 与 add 在同一 bin 锁互斥的同时，留下 expired 标志
     *      —— 任何在 deleteRules 之后再发起的 add 都会通过 existing.isExpired() 检测到，
     *      在 compute lambda 内重新创建 container。
     *
     * 测试目的：deleteRules 后立即 add 同一 revision，新 container 必须被创建（替换旧 container），
     * 旧 container 的 window 必须被 unInit 调用。
     */
    @Test
    public void deleteRules_FollowedByAddOnSameRevision_ShouldReplaceContainerAndUnInitOldWindow() {
        RateLimitWindow oldWindow = mock(RateLimitWindow.class);
        WindowContainer oldContainer = new WindowContainer(
                new ServiceKey("ns", "svc"), "oldLabel", oldWindow, true);
        windowSet.getWindowByRule().put(REVISION, oldContainer);

        windowSet.deleteRules(java.util.Collections.singleton(REVISION));

        // 不变式 1：deleteRules 完成后旧 window 必须被 unInit 调用
        verify(oldWindow).unInit();
        // 不变式 2：oldContainer 应被标记为 expired，下次 add 能识别并替换
        assertThat(oldContainer.isExpired())
                .as("deleteRules 必须 markExpired，让并发 add 通过 existing.isExpired() 替换 container")
                .isTrue();
        // 不变式 3：windowByRule 中该 entry 已被移除
        assertThat(windowSet.getWindowByRule()).doesNotContainKey(REVISION);

        // 紧接着 add 同 revision，应当创建一个全新的 container（不会复用 oldContainer）
        RateLimitWindow newWindow = mock(RateLimitWindow.class);
        windowSet.addLabelToRevision(REVISION, "newLabel", true, label -> newWindow);
        WindowContainer survivor = windowSet.getWindowByRule().get(REVISION);
        assertThat(survivor)
                .as("deleteRules 后再 add 必须创建新 container")
                .isNotNull()
                .isNotSameAs(oldContainer);
        assertThat(survivor.getLabelWindow("newLabel")).isSameAs(newWindow);
    }

    /**
     * H-1：getRateLimitWindow 不能返回已被 cleanup / deleteRules 标记 expired 的 container。
     *
     * 当前 getRateLimitWindow 只判 `windowContainer == null`，就把 container 返给上层；
     * 上层 QuotaFlow.lookupRateLimitWindow 只要拿到非 null window 就用它做 allocateQuota，
     * 不会再走 addRateLimitWindow。一旦 cleanup/deleteRules 已经把 container 内的 window
     * 调过 unInit（status=DELETED）但 entry 还在 map 中（中间态），用户就会拿到 stale window，
     * 用退化后的 bucket 做配额计算，限流不准。
     *
     * 测试目的：windowContainer.isExpired() == true 时 getRateLimitWindow 必须返回 null，
     *           让上层走 addRateLimitWindow 重建 container。
     */
    @Test
    public void getRateLimitWindow_ContainerMarkedExpired_ShouldReturnNull() {
        WindowContainer expiredContainer = new WindowContainer(
                new ServiceKey("ns", "svc"), "labels", mock(RateLimitWindow.class), true);
        expiredContainer.markExpired();
        windowSet.getWindowByRule().put(REVISION, expiredContainer);

        Rule rule = Rule.newBuilder().setRevision(StringValue.of(REVISION)).build();

        RateLimitWindow result = windowSet.getRateLimitWindow(rule, "labels");

        assertThat(result)
                .as("container.isExpired()==true 时 getRateLimitWindow 必须返回 null，"
                        + "否则上层会用已经退化（DELETED 状态）的 window 做配额计算")
                .isNull();
    }

    /**
     * M-3：cleanup 与 deleteRules 必须先 markExpired 再 unInit，
     * 这样当用户线程并发 windowByRule.get 拿到 container 时，立刻 isExpired()==true
     * 就走 H-1 的 null 短路；不会出现"isExpired 还是 false 但 mainWindow 已经 unInit"的中间态。
     *
     * 测试目的：cleanupContainers 触发淘汰一个 mainWindow 模式 container 时，
     *           container.markExpired() 必须在 mainWindow.unInit() 之前发生。
     * 测试方式：spy mainWindow.unInit()，在被调用的瞬间断言 container.isExpired()==true。
     */
    @Test
    public void cleanupContainers_MainWindowMode_MustMarkExpiredBeforeUnInit() {
        // 由 isExpired()==true 触发 checkAndCleanExpiredWindows 走淘汰分支
        RateLimitWindow staleWindow = mock(RateLimitWindow.class);
        when(staleWindow.isExpired()).thenReturn(true);
        WindowContainer container = new WindowContainer(
                new ServiceKey("ns", "svc"), "labels", staleWindow, false);
        windowSet.getWindowByRule().put(REVISION, container);

        // unInit 被调时记录此刻 container 的 expired 状态
        AtomicBoolean expiredAtUnInit = new AtomicBoolean(false);
        doAnswer(invocation -> {
            expiredAtUnInit.set(container.isExpired());
            return null;
        }).when(staleWindow).unInit();

        windowSet.cleanupContainers();

        assertThat(expiredAtUnInit.get())
                .as("cleanup 必须先 markExpired 再 unInit；"
                        + "否则用户线程在 mainWindow.unInit 与 container.markExpired 之间能 get 到"
                        + "isExpired==false 但 mainWindow 已 DELETED 的 container")
                .isTrue();
    }

    /**
     * existing mainWindow 健在时 addLabelToRevision 不应触发 factory，
     * 否则每次 get-miss 都白白构造一个 RateLimitWindow（含 TokenBucket / SlidingWindow / ServiceAddressRepository），
     * 这些对象虽无外部副作用但浪费 CPU 与堆内存。
     */
    @Test
    public void addLabelToRevision_ExistingMainWindow_DoesNotInvokeFactory() {
        RateLimitWindow existingMainWindow = mock(RateLimitWindow.class);
        WindowContainer existing = new WindowContainer(
                new ServiceKey("ns", "svc"), "labels", existingMainWindow, false);
        windowSet.getWindowByRule().put(REVISION, existing);

        AtomicInteger factoryCalls = new AtomicInteger();
        RateLimitWindow result = windowSet.addLabelToRevision(REVISION, "labels", false, label -> {
            factoryCalls.incrementAndGet();
            return mock(RateLimitWindow.class);
        });

        assertThat(result).isSameAs(existingMainWindow);
        assertThat(factoryCalls.get())
                .as("existing mainWindow 健在时不应触发 RateLimitWindow 构造")
                .isZero();
    }

    /**
     * existing regexSpread 模式且 label 已存在时不应触发 factory；
     * 同 1.1.2，computeIfAbsent 不会替换已有 entry，预构造是浪费。
     */
    @Test
    public void addLabelToRevision_ExistingRegexSpreadWithLabel_DoesNotInvokeFactory() {
        RateLimitWindow existingLabelWindow = mock(RateLimitWindow.class);
        WindowContainer existing = new WindowContainer(
                new ServiceKey("ns", "svc"), "label", existingLabelWindow, true);
        windowSet.getWindowByRule().put(REVISION, existing);

        AtomicInteger factoryCalls = new AtomicInteger();
        RateLimitWindow result = windowSet.addLabelToRevision(REVISION, "label", true, label -> {
            factoryCalls.incrementAndGet();
            return mock(RateLimitWindow.class);
        });

        assertThat(result).isSameAs(existingLabelWindow);
        assertThat(factoryCalls.get())
                .as("existing regexSpread + label 已存在时不应触发 RateLimitWindow 构造")
                .isZero();
    }

    /**
     * factory 创建失败时的语义不变量：异常上抛、map 不残留 partial entry、后续 add 仍可工作。
     */
    @Test
    public void addLabelToRevision_FactoryThrows_PropagatesAndKeepsMapClean() {
        // 第一次 add：factory 抛错
        IllegalStateException boom = new IllegalStateException("boom");
        Throwable thrown = catchThrowable(() -> windowSet.addLabelToRevision(
                REVISION, "label", false, label -> { throw boom; }));

        assertThat(thrown)
                .as("factory 抛异常时应当向上传播给 caller")
                .isSameAs(boom);
        assertThat(windowSet.getWindowByRule())
                .as("factory 抛异常后 windowByRule 不应残留 partial entry")
                .doesNotContainKey(REVISION);

        // 第二次 add：用能正常工作的 factory，应当能创建 container
        RateLimitWindow goodWindow = mock(RateLimitWindow.class);
        RateLimitWindow result = windowSet.addLabelToRevision(
                REVISION, "label", false, label -> goodWindow);
        assertThat(result)
                .as("第二次 add（factory 能正常返回）应当能创建 container，bin 锁未被卡住")
                .isSameAs(goodWindow);
        assertThat(windowSet.getWindowByRule().get(REVISION))
                .as("第二次 add 之后 windowByRule[REVISION] 必须可见")
                .isNotNull();
    }

    /**
     * 显式 stub isExpired()=false 的 RateLimitWindow mock。
     * 用 lenient() 是因为当前调用路径不一定会读到，但若未来 cleanupContainers 改为 retry-loop
     * 就会触发；显式声明可表达意图、防止默认行为变更带来的回归。
     */
    private static RateLimitWindow freshRateLimitWindow() {
        RateLimitWindow window = mock(RateLimitWindow.class);
        lenient().when(window.isExpired()).thenReturn(false);
        return window;
    }

    /**
     * 测试用 container：checkAndCleanExpiredWindows 会等到外部触发后才返回 true，
     * 用于精准复现 cleanup 与并发 add 之间的 race window。
     */
    private static class ExplodingExpiredContainer extends WindowContainer {

        final CountDownLatch cleanupEntered = new CountDownLatch(1);
        final CountDownLatch t2Done = new CountDownLatch(1);

        ExplodingExpiredContainer() {
            super(new ServiceKey("ns", "svc"), "labels", mock(RateLimitWindow.class), true);
        }

        @Override
        public boolean checkAndCleanExpiredWindows() {
            // 模拟生产代码：判定过期时立刻打上 expired 标志
            markExpired();
            cleanupEntered.countDown();
            try {
                // 等 T2 跑完 add，再返回 true 让 cleanup 删 entry
                t2Done.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return true;
        }
    }
}
