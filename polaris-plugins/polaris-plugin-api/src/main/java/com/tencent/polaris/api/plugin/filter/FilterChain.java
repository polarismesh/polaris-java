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

package com.tencent.polaris.api.plugin.filter;

import com.tencent.polaris.api.plugin.Plugin;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.configuration.ConfigFile;
import com.tencent.polaris.api.plugin.configuration.ConfigFileResponse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;

/**
 * FilterChain 过滤器链
 *
 * @author fabian4
 * @date 2023/6/13
 */
public class FilterChain {

    private ArrayList<ConfigFileFilter> chain = new ArrayList<>();

    public ConfigFileResponse excute(ConfigFile configFile, Function<ConfigFile, ConfigFileResponse> next) {
        for (ConfigFileFilter configFileFilter : chain) {
            Function<ConfigFile, ConfigFileResponse> curr = next;
            next = configFileFilter.doFilter(configFile, curr);
        }
        return next.apply(configFile);
    }

    public FilterChain(Collection<Plugin> plugins) {
        plugins.forEach(plugin -> {
            if (plugin.getType() == PluginTypes.CONFIG_FILTER.getBaseType()) {
                chain.add((ConfigFileFilter) plugin);
            }
        });
    }
}