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

    /**
     * 生效值实际来源的配置文件坐标，供 SDK 反查该来源文件是否为加密配置。
     * <p>
     * 与 {@link #propertySource} 的区别：后者是给人看的来源标识（字符串），前者是给 SDK 判定用的
     * 结构化坐标，仅在客户端内部使用，不进入上报报文。
     * <p>
     * 来源不是 polaris 配置文件（环境变量、本地配置文件等）或采集侧无法归因时为 null，
     * 此时 SDK 退回保守策略。
     */
    private final ConfigFileMetadata sourceFile;

    /**
     * 兼容旧采集侧的构造器，来源坐标视为未知。
     *
     * @param fileValue 文件内原始值
     * @param effectiveValue 运行时生效值
     * @param propertySource 生效值来源标识
     */
    public EffectiveValue(String fileValue, String effectiveValue, String propertySource) {
        this(fileValue, effectiveValue, propertySource, null);
    }

    /**
     * 全量构造器。
     *
     * @param fileValue 文件内原始值
     * @param effectiveValue 运行时生效值
     * @param propertySource 生效值来源标识
     * @param sourceFile 生效值来源的配置文件坐标，无法归因时为 null
     */
    public EffectiveValue(String fileValue, String effectiveValue, String propertySource,
            ConfigFileMetadata sourceFile) {
        this.fileValue = fileValue;
        this.effectiveValue = effectiveValue;
        this.propertySource = propertySource;
        this.sourceFile = sourceFile;
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

    public ConfigFileMetadata getSourceFile() {
        return sourceFile;
    }
}
