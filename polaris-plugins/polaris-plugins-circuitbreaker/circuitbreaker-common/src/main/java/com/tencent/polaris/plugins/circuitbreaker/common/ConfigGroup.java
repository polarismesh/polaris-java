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

package com.tencent.polaris.plugins.circuitbreaker.common;

import com.tencent.polaris.api.config.verify.Verifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class ConfigGroup<T extends Verifier> {

    private final ConfigSet<T> localConfig;

    private final Map<RuleIdentifier, ConfigSet<T>> serviceConfigs = new ConcurrentHashMap<>();

    public ConfigGroup(ConfigSet<T> localConfig) {
        this.localConfig = localConfig;
    }

    public ConfigSet<T> getLocalConfig() {
        return localConfig;
    }

    public ConfigSet<T> getServiceConfig(RuleIdentifier ruleIdentifier, Function<RuleIdentifier, ConfigSet<T>> create) {
        ConfigSet<T> tConfigSet = serviceConfigs.computeIfAbsent(ruleIdentifier, create);
        if (tConfigSet.isUseDefault()) {
            return localConfig;
        }
        return tConfigSet;
    }
}
