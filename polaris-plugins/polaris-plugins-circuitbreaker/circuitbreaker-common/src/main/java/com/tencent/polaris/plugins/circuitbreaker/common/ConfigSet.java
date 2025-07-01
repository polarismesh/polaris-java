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
import com.tencent.polaris.api.pojo.StatusDimension;

/**
 * 配置集合
 */
public class ConfigSet<T extends Verifier> {

    private final StatusDimension.Level level;

    private final boolean useDefault;

    private final HalfOpenConfig halfOpenConfig;

    private final T plugConfig;

    public ConfigSet(StatusDimension.Level level, boolean useDefault, HalfOpenConfig halfOpenConfig, T plugConfig) {
        this.level = level;
        this.useDefault = useDefault;
        this.halfOpenConfig = halfOpenConfig;
        this.plugConfig = plugConfig;
    }

    public StatusDimension.Level getLevel() {
        return level;
    }

    public boolean isUseDefault() {
        return useDefault;
    }

    public HalfOpenConfig getHalfOpenConfig() {
        return halfOpenConfig;
    }

    public T getPlugConfig() {
        return plugConfig;
    }
}
