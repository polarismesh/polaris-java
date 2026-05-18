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
import com.tencent.polaris.client.pojo.Node;
import com.tencent.polaris.client.remote.ServiceAddressRepository;
import com.tencent.polaris.ratelimit.client.sync.RemoteSyncTask;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link RateLimitExtension#stopSyncTask}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class RateLimitExtensionStopSyncTaskTest {

    private static final String UNIQUE_KEY = "rev#svc#ns#labels";

    @Mock
    private Extensions extensions;

    @Mock
    private RateLimitWindow window;

    @Mock
    private RateLimitWindowSet windowSet;

    @Mock
    private ServiceAddressRepository serviceAddressRepository;

    private ScheduledThreadPoolExecutor syncExecutor;
    private ScheduledThreadPoolExecutor expireExecutor;
    private RateLimitExtension rateLimitExtension;
    private AsyncRateLimitConnector connector;

    @Before
    public void setUp() {
        syncExecutor = new ScheduledThreadPoolExecutor(1);
        expireExecutor = new ScheduledThreadPoolExecutor(1);
        rateLimitExtension = new RateLimitExtension(extensions, syncExecutor, expireExecutor);
        connector = new AsyncRateLimitConnector();

        when(window.getWindowSet()).thenReturn(windowSet);
        when(window.getSvcKey()).thenReturn(new ServiceKey("ns", "svc"));
        when(window.getLabels()).thenReturn("labels");
        when(window.getUniqueKey()).thenReturn(UNIQUE_KEY);
        // stream 已回收时这些字段不应被读到
        lenient().when(window.getRemoteCluster()).thenReturn(new ServiceKey("ns", "limiter"));
        lenient().when(window.getServiceAddressRepository()).thenReturn(serviceAddressRepository);
        when(windowSet.getAsyncRateLimitConnector()).thenReturn(connector);
        lenient().when(windowSet.getRateLimitExtension()).thenReturn(rateLimitExtension);
    }

    @After
    public void tearDown() {
        if (rateLimitExtension != null) {
            rateLimitExtension.destroy();
        }
    }

    /**
     * 测试目的：stream 已被回收时，stopSyncTask 不应通过 getStreamCounterSet 触发新建连。
     * 测试场景：connector 为空，调用 stopSyncTask。
     * 验证内容：scheduledTasks 已清理；nodeToStream / uniqueKeyToStream 保持为空。
     */
    @Test
    public void stopSyncTask_NoExistingStream_ShouldNotCreateStream() throws Exception {
        lenient().when(serviceAddressRepository.getServiceAddressNode()).thenReturn(new Node("limiter-host", 8081));

        assertThat(connector.getNodeToStream()).isEmpty();
        assertThat(connector.getUniqueKeyToStream()).isEmpty();

        rateLimitExtension.stopSyncTask(UNIQUE_KEY, window);

        // scheduledTasks 由 stopSyncTask 同步清理，connector 由 10ms 延迟的 cleanTask 处理；
        // 用轮询取代固定 sleep，避免慢机器误判 + 快机器空等
        awaitTrue(() -> !rateLimitExtension.getScheduledTasks().containsKey(UNIQUE_KEY)
                && connector.getNodeToStream().isEmpty()
                && connector.getUniqueKeyToStream().isEmpty(), 2000);

        assertThat(rateLimitExtension.getScheduledTasks()).doesNotContainKey(UNIQUE_KEY);
        assertThat(connector.getNodeToStream()).isEmpty();
        assertThat(connector.getUniqueKeyToStream()).isEmpty();
    }

    private static void awaitTrue(java.util.function.BooleanSupplier cond, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (cond.getAsBoolean()) {
                return;
            }
            Thread.sleep(20);
        }
    }

    /**
     * H-2：submitSyncTask 检测到 scheduledTasks 已有 entry 时，
     * 不应擅自把 window 的 status 设回 CREATED——这会破坏 init 状态机：
     *   - 远端 init 响应到达后 handleRateLimitInitResponse 只允许 INITIALIZING→INITIALIZED；
     *   - 一旦 status 被改成 CREATED，响应到达后无法切到 INITIALIZED，长期降级。
     * 也不应再调度新的 future——computeIfAbsent 守住"判定 + schedule + put" 原子。
     */
    @Test
    public void submitSyncTask_DuplicateEntry_DoesNotResetWindowStatusOrScheduleNew() {
        // 用 mock executor 替换 RateLimitExtension 内部的真实 executor，便于断言 schedule 调用
        ScheduledExecutorService mockSyncExecutor = mock(ScheduledExecutorService.class);
        ScheduledExecutorService mockExpireExecutor = mock(ScheduledExecutorService.class);
        RateLimitExtension extWithMockExec = new RateLimitExtension(extensions, mockSyncExecutor, mockExpireExecutor);
        try {
            // 预置 scheduledTasks 中已存在 UNIQUE_KEY 的 entry
            ScheduledFuture<?> stale = mock(ScheduledFuture.class);
            extWithMockExec.getScheduledTasks().put(UNIQUE_KEY, stale);

            RemoteSyncTask task = mock(RemoteSyncTask.class);
            when(task.getWindow()).thenReturn(window);

            extWithMockExec.submitSyncTask(task, 0L, 1000L);

            verify(window, never()).setStatus(anyInt());
            verify(mockSyncExecutor, never()).scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(),
                    any(TimeUnit.class));
            assertThat((Object) extWithMockExec.getScheduledTasks().get(UNIQUE_KEY))
                    .as("重入路径不应覆盖已存在的 future")
                    .isSameAs(stale);
        } finally {
            extWithMockExec.destroy();
        }
    }
}
