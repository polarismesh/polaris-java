/*
 * Tencent is pleased to support the open source community by making Polaris available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
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
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.registry.CacheHandler;
import com.tencent.polaris.api.plugin.registry.CacheHandler.CachedStatus;
import com.tencent.polaris.api.plugin.registry.EventCompleteNotifier;
import com.tencent.polaris.api.plugin.registry.ResourceEventListener;
import com.tencent.polaris.api.plugin.server.EventHandler;
import com.tencent.polaris.api.plugin.server.ServerEvent;
import com.tencent.polaris.api.pojo.RegistryCacheValue;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.client.pojo.ServiceInstancesByProto;
import com.tencent.polaris.client.pojo.ServiceRuleByProto;
import com.tencent.polaris.client.pojo.ServicesByProto;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 服务缓存的封装类型
 *
 * @author andrewshan
 * @date 2019/8/22
 */
public class CacheObject implements EventHandler {

    public static final ServicesByProto EMPTY_SERVICE = new ServicesByProto();
    public static final ServiceInstancesByProto EMPTY_INSTANCES = new ServiceInstancesByProto();
    public static final ServiceRuleByProto EMPTY_SERVICE_RULE = new ServiceRuleByProto();

    private static final Logger LOG = LoggerFactory.getLogger(CacheObject.class);

    private final AtomicReference<RegistryCacheValue> value = new AtomicReference<>();

    private final ServiceEventKey svcEventKey;

    private final CacheHandler cacheHandler;

    private final InMemoryRegistry registry;

    //用于守护notifier的变更
    private final Object lock = new Object();

    private final List<EventCompleteNotifier> notifiers = new ArrayList<>();

    private final AtomicLong lastAccessTimeMs = new AtomicLong(0);

    private final long createTime;

    //是否经过远程更新
    private final AtomicBoolean remoteUpdated = new AtomicBoolean(false);

    //是否已经注册了connector监听
    private final AtomicBoolean registered = new AtomicBoolean(false);

    //这个服务对象是否已经被删除，防止connector收到多次服务不存在的消息，导致重复删除
    private final AtomicBoolean deleted = new AtomicBoolean(false);

    //是否已经触发了资源新增回调
    private final AtomicBoolean notifyResourceAdded = new AtomicBoolean(false);

    public CacheObject(CacheHandler cacheHandler, ServiceEventKey svcEventKey, InMemoryRegistry registry) {
        this.svcEventKey = svcEventKey;
        this.registry = registry;
        this.cacheHandler = cacheHandler;
        long nowMs = System.currentTimeMillis();
        createTime = nowMs;
        setLastAccessTimeMs(nowMs);
    }

    public CacheObject(CacheHandler cacheHandler, ServiceEventKey svcEventKey, InMemoryRegistry registry,
            Message initValue) {
        this.svcEventKey = svcEventKey;
        this.registry = registry;
        this.cacheHandler = cacheHandler;
        long nowMs = System.currentTimeMillis();
        createTime = nowMs;
        setLastAccessTimeMs(nowMs);
        RegistryCacheValue registryCacheValue = cacheHandler.messageToCacheValue(null, initValue, true);
        value.set(registryCacheValue);
    }

    /**
     * 创建时间
     *
     * @return long
     */
    public long getCreateTime() {
        return createTime;
    }

    /**
     * 返回是否已经经过了远程更新
     *
     * @return remoteUpdated
     */
    public boolean isRemoteUpdated() {
        return remoteUpdated.get();
    }

    /**
     * 通知并清理所有的监听器
     *
     * @param error 异常
     */
    private void notifyEvent(Throwable error) {
        if (CollectionUtils.isEmpty(notifiers)) {
            return;
        }
        notifiers.forEach(notifier -> {
            if (null != error) {
                notifier.completeExceptionally(svcEventKey, error);
            } else {
                notifier.complete(svcEventKey);
            }
        });
        notifiers.clear();
    }

    /**
     * 增加监听器
     *
     * @param notifier 实例事件监听器
     */
    public void addNotifier(EventCompleteNotifier notifier) {
        if (checkNotifyNow(notifier)) {
            return;
        }
        synchronized (lock) {
            if (checkNotifyNow(notifier)) {
                return;
            }
            notifiers.add(notifier);
        }
    }

