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

package com.tencent.polaris.plugins.connector.consul.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.polaris.plugins.connector.common.ServiceUpdateTask;
import com.tencent.polaris.plugins.connector.consul.ConsulContext;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Test for {@link ConsulService} refresh executor.
 *
 * @author Ajax-1
 */
public class ConsulServiceTest {

    // 让任务阻塞的闸门，模拟 blocking query 占住线程
    private final CountDownLatch blockLatch = new CountDownLatch(1);

    private TestConsulService consulService;

    @After
    public void tearDown() {
        // 放开阻塞任务并关闭线程池，避免线程泄漏影响其他测试
        blockLatch.countDown();
        if (consulService != null) {
            consulService.destroy();
        }
    }

    @Test
    public void testRefreshExecutor_defaultContext_poolSizeIsDefault() {
        // Arrange & Act：使用默认上下文创建服务
        consulService = new TestConsulService(new ConsulContext(), null);

        // Assert：线程池大小为 8
        assertPoolSize(ConsulContext.DEFAULT_REFRESH_THREAD_POOL_SIZE);
    }

    @Test
    public void testRefreshExecutor_customSize_poolSizeIsCustom() {
        // Arrange：配置 16 个线程
        ConsulContext consulContext = new ConsulContext();
        consulContext.setRefreshThreadPoolSize(16);

        // Act：创建服务
        consulService = new TestConsulService(consulContext, null);

        // Assert：线程池大小为 16
        assertPoolSize(16);
    }

    @Test
    public void testRefreshExecutor_nullContext_poolSizeIsDefault() {
        // Act：上下文为 null 时创建服务
        consulService = new TestConsulService(null, null);

        // Assert：回退到默认值
        assertPoolSize(ConsulContext.DEFAULT_REFRESH_THREAD_POOL_SIZE);
    }

    @Test
    public void testRefreshExecutor_notPositiveSize_poolSizeIsDefault() {
        // Arrange：线程数被设置为非法值
        ConsulContext consulContext = new ConsulContext();
        consulContext.setRefreshThreadPoolSize(0);

        // Act：创建服务
        consulService = new TestConsulService(consulContext, null);

        // Assert：回退到默认值
        assertPoolSize(ConsulContext.DEFAULT_REFRESH_THREAD_POOL_SIZE);
    }

    @Test
    public void testAsyncSendRequest_poolSizeTen_tenTasksRunConcurrently() throws InterruptedException {
        // Arrange：配置 10 个线程，准备 10 个阻塞任务
        ConsulContext consulContext = new ConsulContext();
        consulContext.setRefreshThreadPoolSize(10);
        CountDownLatch startedLatch = new CountDownLatch(10);
        consulService = new TestConsulService(consulContext, startedLatch);

        // Act：提交 10 个任务
        submitTasks(10);

        // Assert：10 个任务都能同时开始执行，队列为空
        Assertions.assertThat(startedLatch.await(2, TimeUnit.SECONDS)).isTrue();
        Assertions.assertThat(executor().getQueue()).isEmpty();
    }

    @Test
    public void testAsyncSendRequest_defaultPoolSize_extraTasksQueued() throws InterruptedException {
        // Arrange：默认 8 个线程，准备 10 个阻塞任务
        CountDownLatch startedLatch = new CountDownLatch(8);
        consulService = new TestConsulService(new ConsulContext(), startedLatch);

        // Act：提交 10 个任务
        submitTasks(10);

        // Assert：只有 8 个任务开始执行，剩余 2 个在队列中排队
        Assertions.assertThat(startedLatch.await(2, TimeUnit.SECONDS)).isTrue();
        Assertions.assertThat(executor().getActiveCount()).isEqualTo(8);
        Assertions.assertThat(executor().getQueue()).hasSize(2);
    }

    private void submitTasks(int count) {
        for (int i = 0; i < count; i++) {
            consulService.asyncSendRequest(Mockito.mock(ServiceUpdateTask.class));
        }
    }

    private ThreadPoolExecutor executor() {
        return (ThreadPoolExecutor) consulService.refreshExecutor;
    }

    private void assertPoolSize(int expected) {
        Assertions.assertThat(executor().getCorePoolSize()).isEqualTo(expected);
        Assertions.assertThat(executor().getMaximumPoolSize()).isEqualTo(expected);
    }

    /**
     * 最小实现：任务开始时计数，然后阻塞直到测试放开闸门。
     */
    private class TestConsulService extends ConsulService {

        private final CountDownLatch startedLatch;

        TestConsulService(ConsulContext consulContext, CountDownLatch startedLatch) {
            super(null, null, consulContext, "consul-test", new ObjectMapper());
            this.startedLatch = startedLatch;
        }

        @Override
        protected void sendRequest(ServiceUpdateTask serviceUpdateTask) {
            if (startedLatch != null) {
                startedLatch.countDown();
            }
            try {
                blockLatch.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
