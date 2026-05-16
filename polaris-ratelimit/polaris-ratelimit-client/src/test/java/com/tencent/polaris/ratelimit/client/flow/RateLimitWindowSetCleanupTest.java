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

import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.pojo.ServiceKey;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link RateLimitWindowSet#cleanupContainers()}。
 * 验证 cleanup 与并发 add 之间不丢失新窗口（race condition regression）。
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class RateLimitWindowSetCleanupTest {

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
        WindowContainer expiredContainer = new ExplodingExpiredContainer(windowSet);
        windowSet.getWindowByRule().put(REVISION, expiredContainer);

        WindowContainer freshContainer = new WindowContainer(
                new ServiceKey("ns", "svc"), "labels", mock(RateLimitWindow.class), true);

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
     * 复现 race：addLabelToRevision 走 compute 拿到 existing 后释放锁，
     * 再调 container.computeLabelWindow 补 label 之前，cleanupContainers 已抢入：
     *   1. cleanup 把 existing container 内最后一个 label 清掉（已过期）
     *   2. windowByLabel.isEmpty() == true → expired.set(true) → entry 被移除
     *   3. add 才把新 window put 进已脱链 container.windowByLabel
     * 结果：新 window 落在没人引用的 container 上，windowByRule.get(revision) 取不到，
     *      新一轮请求会再造一个 container，旧 window 永远泄漏（unInit 不会被调用）。
     *
     * 测试目的：add 写入的 window 必须能通过 windowByRule.get(revision) 取回。
     * 测试场景：
     *   1. 预置 windowByRule[REVISION] = SlowComputeLabelContainer（regexSpread，含一个已过期 label window）
     *   2. T1 调 addLabelToRevision 加 "newLabel"；
     *      在修复前的代码路径下，T1 出 compute 后会调 container.computeLabelWindow，
     *      SlowComputeLabelContainer 在此处 await latch 把 race window 撑开
     *   3. T2 调 cleanupContainers，由于 windowByRule.compute(REVISION, ...) 与 add 互斥，
     *      只能在 add 释放锁后进入；进入时 SlowComputeLabelContainer.windowByLabel 仍为空
     *      （T1 还卡在 computeLabelWindow 的 latch 上），所以 cleanup 会判定 expired 并删除 entry
     *   4. T1 解 latch，把新 window put 进已脱链 container
     * 验证内容：windowByRule[REVISION] 不应为 null，且能取到 newLabel 对应的 window。
     */
    @Test
    public void addLabelToRevision_RaceWithCleanup_NewWindowReachable() throws Exception {
        CountDownLatch t2Done = new CountDownLatch(1);
        // existing：regexSpread 模式，已含一个 label "stale"；slowCompute container 让 T1 在 computeLabelWindow 内 await
        RateLimitWindow staleWindow = mockExpiredWindow(true);
        SlowComputeLabelContainer existing = new SlowComputeLabelContainer(staleWindow, t2Done);
        windowSet.getWindowByRule().put(REVISION, existing);

        RateLimitWindow freshWindow = mockExpiredWindow(false);

        Thread t1 = new Thread(() ->
                windowSet.addLabelToRevision(REVISION, "newLabel", true, label -> freshWindow),
                "add-thread");
        Thread t2 = new Thread(() -> {
            try {
                // 等 T1 进入 computeLabelWindow 的 await
                if (!existing.t1InComputeLabel.await(2, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("t1 did not reach computeLabelWindow in time");
                }
                windowSet.cleanupContainers();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                t2Done.countDown();
            }
        }, "cleanup-thread");

        t1.start();
        t2.start();
        t1.join(5000);
        t2.join(5000);

        WindowContainer survivor = windowSet.getWindowByRule().get(REVISION);
        assertThat(survivor)
                .as("add 与 cleanup 完成后，windowByRule[REVISION] 不应为 null。"
                        + "若为 null，说明 cleanup 在 add 写入新 label 之前就把 container 标记 expired 并移除了，"
                        + "新 window 会落在脱链 container 上无法被取回，导致泄漏。")
                .isNotNull();
        assertThat(survivor.getLabelWindow("newLabel"))
                .as("新 label 的 window 必须能在 windowByRule[REVISION] 上取回。")
                .isSameAs(freshWindow);
    }

    private static RateLimitWindow mockExpiredWindow(boolean expired) {
        RateLimitWindow window = mock(RateLimitWindow.class);
        when(window.isExpired()).thenReturn(expired);
        return window;
    }

    /**
     * 测试用 container：computeLabelWindow 会等到外部触发后才执行，
     * 用于精准复现 add 在 compute 后、computeLabelWindow 前的 race window。
     * 仅在修复前的代码路径上才会被命中（即 T1 出 compute 后调 computeLabelWindow）。
     */
    private static class SlowComputeLabelContainer extends WindowContainer {

        final CountDownLatch t1InComputeLabel = new CountDownLatch(1);
        final CountDownLatch waitForT2;

        SlowComputeLabelContainer(RateLimitWindow seed, CountDownLatch waitForT2) {
            super(new ServiceKey("ns", "svc"), "stale", seed, true);
            this.waitForT2 = waitForT2;
        }

        @Override
        public RateLimitWindow computeLabelWindow(String label, java.util.function.Function<String, RateLimitWindow> function) {
            t1InComputeLabel.countDown();
            try {
                waitForT2.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return super.computeLabelWindow(label, function);
        }
    }

    /**
     * 测试用 container：checkAndCleanExpiredWindows 会等到外部触发后才返回 true，
     * 用于精准复现 cleanup 与并发 add 之间的 race window。
     */
    private static class ExplodingExpiredContainer extends WindowContainer {

        final CountDownLatch cleanupEntered = new CountDownLatch(1);
        final CountDownLatch t2Done = new CountDownLatch(1);

        ExplodingExpiredContainer(RateLimitWindowSet ignored) {
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
