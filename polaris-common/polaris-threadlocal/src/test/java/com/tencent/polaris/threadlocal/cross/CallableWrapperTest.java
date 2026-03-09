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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link CallableWrapper}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class CallableWrapperTest {

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
     * 测试上下文跨线程传播
     * 测试目的：验证在父线程设置的上下文能够正确传播到子线程
     * 测试场景：在父线程设置上下文后创建 CallableWrapper，然后在新线程中执行
     * 验证内容：子线程中获取到的上下文值与父线程设置的一致
     */
    @Test
    public void testCallPropagatesContextToNewThread() throws Exception {
        // Arrange
        TEST_THREAD_LOCAL.set(TEST);
        AtomicReference<Boolean> result = new AtomicReference<>(false);
        CallableWrapper<Void, String> callableWrapper = new CallableWrapper<>(
                () -> {
                    result.set(TEST.equals(TEST_THREAD_LOCAL.get()));
                    return null;
                },
                TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set);

        // Act
        FutureTask<Void> futureTask = new FutureTask<>(callableWrapper);
        Thread thread = new Thread(futureTask);
        thread.start();
        thread.join();

        // Assert
        assertThat(result.get()).isTrue();
    }

    /**
     * 测试执行后恢复原上下文
     * 测试目的：验证 call() 执行完成后当前线程的上下文被正确恢复
     * 测试场景：构造时捕获上下文 A，执行前将当前线程上下文改为 B，执行 call() 后检查恢复
     * 验证内容：call() 执行后当前线程上下文为 B（执行前的值），而非 A（捕获的值）
     */
    @Test
    public void testCallRestoresContextAfterExecution() throws Exception {
        // Arrange
        TEST_THREAD_LOCAL.set(OTHER);
        CallableWrapper<Void, String> callableWrapper = new CallableWrapper<>(
                () -> null,
                TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set);
        TEST_THREAD_LOCAL.set(TEST);

        // Act
        callableWrapper.call();

        // Assert
        assertThat(TEST_THREAD_LOCAL.get()).isEqualTo(TEST);
    }

    /**
     * 测试异常时恢复原上下文
     * 测试目的：验证 callable 抛出异常时，当前线程的上下文仍能被正确恢复
     * 测试场景：callable 执行时抛出 RuntimeException
     * 验证内容：1) 异常被正确抛出；2) 上下文被恢复为 call() 执行前的值
     */
    @Test
    public void testCallRestoresContextOnException() throws Exception {
        // Arrange
        TEST_THREAD_LOCAL.set(OTHER);
        CallableWrapper<Void, String> callableWrapper = new CallableWrapper<>(
                () -> {
                    throw new RuntimeException("test exception");
                },
                TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set);
        TEST_THREAD_LOCAL.set(TEST);

        // Act & Assert
        assertThatThrownBy(callableWrapper::call)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("test exception");
        assertThat(TEST_THREAD_LOCAL.get()).isEqualTo(TEST);
    }

    /**
     * 测试 null 上下文的传播
     * 测试目的：验证当父线程上下文为 null 时，子线程也能正确接收到 null 值
     * 测试场景：父线程不设置上下文（即 null），在子线程中检查传播的值
     * 验证内容：子线程中获取到的上下文值为 null
     */
    @Test
    public void testCallWithNullContext() throws Exception {
        // Arrange
        TEST_THREAD_LOCAL.remove();
        AtomicReference<String> capturedContext = new AtomicReference<>("polaris");
        CallableWrapper<Void, String> callableWrapper = new CallableWrapper<>(
                () -> {
                    capturedContext.set(TEST_THREAD_LOCAL.get());
                    return null;
                },
                TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set);

        // Act
        FutureTask<Void> futureTask = new FutureTask<>(callableWrapper);
        Thread thread = new Thread(futureTask);
        thread.start();
        thread.join();

        // Assert
        assertThat(capturedContext.get()).isNull();
    }

    /**
     * 测试返回值正确传递
     * 测试目的：验证 callable 的返回值能通过 call() 正确返回
     * 测试场景：callable 返回当前上下文值
     * 验证内容：call() 的返回值与预期一致
     */
    @Test
    public void testCallReturnsValue() throws Exception {
        // Arrange
        TEST_THREAD_LOCAL.set(TEST);
        CallableWrapper<String, String> callableWrapper = new CallableWrapper<>(
                TEST_THREAD_LOCAL::get,
                TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set);

        // Act
        String result = callableWrapper.call();

        // Assert
        assertThat(result).isEqualTo(TEST);
    }

    /**
     * 测试构造函数传入 null callable 时的断言失败
     * 测试目的：验证构造函数对 callable 参数的非空校验
     * 测试场景：传入 null callable，其余参数正常
     * 验证内容：抛出 AssertionError
     */
    @Test
    public void testConstructorWithNullCallable() {
        // Act & Assert
        assertThatThrownBy(() -> new CallableWrapper<>(null, TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set))
                .isInstanceOf(AssertionError.class);
    }

    /**
     * 测试构造函数传入 null contextGetter 时的断言失败
     * 测试目的：验证构造函数对 contextGetter 参数的非空校验
     * 测试场景：传入 null contextGetter，其余参数正常
     * 验证内容：抛出 AssertionError
     */
    @Test
    public void testConstructorWithNullContextGetter() {
        // Act & Assert
        assertThatThrownBy(() -> new CallableWrapper<>(() -> null, null, TEST_THREAD_LOCAL::set))
                .isInstanceOf(AssertionError.class);
    }

    /**
     * 测试构造函数传入 null contextSetter 时的断言失败
     * 测试目的：验证构造函数对 contextSetter 参数的非空校验
     * 测试场景：传入 null contextSetter，其余参数正常
     * 验证内容：抛出 AssertionError
     */
    @Test
    public void testConstructorWithNullContextSetter() {
        // Act & Assert
        assertThatThrownBy(() -> new CallableWrapper<>(() -> null, TEST_THREAD_LOCAL::get, null))
                .isInstanceOf(AssertionError.class);
    }

    /**
     * 测试多次调用 call() 时上下文始终一致
     * 测试目的：验证 CallableWrapper 的 contextRef 在构造时捕获一次，多次 call() 使用同一快照
     * 测试场景：构造时捕获上下文 TEST，之后修改上下文为 OTHER，多次调用 call() 检查传入的上下文
     * 验证内容：每次 call() 执行时 callable 接收到的上下文都是构造时捕获的 TEST
     */
    @Test
    public void testMultipleCallsUseSameCapturedContext() throws Exception {
        // Arrange
        TEST_THREAD_LOCAL.set(TEST);
        AtomicReference<String> capturedContext = new AtomicReference<>();
        CallableWrapper<Void, String> callableWrapper = new CallableWrapper<>(
                () -> {
                    capturedContext.set(TEST_THREAD_LOCAL.get());
                    return null;
                },
                TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set);

        // Act & Assert - first call
        TEST_THREAD_LOCAL.set(OTHER);
        callableWrapper.call();
        assertThat(capturedContext.get()).isEqualTo(TEST);
        assertThat(TEST_THREAD_LOCAL.get()).isEqualTo(OTHER);

        // Act & Assert - second call with different current context
        TEST_THREAD_LOCAL.set("THIRD");
        callableWrapper.call();
        assertThat(capturedContext.get()).isEqualTo(TEST);
        assertThat(TEST_THREAD_LOCAL.get()).isEqualTo("THIRD");
    }
}
