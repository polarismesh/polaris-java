/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.polaris.plugins.flow.cache.expired;

import com.tencent.polaris.api.config.global.FlowCacheConfig;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.cache.FlowCache;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.utils.ThreadPoolUtils;
import com.tencent.polaris.client.util.NamedThreadFactory;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Pattern;

@SuppressWarnings("unchecked")
public class ExpiredFlowCache implements FlowCache {

    private final Map<String, PatternCacheItem> regexPatterns = new ConcurrentHashMap<>();

    private final ThreadLocal<Map<Class<?>, Queue<Object>>> threadLocalValue = new ThreadLocal<>();

    private final Map<Integer, PluginCacheValues> pluginValues = new ConcurrentHashMap<>();

    private ScheduledExecutorService expireExecutor;

    private long expireIntervalMs;

    private boolean enable;

    @Override
    public Pattern loadOrStoreCompiledRegex(String regexStr) {
        if (!enable) {
            return Pattern.compile(regexStr);
        }
        PatternCacheItem patternCacheItem = regexPatterns
                .computeIfAbsent(regexStr, s -> new PatternCacheItem(Pattern.compile(s)));
        patternCacheItem.updateLastAccessTimeMs();
        return patternCacheItem.getPattern();
    }

    private <T> T createObject(Class<T> clazz) {
        try {
            return clazz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("fail to create object with type " + clazz.getCanonicalName(), e);
        }
    }

    @Override
    public <T> T borrowThreadCacheObject(Class<T> clazz) {
        if (!enable) {
            return createObject(clazz);
        }
        Map<Class<?>, Queue<Object>> classQueueMap = threadLocalValue.get();
        if (null == classQueueMap) {
            classQueueMap = new WeakHashMap<>();
            threadLocalValue.set(classQueueMap);
        }
        Queue<Object> objects = classQueueMap.computeIfAbsent(clazz, k -> new LinkedList<>());
        T object = (T) objects.poll();
        if (null != object) {
            return object;
        }
        return createObject(clazz);
    }

    @Override
    public <T> void giveBackThreadCacheObject(T object) {
        if (!enable) {
            return;
        }
        Class<?> clazz = object.getClass();
        Map<Class<?>, Queue<Object>> classQueueMap = threadLocalValue.get();
        if (null == classQueueMap) {
            classQueueMap = new WeakHashMap<>();
            threadLocalValue.set(classQueueMap);
        }
        Queue<Object> objects = classQueueMap.computeIfAbsent(clazz, k -> new LinkedList<>());
        objects.offer(object);
    }

    @Override
    public <T> T loadPluginCacheObject(int pluginIdx, Object key, Function<Object, T> createFunc) {
        PluginCacheValues cacheValues = pluginValues.get(pluginIdx);
        if (null == cacheValues) {
            cacheValues = pluginValues.computeIfAbsent(pluginIdx,
                    idx -> new PluginCacheValues());
        }
        return cacheValues.getOrCreateValue(key, createFunc);
    }

    @Override
    public String getName() {
        return FlowCacheConfig.DEFAULT_FLOW_CACHE_NAME;
    }

    @Override
    public PluginType getType() {
        return PluginTypes.FLOW_CACHE.getBaseType();
    }

    @Override
    public void init(InitContext ctx) throws PolarisException {
        enable = ctx.getConfig().getGlobal().getSystem().getFlowCache().isEnable();
        expireIntervalMs = ctx.getConfig().getGlobal().getSystem().getFlowCache().getExpireInterval();
        expireExecutor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory(getName()));
    }

    @Override
    public void postContextInit(Extensions ctx) throws PolarisException {
        expireExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                for (PluginCacheValues pluginCacheValues : pluginValues.values()) {
                    pluginCacheValues.expireValues(expireIntervalMs);
                }
            }
        }, expireIntervalMs, expireIntervalMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void destroy() {
        ThreadPoolUtils.waitAndStopThreadPools(new ExecutorService[]{expireExecutor});
    }
}
