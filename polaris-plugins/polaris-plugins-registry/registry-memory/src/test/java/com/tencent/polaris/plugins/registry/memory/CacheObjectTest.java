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

package com.tencent.polaris.plugins.registry.memory;

import com.tencent.polaris.api.plugin.registry.CacheHandler;
import com.tencent.polaris.api.plugin.registry.EventCompleteNotifier;
import com.tencent.polaris.api.pojo.RegistryCacheValue;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.pojo.ServiceKey;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link CacheObject}.
 *
 * @author hewei
 */
@RunWith(MockitoJUnitRunner.class)
public class CacheObjectTest {

    private static final String TEST_NAMESPACE = "TestNamespace";

    private static final String TEST_SERVICE = "TestService";

    @Mock
    private CacheHandler cacheHandler;

    @Mock
    private InMemoryRegistry registry;

    private ServiceEventKey svcEventKey;

    @Before
    public void setUp() {
        svcEventKey = new ServiceEventKey(new ServiceKey(TEST_NAMESPACE, TEST_SERVICE), EventType.INSTANCE);
    }

    /**
     * 测试目的：验证资源已就绪时，向 addNotifier 传入 null 不会抛出 NPE。
     * 测试场景：磁盘容灾命中且并发远程更新后，loadRemoteValue 会以 null 调用 addNotifier，
     * 此时 checkResourceAvailable() 为 true，修复前会在 checkNotifyNow 中触发 NPE。
     * 验证内容：addNotifier(null) 不抛异常，且不会将 null 加入监听列表。
     */
    @Test
    public void testAddNotifierWithNullWhenResourceAvailableShouldNotThrow() throws Exception {
        // Arrange：构造一个资源已就绪（已初始化且非来自文件）的 CacheObject
        RegistryCacheValue cacheValue = mock(RegistryCacheValue.class);
        lenient().when(cacheValue.isInitialized()).thenReturn(true);
        lenient().when(cacheValue.isLoadedFromFile()).thenReturn(false);
        CacheObject cacheObject = newCacheObject(cacheValue);

        // Act & Assert：传入 null 不应抛出 NPE
        assertThatCode(() -> cacheObject.addNotifier(null)).doesNotThrowAnyException();

        // 监听列表应保持为空，null 不应被加入
        List<EventCompleteNotifier> notifiers = getPrivateField(cacheObject, "notifiers");
        assertThat(notifiers).isEmpty();
    }

    /**
     * 测试目的：验证资源未就绪时，向 addNotifier 传入 null 同样不会抛异常。
     * 测试场景：CacheObject 尚无任何缓存值。
     * 验证内容：addNotifier(null) 不抛异常，监听列表为空。
     */
    @Test
    public void testAddNotifierWithNullWhenResourceNotAvailableShouldNotThrow() throws Exception {
        // Arrange：构造一个没有缓存值的 CacheObject
        CacheObject cacheObject = newCacheObject(null);

        // Act & Assert：传入 null 不应抛异常
        assertThatCode(() -> cacheObject.addNotifier(null)).doesNotThrowAnyException();

        List<EventCompleteNotifier> notifiers = getPrivateField(cacheObject, "notifiers");
        assertThat(notifiers).isEmpty();
    }

    /**
     * 测试目的：验证资源已就绪时，传入有效监听器会立即回调 complete 且不加入列表。
     * 测试场景：缓存值已初始化且非来自文件。
     * 验证内容：notifier.complete 被调用一次，监听列表为空（保持原有行为不变）。
     */
    @Test
    public void testAddNotifierWithValueWhenResourceAvailableShouldCompleteNow() throws Exception {
        // Arrange
        RegistryCacheValue cacheValue = mock(RegistryCacheValue.class);
        when(cacheValue.isInitialized()).thenReturn(true);
        when(cacheValue.isLoadedFromFile()).thenReturn(false);
        CacheObject cacheObject = newCacheObject(cacheValue);
        EventCompleteNotifier notifier = mock(EventCompleteNotifier.class);

        // Act
        cacheObject.addNotifier(notifier);

        // Assert：立即回调且不加入监听列表
        verify(notifier).complete(svcEventKey);
        List<EventCompleteNotifier> notifiers = getPrivateField(cacheObject, "notifiers");
        assertThat(notifiers).isEmpty();
    }

    /**
     * 测试目的：验证资源未就绪时，传入有效监听器会被加入监听列表且不立即回调。
     * 测试场景：CacheObject 尚无缓存值。
     * 验证内容：notifier 被加入列表，complete 未被调用（保持原有行为不变）。
     */
    @Test
    public void testAddNotifierWithValueWhenResourceNotAvailableShouldAddToList() throws Exception {
        // Arrange
        CacheObject cacheObject = newCacheObject(null);
        EventCompleteNotifier notifier = mock(EventCompleteNotifier.class);

        // Act
        cacheObject.addNotifier(notifier);

        // Assert：加入监听列表，等待后续远程更新回调
        verify(notifier, never()).complete(any());
        List<EventCompleteNotifier> notifiers = getPrivateField(cacheObject, "notifiers");
        assertThat(notifiers).containsExactly(notifier);
    }

    /**
     * 构造一个 CacheObject，并通过反射设置其缓存值。
     *
     * @param cacheValue 缓存值，可为 null
     * @return CacheObject 实例
     */
    private CacheObject newCacheObject(RegistryCacheValue cacheValue) throws Exception {
        CacheObject cacheObject = new CacheObject(cacheHandler, svcEventKey, registry);
        AtomicReference<RegistryCacheValue> valueRef = getPrivateField(cacheObject, "value");
        valueRef.set(cacheValue);
        return cacheObject;
    }

    @SuppressWarnings("unchecked")
    private static <T> T getPrivateField(Object object, String fieldName) throws Exception {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(object);
    }
}
