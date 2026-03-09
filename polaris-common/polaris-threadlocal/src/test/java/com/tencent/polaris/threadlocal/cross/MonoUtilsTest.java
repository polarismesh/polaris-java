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

import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link MonoUtils}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class MonoUtilsTest {

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
     * 测试 fromSupplier 上下文传播
     * 测试目的：验证 fromSupplier 能将父线程的上下文传播到 Mono 执行线程
     * 测试场景：在父线程设置上下文后调用 fromSupplier
     * 验证内容：Mono 执行时获取到的上下文值与父线程一致
     */
    @Test
    public void testFromSupplierPropagatesContext() {
        // Arrange
        TEST_THREAD_LOCAL.set(TEST);
        AtomicReference<Boolean> result = new AtomicReference<>(false);

        // Act
        MonoUtils.fromSupplier(
                () -> {
                    result.set(TEST.equals(TEST_THREAD_LOCAL.get()));
                    return null;
                },
                TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set).block();

        // Assert
        assertThat(result.get()).isTrue();
    }

    /**
     * 测试 fromSupplier 返回值传递
     * 测试目的：验证 fromSupplier 能正确返回 supplier 的返回值
     * 测试场景：supplier 返回当前上下文值
     * 验证内容：Mono 发出的值与预期一致
     */
    @Test
    public void testFromSupplierReturnsValue() {
        // Arrange
        TEST_THREAD_LOCAL.set(TEST);

        // Act
        String result = MonoUtils.<String, String>fromSupplier(
                TEST_THREAD_LOCAL::get,
                TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set).block();

        // Assert
        assertThat(result).isEqualTo(TEST);
    }

    /**
     * 测试 fromSupplier null 上下文传播
     * 测试目的：验证当父线程上下文为 null 时，Mono 执行线程也能正确接收到 null
     * 测试场景：父线程不设置上下文
     * 验证内容：Mono 执行时获取到的上下文为 null
     */
    @Test
    public void testFromSupplierWithNullContext() {
        // Arrange
        TEST_THREAD_LOCAL.remove();
        AtomicReference<String> capturedContext = new AtomicReference<>("sentinel");

        // Act
        MonoUtils.fromSupplier(
                () -> {
                    capturedContext.set(TEST_THREAD_LOCAL.get());
                    return null;
                },
                TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set).block();

        // Assert
        assertThat(capturedContext.get()).isNull();
    }

    /**
     * 测试 fromSupplier 不修改调用者上下文
     * 测试目的：验证 Mono 执行线程中修改上下文不会影响父线程的上下文
     * 测试场景：Mono 执行中将上下文修改为 OTHER
     * 验证内容：父线程的上下文仍为 TEST
     */
    @Test
    public void testFromSupplierDoesNotModifyCallerContext() {
        // Arrange
        TEST_THREAD_LOCAL.set(TEST);

        // Act
        MonoUtils.fromSupplier(
                () -> {
                    TEST_THREAD_LOCAL.set(OTHER);
                    return null;
                },
                TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set).block();

        // Assert
        assertThat(TEST_THREAD_LOCAL.get()).isEqualTo(TEST);
    }

    /**
     * 测试 fromSupplier 异常传播
     * 测试目的：验证 supplier 抛出的异常能通过 Mono 正确传播
     * 测试场景：supplier 执行时抛出 RuntimeException
     * 验证内容：Mono.block() 抛出包含原始异常的 RuntimeException
     */
    @Test
    public void testFromSupplierPropagatesException() {
        // Arrange
        TEST_THREAD_LOCAL.set(TEST);

        // Act & Assert
        assertThatThrownBy(() ->
                MonoUtils.fromSupplier(
                        () -> {
                            throw new RuntimeException("supplier error");
                        },
                        TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set).block()
        ).isInstanceOf(RuntimeException.class);
    }

    /**
     * 测试 fromCallable 上下文传播
     * 测试目的：验证 fromCallable 能将父线程的上下文传播到 Mono 执行线程
     * 测试场景：在父线程设置上下文后调用 fromCallable
     * 验证内容：Mono 执行时获取到的上下文值与父线程一致
     */
    @Test
    public void testFromCallablePropagatesContext() {
        // Arrange
        TEST_THREAD_LOCAL.set(TEST);
        AtomicReference<Boolean> result = new AtomicReference<>(false);

        // Act
        MonoUtils.fromCallable(
                () -> {
                    result.set(TEST.equals(TEST_THREAD_LOCAL.get()));
                    return null;
                },
                TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set).block();

        // Assert
        assertThat(result.get()).isTrue();
    }

    /**
     * 测试 fromCallable 返回值传递
     * 测试目的：验证 fromCallable 能正确返回 callable 的返回值
     * 测试场景：callable 返回当前上下文值
     * 验证内容：Mono 发出的值与预期一致
     */
    @Test
    public void testFromCallableReturnsValue() throws Exception {
        // Arrange
        TEST_THREAD_LOCAL.set(TEST);

        // Act
        String result = MonoUtils.<String, String>fromCallable(
                TEST_THREAD_LOCAL::get,
                TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set).block();

        // Assert
        assertThat(result).isEqualTo(TEST);
    }

    /**
     * 测试 fromCallable null 上下文传播
     * 测试目的：验证当父线程上下文为 null 时，Mono 执行线程也能正确接收到 null
     * 测试场景：父线程不设置上下文
     * 验证内容：Mono 执行时获取到的上下文为 null
     */
    @Test
    public void testFromCallableWithNullContext() {
        // Arrange
        TEST_THREAD_LOCAL.remove();
        AtomicReference<String> capturedContext = new AtomicReference<>("sentinel");

        // Act
        MonoUtils.fromCallable(
                () -> {
                    capturedContext.set(TEST_THREAD_LOCAL.get());
                    return null;
                },
                TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set).block();

        // Assert
        assertThat(capturedContext.get()).isNull();
    }

    /**
     * 测试 fromCallable 不修改调用者上下文
     * 测试目的：验证 Mono 执行线程中修改上下文不会影响父线程的上下文
     * 测试场景：Mono 执行中将上下文修改为 OTHER
     * 验证内容：父线程的上下文仍为 TEST
     */
    @Test
    public void testFromCallableDoesNotModifyCallerContext() {
        // Arrange
        TEST_THREAD_LOCAL.set(TEST);

        // Act
        MonoUtils.fromCallable(
                () -> {
                    TEST_THREAD_LOCAL.set(OTHER);
                    return null;
                },
                TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set).block();

        // Assert
        assertThat(TEST_THREAD_LOCAL.get()).isEqualTo(TEST);
    }

    /**
     * 测试 fromCallable 异常传播
     * 测试目的：验证 callable 抛出的异常能通过 Mono 正确传播
     * 测试场景：callable 执行时抛出 Exception
     * 验证内容：Mono.block() 抛出包含原始异常的 RuntimeException
     */
    @Test
    public void testFromCallablePropagatesException() {
        // Arrange
        TEST_THREAD_LOCAL.set(TEST);

        // Act & Assert
        assertThatThrownBy(() ->
                MonoUtils.fromCallable(
                        () -> {
                            throw new Exception("callable error");
                        },
                        TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set).block()
        ).isInstanceOf(RuntimeException.class);
    }

    /**
     * 测试 fromRunnable 上下文传播
     * 测试目的：验证 fromRunnable 能将父线程的上下文传播到 Mono 执行线程
     * 测试场景：在父线程设置上下文后调用 fromRunnable
     * 验证内容：Mono 执行时获取到的上下文值与父线程一致
     */
    @Test
    public void testFromRunnablePropagatesContext() {
        // Arrange
        TEST_THREAD_LOCAL.set(TEST);
        AtomicReference<Boolean> result = new AtomicReference<>(false);

        // Act
        MonoUtils.fromRunnable(
                () -> result.set(TEST.equals(TEST_THREAD_LOCAL.get())),
                TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set).block();

        // Assert
        assertThat(result.get()).isTrue();
    }

    /**
     * 测试 fromRunnable null 上下文传播
     * 测试目的：验证当父线程上下文为 null 时，Mono 执行线程也能正确接收到 null
     * 测试场景：父线程不设置上下文
     * 验证内容：Mono 执行时获取到的上下文为 null
     */
    @Test
    public void testFromRunnableWithNullContext() {
        // Arrange
        TEST_THREAD_LOCAL.remove();
        AtomicReference<String> capturedContext = new AtomicReference<>("sentinel");

        // Act
        MonoUtils.fromRunnable(
                () -> capturedContext.set(TEST_THREAD_LOCAL.get()),
                TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set).block();

        // Assert
        assertThat(capturedContext.get()).isNull();
    }

    /**
     * 测试 fromRunnable 不修改调用者上下文
     * 测试目的：验证 Mono 执行线程中修改上下文不会影响父线程的上下文
     * 测试场景：Mono 执行中将上下文修改为 OTHER
     * 验证内容：父线程的上下文仍为 TEST
     */
    @Test
    public void testFromRunnableDoesNotModifyCallerContext() {
        // Arrange
        TEST_THREAD_LOCAL.set(TEST);

        // Act
        MonoUtils.fromRunnable(
                () -> TEST_THREAD_LOCAL.set(OTHER),
                TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set).block();

        // Assert
        assertThat(TEST_THREAD_LOCAL.get()).isEqualTo(TEST);
    }

    /**
     * 测试 fromRunnable 异常传播
     * 测试目的：验证 runnable 抛出的异常能通过 Mono 正确传播
     * 测试场景：runnable 执行时抛出 RuntimeException
     * 验证内容：Mono.block() 抛出包含原始异常的 RuntimeException
     */
    @Test
    public void testFromRunnablePropagatesException() {
        // Arrange
        TEST_THREAD_LOCAL.set(TEST);

        // Act & Assert
        assertThatThrownBy(() ->
                MonoUtils.fromRunnable(
                        () -> {
                            throw new RuntimeException("runnable error");
                        },
                        TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set).block()
        ).isInstanceOf(RuntimeException.class);
    }

    /**
     * 测试 fromFuture 上下文传播
     * 测试目的：验证 fromFuture 能将父线程的上下文传播到异步 supplier 的执行中
     * 测试场景：在父线程设置上下文后调用 fromFuture
     * 验证内容：异步执行时获取到的上下文值与父线程一致
     */
    @Test
    public void testFromFuturePropagatesContext() {
        // Arrange
        TEST_THREAD_LOCAL.set(TEST);
        AtomicReference<Boolean> result = new AtomicReference<>(false);

        // Act
        MonoUtils.fromFuture(
                () -> {
                    result.set(TEST.equals(TEST_THREAD_LOCAL.get()));
                    return null;
                },
                TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set).block();

        // Assert
        assertThat(result.get()).isTrue();
    }

    /**
     * 测试 fromFuture 返回值传递
     * 测试目的：验证 fromFuture 能正确发出 supplier 的结果值
     * 测试场景：supplier 返回当前上下文值
     * 验证内容：Mono 发出的值与预期一致
     */
    @Test
    public void testFromFutureReturnsValue() {
        // Arrange
        TEST_THREAD_LOCAL.set(TEST);

        // Act
        String result = MonoUtils.<String, String>fromFuture(
                TEST_THREAD_LOCAL::get,
                TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set).block();

        // Assert
        assertThat(result).isEqualTo(TEST);
    }

    /**
     * 测试 fromFuture null 上下文传播
     * 测试目的：验证当父线程上下文为 null 时，fromFuture 执行中也能正确接收到 null
     * 测试场景：父线程不设置上下文
     * 验证内容：异步执行时获取到的上下文为 null
     */
    @Test
    public void testFromFutureWithNullContext() {
        // Arrange
        TEST_THREAD_LOCAL.remove();
        AtomicReference<String> capturedContext = new AtomicReference<>("sentinel");

        // Act
        MonoUtils.fromFuture(
                () -> {
                    capturedContext.set(TEST_THREAD_LOCAL.get());
                    return null;
                },
                TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set).block();

        // Assert
        assertThat(capturedContext.get()).isNull();
    }

    /**
     * 测试 fromFuture 异常传播
     * 测试目的：验证 supplier 中的异常能通过 Mono 正确传播
     * 测试场景：supplier 执行时抛出 RuntimeException
     * 验证内容：Mono.block() 抛出包含原始异常的 RuntimeException
     */
    @Test
    public void testFromFuturePropagatesException() {
        // Arrange
        TEST_THREAD_LOCAL.set(TEST);

        // Act & Assert
        assertThatThrownBy(() ->
                MonoUtils.<Void, String>fromFuture(
                        () -> {
                            throw new RuntimeException("future error");
                        },
                        TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set).block()
        ).isInstanceOf(RuntimeException.class);
    }
}
