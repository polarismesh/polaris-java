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

package com.tencent.polaris.api.plugin.cache;

import com.tencent.polaris.api.plugin.Plugin;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * 流程缓存管理器
 */
public interface FlowCache extends Plugin {

    /**
     * 获取
     *
     * @param regexStr 正则表达式原始字符
     * @return 编译正则后的结果
     */
    Pattern loadOrStoreCompiledRegex(String regexStr);

    /**
     * 借取缓存对象
     *
     * @param clazz 缓存类型
     * @param <T> 类型
     * @return 缓存对象
     */
    <T> T borrowThreadCacheObject(Class<T> clazz);

    /**
     * 归还缓存对象
     *
     * @param object 缓存对象
     * @param <T> 类型
     */
    <T> void giveBackThreadCacheObject(T object);

    /**
     * 加载插件缓存，提供函数，当不存在时候进行构建
     *
     * @param pluginIdx 插件ID
     * @param key 缓存键
     * @param createFunc 创建缓存值的工厂方法
     * @param <T> 缓存值类型
     * @return 缓存值
     */
    <T> T loadPluginCacheObject(int pluginIdx, Object key, Function<Object, T> createFunc);
}
