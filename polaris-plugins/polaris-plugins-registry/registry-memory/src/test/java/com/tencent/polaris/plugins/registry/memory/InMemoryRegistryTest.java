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

import com.google.protobuf.Message;
import com.tencent.polaris.api.plugin.registry.CacheHandler;
import com.tencent.polaris.api.plugin.registry.EventCompleteNotifier;
import com.tencent.polaris.api.plugin.server.ServerConnector;
import com.tencent.polaris.api.pojo.RegistryCacheValue;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.pojo.ServiceKey;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link InMemoryRegistry}.
 *
 * @author hewei
 */
@RunWith(MockitoJUnitRunner.class)
public class InMemoryRegistryTest {

    private static final String TEST_NAMESPACE = "TestNamespace";

    private static final String TEST_SERVICE = "TestService";

    @Mock
    private ServerConnector connector;

    @Mock
    private MessagePersistHandler messagePersistHandler;

    @Mock
    private CacheHandler cacheHandler;

    @InjectMocks
    private InMemoryRegistry registry;

    private ServiceEventKey svcEventKey;

    @Before
    public void setUp() {
        svcEventKey = new ServiceEventKey(new ServiceKey(TEST_NAMESPACE, TEST_SERVICE), EventType.INSTANCE);
    }

    /**
     * 测试目的：验证以 null 调用 loadRemoteValue 不会抛出 NPE。
     * 测试场景：resourceMap 中已存在"远程已更新且资源就绪"的 CacheObject，
     * 对应"磁盘容灾命中且已远程更新"分支中 loadRemoteValue(key, null) 的调用。
     * 验证内容：loadRemoteValue(key, null) 不抛异常，且正常触发往 serverConnector 注册。
     */
    @Test
    public void testLoadRemoteValueWithNullNotifierShouldNotThrow() throws Exception {
        // Arrange：预置一个"远程已更新且资源就绪"的 CacheObject
        setPrivateField(registry, "persistEnable", true);
        Map<ServiceEventKey, CacheObject> resourceMap = getPrivateField(registry, "resourceMap");
        resourceMap.put(svcEventKey, buildRemoteUpdatedCacheObject());

        // Act & Assert：以 null 调用 loadRemoteValue 不应抛 NPE
        assertThatCode(() -> invokeLoadRemoteValue(svcEventKey, null)).doesNotThrowAnyException();

        // 应正常触发往 serverConnector 注册
        verify(connector).registerServiceHandler(any());
    }

    /**
     * 测试目的：复现"磁盘容灾命中且并发远程更新"竞态分支，验证不会抛 NPE。
     * 测试场景：persistEnable=true 且磁盘容灾文件可读；在 loadFileCache 读取磁盘期间，
     * 另一线程已把该资源远程更新并放入 resourceMap，使 isRemoteUpdated() 返回 true，
     * 从而进入以 null 调用 loadRemoteValue 的分支。修复前会在 checkNotifyNow 中触发 NPE。
     * 验证内容：loadResources 不抛 NPE，且原始 notifier 被正常回调 complete。
     */
    @Test
    public void testLoadResourcesWithRemoteUpdatedFileCacheShouldNotThrowNPE() throws Exception {
        // Arrange
        setPrivateField(registry, "persistEnable", true);
        Map<ServiceEventKey, CacheObject> resourceMap = getPrivateField(registry, "resourceMap");
        CacheObject remoteUpdatedCache = buildRemoteUpdatedCacheObject();
        Message message = mock(Message.class);
        // 模拟竞态：读取磁盘文件时，另一线程已完成远程更新并将对象放入 resourceMap，
        // 因此后续 addOrGetCacheObject 的 computeIfAbsent 会直接命中这个"远程已更新"的对象
        when(messagePersistHandler.loadPersistedServices(any(), any())).thenAnswer(invocation -> {
            resourceMap.put(svcEventKey, remoteUpdatedCache);
            return message;
        });
        EventCompleteNotifier notifier = mock(EventCompleteNotifier.class);

        // Act & Assert：走"磁盘容灾命中且已远程更新"分支，内部以 null 调用 loadRemoteValue，不应抛 NPE
        assertThatCode(() -> invokeLoadResources(svcEventKey, notifier)).doesNotThrowAnyException();

        // 原始 notifier 应被正常回调
        verify(notifier).complete(svcEventKey);
    }

    /**
     * 构造一个"远程已更新且资源就绪"的 CacheObject。
     *
     * @return CacheObject 实例
     */
    private CacheObject buildRemoteUpdatedCacheObject() throws Exception {
        CacheObject cacheObject = new CacheObject(cacheHandler, svcEventKey, registry);
        RegistryCacheValue cacheValue = mock(RegistryCacheValue.class);
        lenient().when(cacheValue.isInitialized()).thenReturn(true);
        lenient().when(cacheValue.isLoadedFromFile()).thenReturn(false);
        AtomicReference<RegistryCacheValue> valueRef = getPrivateField(cacheObject, "value");
        valueRef.set(cacheValue);
        AtomicBoolean remoteUpdated = getPrivateField(cacheObject, "remoteUpdated");
        remoteUpdated.set(true);
        return cacheObject;
    }

    private void invokeLoadRemoteValue(ServiceEventKey key, EventCompleteNotifier notifier) throws Throwable {
        Method method = InMemoryRegistry.class.getDeclaredMethod("loadRemoteValue",
                ServiceEventKey.class, EventCompleteNotifier.class);
        method.setAccessible(true);
        try {
            method.invoke(registry, key, notifier);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    private void invokeLoadResources(ServiceEventKey key, EventCompleteNotifier notifier) throws Throwable {
        Method method = InMemoryRegistry.class.getDeclaredMethod("loadResources",
                ServiceEventKey.class, EventCompleteNotifier.class);
        method.setAccessible(true);
        try {
            method.invoke(registry, key, notifier);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T getPrivateField(Object object, String fieldName) throws Exception {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(object);
    }

    private static void setPrivateField(Object object, String fieldName, Object value) throws Exception {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(object, value);
    }
}
