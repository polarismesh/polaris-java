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
 * Test for {@link RunnableWrapper}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class RunnableWrapperTest {

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
     * 测试场景：在父线程设置上下文后创建 RunnableWrapper，然后在新线程中执行
     * 验证内容：子线程中获取到的上下文值与父线程设置的一致
     */
    @Test
    public void testRunPropagatesContextToNewThread() throws InterruptedException {
        // Arrange
        TEST_THREAD_LOCAL.set(TEST);
        AtomicReference<Boolean> result = new AtomicReference<>(false);
        RunnableWrapper<String> runnableWrapper = new RunnableWrapper<>(
                () -> result.set(TEST.equals(TEST_THREAD_LOCAL.get())),
                TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set);

        // Act
        Thread thread = new Thread(runnableWrapper);
        thread.start();
        thread.join();

        // Assert
        assertThat(result.get()).isTrue();
    }

    /**
     * 测试执行后恢复原上下文
     * 测试目的：验证 run() 执行完成后当前线程的上下文被正确恢复
     * 测试场景：构造时捕获上下文 OTHER，执行前将当前线程上下文改为 TEST，执行 run() 后检查恢复
     * 验证内容：run() 执行后当前线程上下文为 TEST（执行前的值）
     */
    @Test
    public void testRunRestoresContextAfterExecution() {
        // Arrange
        TEST_THREAD_LOCAL.set(OTHER);
        RunnableWrapper<String> runnableWrapper = new RunnableWrapper<>(
                () -> { },
                TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set);
        TEST_THREAD_LOCAL.set(TEST);

        // Act
        runnableWrapper.run();

        // Assert
        assertThat(TEST_THREAD_LOCAL.get()).isEqualTo(TEST);
    }

    /**
     * 测试异常时恢复原上下文
     * 测试目的：验证 runnable 抛出异常时，执行线程的上下文仍能被正确恢复
     * 测试场景：runnable 执行时抛出 RuntimeException
     * 验证内容：1) 异常被正确抛出；2) 上下文在异常后被恢复
     */
    @Test
    public void testRunRestoresContextOnException() throws InterruptedException {
        // Arrange
        TEST_THREAD_LOCAL.set(OTHER);
        RunnableWrapper<String> runnableWrapper = new RunnableWrapper<>(
                () -> {
                    throw new RuntimeException("test exception");
                },
                TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set);
        TEST_THREAD_LOCAL.set(TEST);
        AtomicReference<Throwable> caughtException = new AtomicReference<>();

        // Act
        Thread thread = new Thread(() -> {
            try {
                runnableWrapper.run();
            } catch (RuntimeException ex) {
                caughtException.set(ex);
            }
        });
        thread.start();
        thread.join();

        // Assert
        assertThat(caughtException.get()).isNotNull();
        assertThat(caughtException.get().getMessage()).isEqualTo("test exception");
    }

    /**
     * 测试 null 上下文的传播
     * 测试目的：验证当父线程上下文为 null 时，子线程也能正确接收到 null 值
     * 测试场景：父线程不设置上下文（即 null），在子线程中检查传播的值
     * 验证内容：子线程中获取到的上下文值为 null
     */
    @Test
    public void testRunWithNullContext() throws InterruptedException {
        // Arrange
        TEST_THREAD_LOCAL.remove();
        AtomicReference<String> capturedContext = new AtomicReference<>("polaris");
        RunnableWrapper<String> runnableWrapper = new RunnableWrapper<>(
                () -> capturedContext.set(TEST_THREAD_LOCAL.get()),
                TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set);

        // Act
        Thread thread = new Thread(runnableWrapper);
        thread.start();
        thread.join();

        // Assert
        assertThat(capturedContext.get()).isNull();
    }

    /**
     * 测试构造函数传入 null runnable 时的断言失败
     * 测试目的：验证构造函数对 runnable 参数的非空校验
     * 测试场景：传入 null runnable，其余参数正常
     * 验证内容：抛出 AssertionError
     */
    @Test
    public void testConstructorWithNullRunnable() {
        // Act & Assert
        assertThatThrownBy(() -> new RunnableWrapper<>(null, TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set))
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
        assertThatThrownBy(() -> new RunnableWrapper<>(() -> { }, null, TEST_THREAD_LOCAL::set))
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
        assertThatThrownBy(() -> new RunnableWrapper<>(() -> { }, TEST_THREAD_LOCAL::get, null))
                .isInstanceOf(AssertionError.class);
    }

    /**
     * 测试多次调用 run() 时上下文始终一致
     * 测试目的：验证 RunnableWrapper 的 contextRef 在构造时捕获一次，多次 run() 使用同一快照
     * 测试场景：构造时捕获上下文 TEST，之后修改上下文，多次调用 run() 检查传入的上下文
     * 验证内容：每次 run() 执行时 runnable 接收到的上下文都是构造时捕获的 TEST
     */
    @Test
    public void testMultipleRunsUseSameCapturedContext() {
        // Arrange
        TEST_THREAD_LOCAL.set(TEST);
        AtomicReference<String> capturedContext = new AtomicReference<>();
        RunnableWrapper<String> runnableWrapper = new RunnableWrapper<>(
                () -> capturedContext.set(TEST_THREAD_LOCAL.get()),
                TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set);

        // Act & Assert - first run
        TEST_THREAD_LOCAL.set(OTHER);
        runnableWrapper.run();
        assertThat(capturedContext.get()).isEqualTo(TEST);
        assertThat(TEST_THREAD_LOCAL.get()).isEqualTo(OTHER);

        // Act & Assert - second run with different current context
        TEST_THREAD_LOCAL.set("THIRD");
        runnableWrapper.run();
        assertThat(capturedContext.get()).isEqualTo(TEST);
        assertThat(TEST_THREAD_LOCAL.get()).isEqualTo("THIRD");
    }
}
