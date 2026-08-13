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

package com.tencent.polaris.configuration.api.core;

import java.util.List;

/**
 * 单个 key 的配置生效详情，聚合 SDK 侧文件原始值与上层框架解析出的生效值。
 *
 * @author evelynwei
 */
public class ConfigKeyEffectiveEntry {

    /**
     * 配置键。
     */
    private final String key;

    /**
     * SDK 从服务端拉到的、该文件内该 key 的原始值。
     */
    private final String fileValue;

    /**
     * 上层框架解析出的最终生效值，未接入或解析失败为 null。
     */
    private final String effectiveValue;

    /**
     * 生效值来源，未接入或解析失败为 null。
     */
    private final String propertySource;

    /**
     * 其他被监听文件中同名 key 的冲突上下文，无冲突为空列表。
     */
    private final List<ConfigKeyConflict> conflicts;

    public ConfigKeyEffectiveEntry(String key, String fileValue, String effectiveValue, String propertySource,
            List<ConfigKeyConflict> conflicts) {
        this.key = key;
        this.fileValue = fileValue;
        this.effectiveValue = effectiveValue;
        this.propertySource = propertySource;
        this.conflicts = conflicts;
    }

    public String getKey() {
        return key;
    }

    public String getFileValue() {
        return fileValue;
    }

    public String getEffectiveValue() {
        return effectiveValue;
    }

    public String getPropertySource() {
        return propertySource;
    }

    public List<ConfigKeyConflict> getConflicts() {
        return conflicts;
    }
}
