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
import com.tencent.polaris.ratelimit.client.flow.QuotaFlow;
import com.tencent.polaris.ratelimit.client.flow.RateLimitExtension;
import com.tencent.polaris.ratelimit.client.flow.RateLimitWindow;
import com.tencent.polaris.ratelimit.client.sync.RemoteSyncTask;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

public class SubmitPolarisRemoteSyncTaskTest {

    @Mock
    private Extensions extensions;

    @Mock
    private ScheduledExecutorService syncExecutor;

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
        MockitoAnnotations.initMocks(this);
        QuotaFlow quotaFlow = new QuotaFlow();
        ConfigurationImpl config = new ConfigurationImpl();
        config.setDefault();
        config.getGlobal().getAPI().setReportEnable(false);
        config.getGlobal().getStatReporter().setEnable(false);
        context = SDKContext.initContextByConfig(config);
        context.init();
        Extensions extensions = context.getExtensions();
        rateLimitExtension = new RateLimitExtension(extensions);

        // 使用反射替换 syncExecutor，避免真实线程池执行
        Field syncExecutorField = RateLimitExtension.class.getDeclaredField("syncExecutor");
        syncExecutorField.setAccessible(true);
        syncExecutorField.set(rateLimitExtension, syncExecutor);

        // 使用反射获取 scheduledTasks
        Field scheduledTasksField = RateLimitExtension.class.getDeclaredField("scheduledTasks");
        scheduledTasksField.setAccessible(true);
        scheduledTasks = (Map<String, ScheduledFuture<?>>) scheduledTasksField.get(rateLimitExtension);

        // Mock 限流窗口
        when(remoteSyncTask.getWindow()).thenReturn(rateLimitWindow);
        when(rateLimitWindow.getUniqueKey()).thenReturn("test-unique-key");
    }

    @After
    public void tearDown() throws Exception {
        context.destroy();
    }

    /*
         case_name: 提交同步任务测试
         case_path: 访问限流/同步任务
         case_description: 测试在任务存在时，不重新提交任务
         */
    @Test
    public void testSubmitPolarisRemoteSyncTask_TaskAlreadyExists() {
        // 先放入一个任务
        scheduledTasks.put("test-unique-key", mockFuture);

        // 执行 submitSyncTask
        rateLimitExtension.submitSyncTask(remoteSyncTask, 0, 30);

        // 确保任务没有被重新提交
        verify(syncExecutor, never()).scheduleWithFixedDelay(any(), anyLong(), anyLong(), any());

        // 确保任务状态被设置为 CREATED
        verify(rateLimitWindow, times(1)).setStatus(RateLimitWindow.WindowStatus.CREATED.ordinal());

        // 确保 scheduledTasks 仍然是原来的任务
        assertEquals(mockFuture, scheduledTasks.get("test-unique-key"));
    }

    /*
     case_name: 提交同步任务测试
     case_path: 访问限流/同步任务
     case_description: 测试在任务不存在时，可以提交任务
     */
    @Test
    public void testSubmitPolarisRemoteSyncTask_NewTask() {
        // Mock 任务调度
        Mockito.<ScheduledFuture<?>>when(
                syncExecutor.scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class))
        ).thenReturn(mockFuture);

        // 执行 submitSyncTask
        rateLimitExtension.submitSyncTask(remoteSyncTask, 0, 30);

        // 确保任务被提交
        verify(syncExecutor, times(1)).scheduleWithFixedDelay(eq(remoteSyncTask), eq(0L), anyLong(),
                eq(TimeUnit.MILLISECONDS));

        // 确保任务被添加到 scheduledTasks
        assertEquals(mockFuture, scheduledTasks.get("test-unique-key"));
    }

    /*
     case_name: 提交同步任务测试
     case_path: 访问限流/同步任务
     case_description: 测试删除任务时，停止任务
     */
    @Test
    public void testStopPolarisRemoteSyncTask() {
        // 先放入一个任务
        scheduledTasks.put("test-unique-key", mockFuture);

        // 执行 stopSyncTask
        rateLimitExtension.stopSyncTask("test-unique-key", rateLimitWindow);

        // 确保任务被移除
        assertNull(scheduledTasks.get("test-unique-key"));

        // 确保 future.cancel(true) 被调用
        verify(mockFuture, times(1)).cancel(true);
    }
}