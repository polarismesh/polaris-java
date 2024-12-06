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

package com.tencent.polaris.api.plugin.registry;

import com.tencent.polaris.api.plugin.cache.FlowCache;
import com.tencent.polaris.api.pojo.RegistryCacheValue;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;

/**
 * 缓存处理操作对象，为不同的类型的缓存提供处理能力
 */
public interface CacheHandler {

    enum CachedStatus {
        /**
         * cache不存在
         */
        CacheNotExists,
        /**
         * cache发生变更
         */
        CacheChanged,
        /**
         * cache未发生变更
         */
        CacheNotChanged,
        /**
         * cache是空的，但是server没有返回data
         */
        CacheEmptyButNoData
    }

    /**
     * 获取缓存类型
     *
     * @return 缓存类型
     */
    EventType getTargetEventType();

    /**
     * 比较缓存值是否发生变更
     *
     * @param oldValue 旧值
     * @param newValue 新值
     * @return 状态
     */
    CachedStatus compareMessage(RegistryCacheValue oldValue, Object newValue);

    /**
     * 将服务端原始消息转换为缓存对象
     *
     * @param oldValue      旧值
     * @param newValue      新原始消息
     * @param isCacheLoaded 是否从本地缓存加载
     * @param flowCache     缓存
     * @return 新缓存值
     */
    RegistryCacheValue messageToCacheValue(RegistryCacheValue oldValue, Object newValue, boolean isCacheLoaded, FlowCache flowCache);

}

