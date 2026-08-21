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

/**
 * 配置生效值，承载某个 key 的文件原始值、运行时最终生效值及其来源。
 *
 * @author evelynwei
 */
public class EffectiveValue {

    /**
     * SDK 从服务端拉到的、该文件内该 key 的原始值。
     */
    private final String fileValue;

    /**
     * Spring Environment 等运行时解析出的最终值；未接入上层框架或解析失败时为 null。
     */
    private final String effectiveValue;

    /**
     * 生效值的来源（如 Spring 的 PropertySource 名）；未接入上层框架或解析失败时为 null。
     */
    private final String propertySource;

    public EffectiveValue(String fileValue, String effectiveValue, String propertySource) {
        this.fileValue = fileValue;
        this.effectiveValue = effectiveValue;
        this.propertySource = propertySource;
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
}
