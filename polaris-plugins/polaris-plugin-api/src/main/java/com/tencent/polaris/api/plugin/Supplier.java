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

package com.tencent.polaris.api.plugin;

import com.tencent.polaris.api.exception.PolarisException;
import java.util.Collection;

/**
 * 插件提供器
 *
 * @author andrewshan
 * @date 2019/8/21
 */
public interface Supplier {

    /**
     * 获取插件实例，获取不到会报异常
     *
     * @param type 插件类型
     * @param name 插件名
     * @return 插件实例
     * @throws PolarisException 获取失败抛出异常
     */
    Plugin getPlugin(PluginType type, String name) throws PolarisException;

    /**
     * 获取可选的插件，获取不到返回null
     *
     * @param type 插件类型
     * @param name 插件名
     * @return 插件实例
     */
    Plugin getOptionalPlugin(PluginType type, String name);

    /**
     * 获取某类型下的所有插件
     *
     * @param type 插件类型
     * @return 插件实例列表
     * @throws PolarisException 获取失败异常
     */
    Collection<Plugin> getPlugins(PluginType type) throws PolarisException;

    /**
     * 获取当前已经加载的所有插件
     * @return 插件实例列表
     * @throws PolarisException 获取失败异常
     */
    Collection<Plugin> getAllPlugins() throws PolarisException;

}
