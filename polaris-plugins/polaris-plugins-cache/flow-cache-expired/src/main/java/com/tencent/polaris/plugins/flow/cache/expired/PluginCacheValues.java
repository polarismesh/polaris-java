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

package com.tencent.polaris.plugins.flow.cache.expired;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class PluginCacheValues {

    private final Map<Object, PluginCacheItem> values = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <T> T getOrCreateValue(Object key, Function<Object, T> createFunc) {
        PluginCacheItem pluginCacheItem = values.get(key);
        if (null != pluginCacheItem) {
            return (T) pluginCacheItem.getValue();
        }
        pluginCacheItem = values.computeIfAbsent(key, new Function<Object, PluginCacheItem>() {

            @Override
            public PluginCacheItem apply(Object o) {
                return new PluginCacheItem(o, createFunc.apply(o));
            }
        });
        return (T) pluginCacheItem.getValue();
    }

    public void expireValues(long expireDuration) {
        long curTimeMs = System.currentTimeMillis();
        for (PluginCacheItem item : values.values()) {
            if (curTimeMs - item.getLastAccessTime() >= expireDuration) {
                values.remove(item.getKey());
            }
        }
    }
}
