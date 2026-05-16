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

import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.ratelimit.client.flow.RateLimitExtension;
import com.tencent.polaris.ratelimit.client.flow.RateLimitWindow;
import com.tencent.polaris.ratelimit.client.sync.RemoteSyncTask;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

/**
 * Test for {@link RateLimitExtension}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class SubmitPolarisRemoteSyncTaskTest {

    @Mock
    private ScheduledExecutorService syncExecutor;

    @Mock
    private ScheduledExecutorService windowExpireExecutor;

    @Mock
    private ScheduledFuture<?> mockFuture;

    @Mock
    private RemoteSyncTask remoteSyncTask;

    @Mock
    private RateLimitWindow rateLimitWindow;

    private SDKContext context;

    private RateLimitExtension rateLimitExtension;

    private Map<String, ScheduledFuture<?>> scheduledTasks;

    @Before
    public void setUp() throws Exception {
        ConfigurationImpl config = new ConfigurationImpl();
        config.setDefault();
        config.getGlobal().getAPI().setReportEnable(false);
        config.getGlobal().getStatReporter().setEnable(false);
        context = SDKContext.initContextByConfig(config);
        context.init();
        Extensions extensions = context.getExtensions();
        rateLimitExtension = new RateLimitExtension(extensions);

        // 使用反射替换 syncExecutor，避免真实线程池执行
        setPrivateField(rateLimitExtension, "syncExecutor", syncExecutor);

        // 使用反射替换 windowExpireExecutor，避免真实线程池执行
        setPrivateField(rateLimitExtension, "windowExpireExecutor", windowExpireExecutor);

        // 使用反射获取 scheduledTasks
        scheduledTasks = getPrivateField(rateLimitExtension, "scheduledTasks");

        // Mock 限流窗口
        when(remoteSyncTask.getWindow()).thenReturn(rateLimitWindow);
        when(rateLimitWindow.getUniqueKey()).thenReturn("test-unique-key");
    }

    @After
    public void tearDown() throws Exception {
        context.destroy();
    }

    /**
     * 测试提交同步任务 — 任务已存在时不重新提交
     * 测试场景：scheduledTasks 中已存在对应 uniqueKey 的任务
     * 验证内容：1. syncExecutor.scheduleWithFixedDelay 不被调用
     *          2. 窗口状态被回退为 CREATED
     *          3. scheduledTasks 中仍为原始任务
     */
    @Test
    public void testSubmitSyncTask_TaskAlreadyExists() {
        // Arrange：放入一个已存在的任务
        scheduledTasks.put("test-unique-key", mockFuture);

        // Act：执行 submitSyncTask
        rateLimitExtension.submitSyncTask(remoteSyncTask, 0, 30);

        // Assert：确保任务没有被重新提交
        verify(syncExecutor, never()).scheduleWithFixedDelay(any(), anyLong(), anyLong(), any());

        // Assert：确保任务状态被设置为 CREATED
        verify(rateLimitWindow, times(1)).setStatus(RateLimitWindow.WindowStatus.CREATED.ordinal());

        // Assert：确保 scheduledTasks 仍然是原来的任务
        assertThat((Object) scheduledTasks.get("test-unique-key")).isEqualTo(mockFuture);
    }

    /**
     * 测试提交同步任务 — 新任务成功提交
     * 测试场景：scheduledTasks 中不存在对应 uniqueKey 的任务
     * 验证内容：1. syncExecutor.scheduleWithFixedDelay 被调用且参数精确匹配
     *          2. 任务被添加到 scheduledTasks
     */
    @Test
    public void testSubmitSyncTask_NewTask() {
        // Arrange：Mock 任务调度返回
        Mockito.<ScheduledFuture<?>>when(
                syncExecutor.scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class))
        ).thenReturn(mockFuture);

        // Act：执行 submitSyncTask
        rateLimitExtension.submitSyncTask(remoteSyncTask, 0, 30);

        // Assert：确保任务被正确提交，验证参数精确匹配
        verify(syncExecutor, times(1)).scheduleWithFixedDelay(
                eq(remoteSyncTask), eq(0L), eq(30L), eq(TimeUnit.MILLISECONDS));

        // Assert：确保任务被添加到 scheduledTasks
        assertThat((Object) scheduledTasks.get("test-unique-key")).isEqualTo(mockFuture);
    }

    /**
     * 测试停止同步任务 — 任务存在时正确停止
     * 测试场景：scheduledTasks 中存在对应 uniqueKey 的任务
     * 验证内容：1. 任务从 scheduledTasks 中被移除
     *          2. future.cancel(true) 被调用
     *          3. cleanTask 被通过 syncExecutor.schedule 提交
     */
    @Test
    public void testStopSyncTask_TaskExists() {
        // Arrange：放入一个已存在的任务
        scheduledTasks.put("test-unique-key", mockFuture);

        // Act：执行 stopSyncTask
        rateLimitExtension.stopSyncTask("test-unique-key", rateLimitWindow);

        // Assert：确保任务被移除
        assertThat((Object) scheduledTasks.get("test-unique-key")).isNull();

        // Assert：确保 future.cancel(true) 被调用
        verify(mockFuture, times(1)).cancel(true);

        // Assert：确保 cleanTask 被提交到 syncExecutor
        verify(syncExecutor, times(1)).schedule(any(Runnable.class), eq(10L), eq(TimeUnit.MILLISECONDS));
    }

    /**
     * 测试停止同步任务 — 任务不存在时不调用 cancel
     * 测试场景：scheduledTasks 中不存在对应 uniqueKey 的任务
     * 验证内容：1. future.cancel 不被调用
     *          2. cleanTask 仍被提交（清理逻辑始终执行）
     *          3. 不抛出异常
     */
    @Test
    public void testStopSyncTask_TaskNotExists() {
        // Arrange：scheduledTasks 为空，不放入任何任务

        // Act & Assert：执行 stopSyncTask 不抛异常
        assertThatCode(() -> rateLimitExtension.stopSyncTask("non-existent-key", rateLimitWindow))
                .doesNotThrowAnyException();

        // Assert：确保 future.cancel 不被调用（没有 future 可以取消）
        verify(mockFuture, never()).cancel(anyBoolean());

        // Assert：确保 cleanTask 仍然被提交到 syncExecutor
        verify(syncExecutor, times(1)).schedule(any(Runnable.class), eq(10L), eq(TimeUnit.MILLISECONDS));
    }

    /**
     * 测试提交过期检查任务
     * 测试场景：调用 submitExpireJob 提交一个定时任务
     * 验证内容：windowExpireExecutor.scheduleWithFixedDelay 被调用且参数为 5 秒间隔
     */
    @Test
    public void testSubmitExpireJob() {
        // Arrange：准备一个模拟任务
        Runnable expireTask = mock(Runnable.class);

        // Act：执行 submitExpireJob
        rateLimitExtension.submitExpireJob(expireTask);

        // Assert：确保过期任务被正确提交到 windowExpireExecutor，间隔 5 秒
        verify(windowExpireExecutor, times(1)).scheduleWithFixedDelay(
                eq(expireTask), eq(5L), eq(5L), eq(TimeUnit.SECONDS));
    }

    /**
     * 测试 destroy 方法触发线程池关闭
     * 测试场景：调用 destroy 方法
     * 验证内容：syncExecutor 和 windowExpireExecutor 被关闭
     */
    @Test
    public void testDestroy() {
        // Act：执行 destroy
        rateLimitExtension.destroy();

        // Assert：确保两个线程池都被关闭
        verify(syncExecutor, times(1)).shutdown();
        verify(windowExpireExecutor, times(1)).shutdown();
    }

    @SuppressWarnings("unchecked")
    private static <T> T getPrivateField(Object object, String fieldName)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(object);
    }

    private static void setPrivateField(Object object, String fieldName, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(object, value);
    }
}