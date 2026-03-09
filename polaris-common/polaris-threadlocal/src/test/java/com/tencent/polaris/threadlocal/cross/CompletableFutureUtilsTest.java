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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link CompletableFutureUtils}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class CompletableFutureUtilsTest {

    private static final ThreadLocal<String> TEST_THREAD_LOCAL = new ThreadLocal<>();

    private static final String TEST = "TEST";

    private static final String OTHER = "OTHER";

    @After
    public void tearDown() {
        TEST_THREAD_LOCAL.remove();
    }

    @Before
    public void setUp() {
    }

    /**
     * 测试 supplyAsync 上下文传播
     * 测试目的：验证 supplyAsync 能将父线程的上下文传播到异步线程
     * 测试场景：在父线程设置上下文后调用 supplyAsync
     * 验证内容：异步线程中获取到的上下文值与父线程一致
     */
    @Test
    public void testSupplyAsyncPropagatesContext() throws ExecutionException, InterruptedException {
        // Arrange
        TEST_THREAD_LOCAL.set(TEST);
        AtomicReference<Boolean> result = new AtomicReference<>(false);

        // Act
        CompletableFutureUtils.supplyAsync(
                () -> {
                    result.set(TEST.equals(TEST_THREAD_LOCAL.get()));
                    return null;
                },
                TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set).get();

        // Assert
        assertThat(result.get()).isTrue();
    }

    /**
     * 测试 supplyAsync 返回值传递
     * 测试目的：验证 supplyAsync 能正确返回 supplier 的返回值
     * 测试场景：supplier 返回当前上下文值
     * 验证内容：返回值与预期一致
     */
    @Test
    public void testSupplyAsyncReturnsValue() throws ExecutionException, InterruptedException {
        // Arrange
        TEST_THREAD_LOCAL.set(TEST);

        // Act
        String result = CompletableFutureUtils.<String, String>supplyAsync(
                TEST_THREAD_LOCAL::get,
                TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set).get();

        // Assert
        assertThat(result).isEqualTo(TEST);
    }

    /**
     * 测试 supplyAsync null 上下文传播
     * 测试目的：验证当父线程上下文为 null 时，异步线程也能正确接收到 null
     * 测试场景：父线程不设置上下文
     * 验证内容：异步线程中获取到的上下文为 null
     */
    @Test
    public void testSupplyAsyncWithNullContext() throws ExecutionException, InterruptedException {
        // Arrange
        TEST_THREAD_LOCAL.remove();
        AtomicReference<String> capturedContext = new AtomicReference<>("polaris");

        // Act
        CompletableFutureUtils.supplyAsync(
                () -> {
                    capturedContext.set(TEST_THREAD_LOCAL.get());
                    return null;
                },
                TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set).get();

        // Assert
        assertThat(capturedContext.get()).isNull();
    }

    /**
     * 测试 supplyAsync 不修改调用者上下文
     * 测试目的：验证异步任务中修改上下文不会影响父线程的上下文
     * 测试场景：异步任务中将上下文修改为 OTHER
     * 验证内容：父线程的上下文仍为 TEST
     */
    @Test
    public void testSupplyAsyncDoesNotModifyCallerContext() throws ExecutionException, InterruptedException {
        // Arrange
        TEST_THREAD_LOCAL.set(TEST);

        // Act
        CompletableFutureUtils.supplyAsync(
                () -> {
                    TEST_THREAD_LOCAL.set(OTHER);
                    return null;
                },
                TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set).get();

        // Assert
        assertThat(TEST_THREAD_LOCAL.get()).isEqualTo(TEST);
    }

    /**
     * 测试带 Executor 的 supplyAsync 上下文传播
     * 测试目的：验证指定 Executor 时 supplyAsync 也能正确传播上下文
     * 测试场景：使用自定义线程池执行异步任务
     * 验证内容：异步线程中获取到的上下文值与父线程一致
     */
    @Test
    public void testSupplyAsyncWithExecutorPropagatesContext() throws ExecutionException, InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            // Arrange
            TEST_THREAD_LOCAL.set(TEST);
            AtomicReference<Boolean> result = new AtomicReference<>(false);

            // Act
            CompletableFutureUtils.supplyAsync(
                    () -> {
                        result.set(TEST.equals(TEST_THREAD_LOCAL.get()));
                        return null;
                    },
                    TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set, executor).get();

            // Assert
            assertThat(result.get()).isTrue();
        } finally {
            executor.shutdown();
        }
    }

    /**
     * 测试带 Executor 的 supplyAsync 返回值传递
     * 测试目的：验证指定 Executor 时 supplyAsync 能正确返回 supplier 的返回值
     * 测试场景：使用自定义线程池，supplier 返回当前上下文值
     * 验证内容：返回值与预期一致
     */
    @Test
    public void testSupplyAsyncWithExecutorReturnsValue() throws ExecutionException, InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            // Arrange
            TEST_THREAD_LOCAL.set(TEST);

            // Act
            String result = CompletableFutureUtils.<String, String>supplyAsync(
                    TEST_THREAD_LOCAL::get,
                    TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set, executor).get();

            // Assert
            assertThat(result).isEqualTo(TEST);
        } finally {
            executor.shutdown();
        }
    }

    /**
     * 测试带 Executor 的 supplyAsync null 上下文传播
     * 测试目的：验证指定 Executor 时上下文为 null 也能正确传播
     * 测试场景：父线程不设置上下文，使用自定义线程池
     * 验证内容：异步线程中获取到的上下文为 null
     */
    @Test
    public void testSupplyAsyncWithExecutorNullContext() throws ExecutionException, InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            // Arrange
            TEST_THREAD_LOCAL.remove();
            AtomicReference<String> capturedContext = new AtomicReference<>("polaris");

            // Act
            CompletableFutureUtils.supplyAsync(
                    () -> {
                        capturedContext.set(TEST_THREAD_LOCAL.get());
                        return null;
                    },
                    TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set, executor).get();

            // Assert
            assertThat(capturedContext.get()).isNull();
        } finally {
            executor.shutdown();
        }
    }

    /**
     * 测试带 Executor 的 supplyAsync 不修改调用者上下文
     * 测试目的：验证指定 Executor 时异步任务中修改上下文不影响父线程
     * 测试场景：使用自定义线程池，异步任务中将上下文修改为 OTHER
     * 验证内容：父线程上下文仍为 TEST
     */
    @Test
    public void testSupplyAsyncWithExecutorDoesNotModifyCallerContext()
            throws ExecutionException, InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            // Arrange
            TEST_THREAD_LOCAL.set(TEST);

            // Act
            CompletableFutureUtils.supplyAsync(
                    () -> {
                        TEST_THREAD_LOCAL.set(OTHER);
                        return null;
                    },
                    TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set, executor).get();

            // Assert
            assertThat(TEST_THREAD_LOCAL.get()).isEqualTo(TEST);
        } finally {
            executor.shutdown();
        }
    }

    /**
     * 测试带 Executor 的 supplyAsync 异常传播
     * 测试目的：验证指定 Executor 时 supplier 抛出的异常能正确传播
     * 测试场景：supplier 执行时抛出 RuntimeException
     * 验证内容：通过 future.get() 获取到 ExecutionException，其 cause 为 RuntimeException
     */
    @Test
    public void testSupplyAsyncWithExecutorPropagatesException() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            // Arrange
            TEST_THREAD_LOCAL.set(TEST);

            // Act
            CompletableFuture<Void> future = CompletableFutureUtils.supplyAsync(
                    () -> {
                        throw new RuntimeException("supply error");
                    },
                    TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set, executor);

            // Assert
            assertThatThrownBy(future::get)
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(RuntimeException.class);
        } finally {
            executor.shutdown();
        }
    }

    /**
     * 测试 runAsync 上下文传播
     * 测试目的：验证 runAsync 能将父线程的上下文传播到异步线程
     * 测试场景：在父线程设置上下文后调用 runAsync
     * 验证内容：异步线程中获取到的上下文值与父线程一致
     */
    @Test
    public void testRunAsyncPropagatesContext() throws ExecutionException, InterruptedException {
        // Arrange
        TEST_THREAD_LOCAL.set(TEST);
        AtomicReference<Boolean> result = new AtomicReference<>(false);

        // Act
        CompletableFutureUtils.runAsync(
                () -> result.set(TEST.equals(TEST_THREAD_LOCAL.get())),
                TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set).get();

        // Assert
        assertThat(result.get()).isTrue();
    }

    /**
     * 测试 runAsync null 上下文传播
     * 测试目的：验证当父线程上下文为 null 时，runAsync 也能正确传播
     * 测试场景：父线程不设置上下文
     * 验证内容：异步线程中获取到的上下文为 null
     */
    @Test
    public void testRunAsyncWithNullContext() throws ExecutionException, InterruptedException {
        // Arrange
        TEST_THREAD_LOCAL.remove();
        AtomicReference<String> capturedContext = new AtomicReference<>("polaris");

        // Act
        CompletableFutureUtils.runAsync(
                () -> capturedContext.set(TEST_THREAD_LOCAL.get()),
                TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set).get();

        // Assert
        assertThat(capturedContext.get()).isNull();
    }

    /**
     * 测试 runAsync 不修改调用者上下文
     * 测试目的：验证异步任务中修改上下文不会影响父线程
     * 测试场景：异步任务中将上下文修改为 OTHER
     * 验证内容：父线程上下文仍为 TEST
     */
    @Test
    public void testRunAsyncDoesNotModifyCallerContext() throws ExecutionException, InterruptedException {
        // Arrange
        TEST_THREAD_LOCAL.set(TEST);

        // Act
        CompletableFutureUtils.runAsync(
                () -> TEST_THREAD_LOCAL.set(OTHER),
                TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set).get();

        // Assert
        assertThat(TEST_THREAD_LOCAL.get()).isEqualTo(TEST);
    }

    /**
     * 测试带 Executor 的 runAsync 上下文传播
     * 测试目的：验证指定 Executor 时 runAsync 也能正确传播上下文
     * 测试场景：使用自定义线程池执行异步任务
     * 验证内容：异步线程中获取到的上下文值与父线程一致
     */
    @Test
    public void testRunAsyncWithExecutorPropagatesContext() throws ExecutionException, InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            // Arrange
            TEST_THREAD_LOCAL.set(TEST);
            AtomicReference<Boolean> result = new AtomicReference<>(false);

            // Act
            CompletableFutureUtils.runAsync(
                    () -> result.set(TEST.equals(TEST_THREAD_LOCAL.get())),
                    TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set, executor).get();

            // Assert
            assertThat(result.get()).isTrue();
        } finally {
            executor.shutdown();
        }
    }

    /**
     * 测试带 Executor 的 runAsync null 上下文传播
     * 测试目的：验证指定 Executor 时上下文为 null 也能正确传播
     * 测试场景：父线程不设置上下文，使用自定义线程池
     * 验证内容：异步线程中获取到的上下文为 null
     */
    @Test
    public void testRunAsyncWithExecutorNullContext() throws ExecutionException, InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            // Arrange
            TEST_THREAD_LOCAL.remove();
            AtomicReference<String> capturedContext = new AtomicReference<>("polaris");

            // Act
            CompletableFutureUtils.runAsync(
                    () -> capturedContext.set(TEST_THREAD_LOCAL.get()),
                    TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set, executor).get();

            // Assert
            assertThat(capturedContext.get()).isNull();
        } finally {
            executor.shutdown();
        }
    }

    /**
     * 测试带 Executor 的 runAsync 不修改调用者上下文
     * 测试目的：验证指定 Executor 时异步任务中修改上下文不影响父线程
     * 测试场景：使用自定义线程池，异步任务中将上下文修改为 OTHER
     * 验证内容：父线程上下文仍为 TEST
     */
    @Test
    public void testRunAsyncWithExecutorDoesNotModifyCallerContext()
            throws ExecutionException, InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            // Arrange
            TEST_THREAD_LOCAL.set(TEST);

            // Act
            CompletableFutureUtils.runAsync(
                    () -> TEST_THREAD_LOCAL.set(OTHER),
                    TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set, executor).get();

            // Assert
            assertThat(TEST_THREAD_LOCAL.get()).isEqualTo(TEST);
        } finally {
            executor.shutdown();
        }
    }

    /**
     * 测试带 Executor 的 runAsync 异常传播
     * 测试目的：验证指定 Executor 时 runnable 抛出的异常能正确传播
     * 测试场景：runnable 执行时抛出 RuntimeException
     * 验证内容：通过 future.get() 获取到 ExecutionException，其 cause 为 RuntimeException
     */
    @Test
    public void testRunAsyncWithExecutorPropagatesException() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            // Arrange
            TEST_THREAD_LOCAL.set(TEST);

            // Act
            CompletableFuture<Void> future = CompletableFutureUtils.runAsync(
                    () -> {
                        throw new RuntimeException("run error");
                    },
                    TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set, executor);

            // Assert
            assertThatThrownBy(future::get)
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(RuntimeException.class);
        } finally {
            executor.shutdown();
        }
    }

    /**
     * 测试 supplyAsync 异常传播（无 Executor）
     * 测试目的：验证不指定 Executor 时 supplier 抛出的异常能正确传播
     * 测试场景：supplier 执行时抛出 RuntimeException
     * 验证内容：通过 future.get() 获取到 ExecutionException，其 cause 为 RuntimeException
     */
    @Test
    public void testSupplyAsyncPropagatesException() {
        // Arrange
        TEST_THREAD_LOCAL.set(TEST);

        // Act
        CompletableFuture<Void> future = CompletableFutureUtils.supplyAsync(
                () -> {
                    throw new RuntimeException("supply error");
                },
                TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set);

        // Assert
        assertThatThrownBy(future::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RuntimeException.class);
    }

    /**
     * 测试 runAsync 异常传播（无 Executor）
     * 测试目的：验证不指定 Executor 时 runnable 抛出的异常能正确传播
     * 测试场景：runnable 执行时抛出 RuntimeException
     * 验证内容：通过 future.get() 获取到 ExecutionException，其 cause 为 RuntimeException
     */
    @Test
    public void testRunAsyncPropagatesException() {
        // Arrange
        TEST_THREAD_LOCAL.set(TEST);

        // Act
        CompletableFuture<Void> future = CompletableFutureUtils.runAsync(
                () -> {
                    throw new RuntimeException("run error");
                },
                TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set);

        // Assert
        assertThatThrownBy(future::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RuntimeException.class);
    }
}
