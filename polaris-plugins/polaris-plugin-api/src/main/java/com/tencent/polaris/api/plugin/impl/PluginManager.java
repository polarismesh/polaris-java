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

package com.tencent.polaris.api.plugin.impl;

import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.IdAwarePlugin;
import com.tencent.polaris.api.plugin.Manager;
import com.tencent.polaris.api.plugin.Plugin;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.utils.MapUtils;
import com.tencent.polaris.api.utils.StringUtils;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;


/**
 * 插件管理器，承载当前API的插件实例列表
 *
 * @author andrewshan
 * @date 2019/8/21
 */
public class PluginManager implements Manager {

    private final Map<PluginType, Map<String, Plugin>> typedPlugins = new HashMap<>();

    private final List<PluginType> pluginTypes;

    public PluginManager(List<PluginType> pluginTypes) {
        Collections.sort(pluginTypes);
        this.pluginTypes = pluginTypes;
    }

    @Override
    public void initPlugins(InitContext context) throws PolarisException {
        //先实例化所有插件
        int baseId = 0;
        for (PluginType pluginType : pluginTypes) {
            Map<String, Plugin> plugins = new HashMap<>();
            typedPlugins.put(pluginType, plugins);
            ServiceLoader<? extends Plugin> loader = ServiceLoader.load(pluginType.getClazz());
            for (Plugin plugin : loader) {
                baseId++;
                String name = plugin.getName();
                if (StringUtils.isBlank(name) || plugins.containsKey(name)) {
                    throw new PolarisException(ErrorCode.PLUGIN_ERROR,
                            String.format("duplicated name for plugin(name=%s, type=%s)", name, pluginType));
                }
                if (plugin instanceof IdAwarePlugin) {
                    ((IdAwarePlugin) plugin).setId(baseId);
                }
                plugins.put(name, plugin);
            }
        }
        //再进行初始化
        for (PluginType pluginType : pluginTypes) {
            Map<String, Plugin> plugins = typedPlugins.get(pluginType);
            for (Map.Entry<String, Plugin> pluginEntry : plugins.entrySet()) {
                pluginEntry.getValue().init(context);
            }
        }
    }

    /**
     * 在应用上下文初始化完毕后进行调用
     *
     * @param extensions 插件实例
     * @throws PolarisException 运行异常
     */
    public void postContextInitPlugins(Extensions extensions) throws PolarisException {
        for (PluginType pluginType : pluginTypes) {
            Map<String, Plugin> plugins = typedPlugins.get(pluginType);
            if (MapUtils.isEmpty(plugins)) {
                continue;
            }
            for (Map.Entry<String, Plugin> pluginEntry : plugins.entrySet()) {
                Plugin plugin = pluginEntry.getValue();
                plugin.postContextInit(extensions);
            }
        }
    }

    /**
     * 销毁已初始化的插件列表
     */
    @Override
    public void destroyPlugins() {
        //倒序遍历
        for (int i = pluginTypes.size() - 1; i >= 0; i--) {
            PluginType pluginType = pluginTypes.get(i);
            Map<String, Plugin> plugins = typedPlugins.get(pluginType);
            if (MapUtils.isEmpty(plugins)) {
                continue;
            }
            for (Map.Entry<String, Plugin> plugin : plugins.entrySet()) {
                plugin.getValue().destroy();
            }
        }
    }


    @Override
    public Plugin getPlugin(PluginType type, String name) throws PolarisException {
        if (!typedPlugins.containsKey(type)) {
            throw new PolarisException(ErrorCode.PLUGIN_ERROR, String.format("plugins type(type=%s) not found", type));
        }
        Map<String, Plugin> plugins = typedPlugins.get(type);
        if (!plugins.containsKey(name)) {
            throw new PolarisException(ErrorCode.PLUGIN_ERROR,
                    String.format("plugin(name=%s, type=%s) not found", name, type));
        }
        return plugins.get(name);
    }

    @Override
    public Collection<Plugin> getPlugins(PluginType type) throws PolarisException {
        if (!typedPlugins.containsKey(type)) {
            throw new PolarisException(ErrorCode.PLUGIN_ERROR, String.format("plugins type(type=%s) not found", type));
        }
        Map<String, Plugin> plugins = typedPlugins.get(type);
        return plugins.values();
    }
}