    /**
     * 加载缓存值
     *
     * @param updateVisitTime 是否更新访问时间
     * @return 值对象
     */
    public RegistryCacheValue loadValue(boolean updateVisitTime) {
        if (updateVisitTime) {
            lastAccessTimeMs.set(System.currentTimeMillis());
        }
        RegistryCacheValue registryCacheValue = value.get();
        if (null == registryCacheValue) {
            return null;
        }
        if (notifyResourceAdded.compareAndSet(false, true)) {
            Collection<ResourceEventListener> resourceEventListeners = registry.getResourceEventListeners();
            if (!CollectionUtils.isEmpty(resourceEventListeners)) {
                for (ResourceEventListener listener : resourceEventListeners) {
                    listener.onResourceAdd(svcEventKey, registryCacheValue);
                }
            }
        }
        return registryCacheValue;
    }

    @Override
    public boolean onEventUpdate(ServerEvent event) {
        ServiceEventKey serviceEventKey = event.getServiceEventKey();
        PolarisException error = event.getError();
        remoteUpdated.set(true);
        boolean svcDeleted = false;
        Collection<ResourceEventListener> resourceEventListeners = registry.getResourceEventListeners();
        if (null != error) {
            //收取消息有出错
            RegistryCacheValue registryCacheValue = loadValue(false);
            //没有服务信息直接删除
            if (error.getCode() == ErrorCode.SERVICE_NOT_FOUND) {
                if (deleted.compareAndSet(false, true)) {
                    registry.removeCache(serviceEventKey);
                    for (ResourceEventListener listener : resourceEventListeners) {
                        listener.onResourceDeleted(svcEventKey, registryCacheValue);
                    }
                    svcDeleted = true;
                }
            } else {
                LOG.error(String.format("received error notify for service %s", serviceEventKey), error);
            }
        } else {
            Object message = event.getValue();
            RegistryCacheValue cachedValue = value.get();
            CachedStatus cachedStatus = cacheHandler.compareMessage(cachedValue, message);
            if (cachedStatus == CachedStatus.CacheChanged || cachedStatus == CachedStatus.CacheNotExists) {
                LOG.info("OnServiceUpdate: cache {} is pending to update", svcEventKey);
                this.registry.saveMessageToFile(serviceEventKey, (Message) message);
                RegistryCacheValue newCachedValue = cacheHandler.messageToCacheValue(cachedValue, message, false);
                setValue(newCachedValue);
                if (cachedStatus == CachedStatus.CacheChanged) {
                    for (ResourceEventListener listener : resourceEventListeners) {
                        listener.onResourceUpdated(svcEventKey, cachedValue, newCachedValue);
                    }
                }
            } else if (cachedStatus == CachedStatus.CacheEmptyButNoData) {
                LOG.error("OnServiceUpdate: {} is empty, but discover returns no data", svcEventKey);
            }
            boolean newRemoteCache = null == cachedValue || cachedValue.isLoadedFromFile();
            if (newRemoteCache && serviceEventKey.getEventType() == EventType.INSTANCE) {
                //设置就绪状态
                registry.setServerServiceReady(serviceEventKey);
            }
        }
        synchronized (lock) {
            if (error != null && ErrorCode.SERVICE_NOT_FOUND.equals(error.getCode())) {
                notifyEvent(null);
            }
            notifyEvent(error);
        }
        return svcDeleted;
    }

    private void setValue(RegistryCacheValue registryCacheValue) {
        value.set(registryCacheValue);
        LOG.info("CacheObject: value for {} is updated, revision {}", svcEventKey, registryCacheValue.getRevision());
    }

    //发起注册，只要一个能够发起成功
    boolean startRegister() {
        synchronized (lock) {
            return registered.compareAndSet(false, true);
        }
    }

    //恢复未注册状态
    void resumeUnRegistered(PolarisException e) {
        synchronized (lock) {
            registered.compareAndSet(true, false);
            notifyEvent(e);
        }
    }

    private boolean checkResourceAvailable() {
        RegistryCacheValue registryCacheValue = value.get();
        if (null == registryCacheValue) {
            return false;
        }
        return registryCacheValue.isInitialized() && !registryCacheValue.isLoadedFromFile();
    }

    private boolean checkNotifyNow(EventCompleteNotifier notifier) {
        if (checkResourceAvailable()) {
            notifier.complete(svcEventKey);
            return true;
        }
        return false;
    }

    /**
     * 获取最近一次的访问时间
     *
     * @return long
     */
    public long getLastAccessTimeMs() {
        return lastAccessTimeMs.get();
    }

    /**
     * 设置最近一次的访问时间
     *
     * @param value 访问时间值，单位毫秒
     */
    public void setLastAccessTimeMs(long value) {
        lastAccessTimeMs.set(value);
    }

    /**
     * 获取资源标识
     *
     * @return 资源标识
     */
    public ServiceEventKey getServiceEventKey() {
        return svcEventKey;
    }
}
