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

import com.tencent.polaris.api.config.configuration.ConfigFilterConfig;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.Supplier;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.configuration.ConfigFile;
import com.tencent.polaris.api.plugin.configuration.ConfigFileResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * FilterChain 过滤器链
 *
 * @author fabian4
 * @date 2023/7/1
 */
public class ConfigFileFilterChain {

    private final Boolean enable;

    private final Map<String, ConfigFileFilter> pluginChain = new HashMap<>();

    public ConfigFileResponse execute(ConfigFile configFile, Function<ConfigFile, ConfigFileResponse> next) {
        if (next == null) {
            throw new PolarisException(ErrorCode.PARAMETER_ERROR, "Exit Function is null");
        }
        if (!enable) {
            return next.apply(configFile);
        }
        for (String plugin : pluginChain.keySet()) {
            Function<ConfigFile, ConfigFileResponse> curr = next;
            next = pluginChain.get(plugin).doFilter(configFile, curr);
        }
        return next.apply(configFile);
    }

    public ConfigFileFilterChain(Supplier supplier, ConfigFilterConfig config) {
        this.enable = config.isEnable();
        if (!config.isEnable()) {
            return;
        }
        config.getChain().forEach(chain ->
                pluginChain.put(chain, (ConfigFileFilter) supplier.getPlugin(PluginTypes.CONFIG_FILTER.getBaseType(), chain))
        );
    }
}
