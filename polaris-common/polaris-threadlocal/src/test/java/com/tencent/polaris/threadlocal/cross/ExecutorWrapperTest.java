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

package com.tencent.polaris.threadlocal.cross;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link ExecutorWrapper}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class ExecutorWrapperTest {

    private static final ThreadLocal<String> TEST_THREAD_LOCAL = new ThreadLocal<>();

    private static final String TEST = "TEST";

    private static final String OTHER = "OTHER";

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
        TEST_THREAD_LOCAL.remove();
    }

    /**
     * 测试 execute 上下文跨线程传播
     * 测试目的：验证通过 ExecutorWrapper 提交的任务能正确接收到父线程的上下文
     * 测试场景：在父线程设置上下文后通过 ExecutorWrapper 执行任务
     * 验证内容：子线程中获取到的上下文值与父线程设置的一致
     */
    @Test
    public void testExecutePropagatesContextToNewThread() throws InterruptedException {
        // Arrange
        TEST_THREAD_LOCAL.set(TEST);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> capturedContext = new AtomicReference<>();
        ExecutorWrapper<String> executorWrapper = new ExecutorWrapper<>(
                Executors.newCachedThreadPool(),
                TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set);

        // Act
        executorWrapper.execute(() -> {
            capturedContext.set(TEST_THREAD_LOCAL.get());
            latch.countDown();
        });
        latch.await();

        // Assert
        assertThat(capturedContext.get()).isEqualTo(TEST);
    }

    /**
     * 测试 execute null 上下文传播
     * 测试目的：验证当父线程上下文为 null 时，子线程也能正确接收到 null
     * 测试场景：父线程不设置上下文，通过 ExecutorWrapper 执行任务
     * 验证内容：子线程中获取到的上下文为 null
     */
    @Test
    public void testExecuteWithNullContext() throws InterruptedException {
        // Arrange
        TEST_THREAD_LOCAL.remove();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> capturedContext = new AtomicReference<>("polaris");
        ExecutorWrapper<String> executorWrapper = new ExecutorWrapper<>(
                Executors.newCachedThreadPool(),
                TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set);

        // Act
        executorWrapper.execute(() -> {
            capturedContext.set(TEST_THREAD_LOCAL.get());
            latch.countDown();
        });
        latch.await();

        // Assert
        assertThat(capturedContext.get()).isNull();
    }

    /**
     * 测试 execute 多任务上下文传播
     * 测试目的：验证通过 ExecutorWrapper 提交多个任务时，每个任务都能正确接收到父线程的上下文
     * 测试场景：使用固定线程池提交多个任务
     * 验证内容：所有任务中获取到的上下文值都与父线程一致
     */
    @Test
    public void testExecuteMultipleTasks() throws InterruptedException {
        // Arrange
        TEST_THREAD_LOCAL.set(TEST);
        int taskCount = 3;
        CountDownLatch latch = new CountDownLatch(taskCount);
        AtomicReference<Boolean> allMatch = new AtomicReference<>(true);
        ExecutorService delegate = Executors.newFixedThreadPool(taskCount);
        ExecutorWrapper<String> executorWrapper = new ExecutorWrapper<>(
                delegate,
                TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set);

        // Act
        for (int idx = 0; idx < taskCount; idx++) {
            executorWrapper.execute(() -> {
                if (!TEST.equals(TEST_THREAD_LOCAL.get())) {
                    allMatch.set(false);
                }
                latch.countDown();
            });
        }
        latch.await();
        delegate.shutdown();

        // Assert
        assertThat(allMatch.get()).isTrue();
    }

    /**
     * 测试 execute 执行后恢复执行线程的上下文
     * 测试目的：验证任务执行完成后，执行线程的原有上下文被正确恢复
     * 测试场景：在子线程中先设置一个已知上下文，然后通过 ExecutorWrapper 执行任务
     * 验证内容：任务执行完成后，执行线程的上下文恢复为任务执行前的值
     */
    @Test
    public void testExecuteRestoresWorkerThreadContext() throws InterruptedException {
        // Arrange
        TEST_THREAD_LOCAL.set(TEST);
        ExecutorService singleThread = Executors.newSingleThreadExecutor();
        ExecutorWrapper<String> executorWrapper = new ExecutorWrapper<>(
                singleThread,
                TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set);

        // First, set a known value on the worker thread
        CountDownLatch setupLatch = new CountDownLatch(1);
        singleThread.execute(() -> {
            TEST_THREAD_LOCAL.set(OTHER);
            setupLatch.countDown();
        });
        setupLatch.await();

        // Act: execute a task through the wrapper (will set context to TEST, then restore to OTHER)
        CountDownLatch taskLatch = new CountDownLatch(1);
        AtomicReference<String> contextDuringTask = new AtomicReference<>();
        executorWrapper.execute(() -> {
            contextDuringTask.set(TEST_THREAD_LOCAL.get());
            taskLatch.countDown();
        });
        taskLatch.await();

        // Verify context was TEST during task execution
        assertThat(contextDuringTask.get()).isEqualTo(TEST);

        // Assert: verify the worker thread's context was restored to OTHER
        CountDownLatch verifyLatch = new CountDownLatch(1);
        AtomicReference<String> restoredContext = new AtomicReference<>();
        singleThread.execute(() -> {
            restoredContext.set(TEST_THREAD_LOCAL.get());
            verifyLatch.countDown();
        });
        verifyLatch.await();
        singleThread.shutdown();
        assertThat(restoredContext.get()).isEqualTo(OTHER);
    }
}
