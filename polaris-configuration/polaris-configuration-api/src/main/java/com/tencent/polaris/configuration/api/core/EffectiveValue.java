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
     * 仅当 {@link #sourceKind} 为 {@link SourceKind#POLARIS_FILE} 时有值。
     */
    private final ConfigFileMetadata sourceFile;

    /**
     * 生效值来源的归因结论。见 {@link SourceKind}。
     */
    private final SourceKind sourceKind;

    /**
     * 生效值来源的归因结论。SDK 据此决定是否要为「来源可能是加密配置」而省略生效值。
     * <p>
     * 三态而非「坐标是否为 null」两态：坐标缺失同时对应两种截然不同的情形 —— 来源确实不是
     * polaris 配置文件（不可能是加密配置，安全），以及采集侧压根没做归因（未知，只能保守）。
     * 两者混为一谈会让环境变量、命令行这类外部覆盖的生效值被无谓地省略。
     */
    public enum SourceKind {

        /**
         * 采集侧未给出归因（旧版本采集侧，或归因过程本身失败）。SDK 退回保守策略。
         */
        UNKNOWN,

        /**
         * 来源是 polaris 配置文件，坐标见 {@link EffectiveValue#getSourceFile()}。
         */
        POLARIS_FILE,

        /**
         * 来源不是 polaris 配置文件（环境变量、命令行、系统属性等）。这类值不由配置中心下发，
         * 因此不可能是加密配置的明文，SDK 应照常回传。
         */
        EXTERNAL
    }

    /**
     * 兼容旧采集侧的构造器，来源视为未归因。
     *
     * @param fileValue 文件内原始值
     * @param effectiveValue 运行时生效值
     * @param propertySource 生效值来源标识
     */
    public EffectiveValue(String fileValue, String effectiveValue, String propertySource) {
        this(fileValue, effectiveValue, propertySource, null);
    }

    /**
     * 按来源坐标归因的构造器。坐标非空即 {@link SourceKind#POLARIS_FILE}，为空则视为未归因 ——
     * 想显式声明「来源不是 polaris 配置文件」请用
     * {@link #EffectiveValue(String, String, String, ConfigFileMetadata, SourceKind)}。
     *
     * @param fileValue 文件内原始值
     * @param effectiveValue 运行时生效值
     * @param propertySource 生效值来源标识
     * @param sourceFile 生效值来源的配置文件坐标，无法归因时为 null
     */
    public EffectiveValue(String fileValue, String effectiveValue, String propertySource,
            ConfigFileMetadata sourceFile) {
        this(fileValue, effectiveValue, propertySource, sourceFile,
                sourceFile == null ? SourceKind.UNKNOWN : SourceKind.POLARIS_FILE);
    }

    /**
     * 全量构造器。
     * <p>
     * 声明为 {@link SourceKind#POLARIS_FILE} 却未给坐标时降级为 {@link SourceKind#UNKNOWN}：
     * 缺了坐标 SDK 无从反查加密态，此时保守处理而非放行。
     *
     * @param fileValue 文件内原始值
     * @param effectiveValue 运行时生效值
     * @param propertySource 生效值来源标识
     * @param sourceFile 生效值来源的配置文件坐标，非 polaris 来源或无法归因时为 null
     * @param sourceKind 来源归因结论，null 视为 {@link SourceKind#UNKNOWN}
     */
    public EffectiveValue(String fileValue, String effectiveValue, String propertySource,
            ConfigFileMetadata sourceFile, SourceKind sourceKind) {
        this.fileValue = fileValue;
        this.effectiveValue = effectiveValue;
        this.propertySource = propertySource;
        this.sourceFile = sourceFile;
        if (sourceKind == null || (sourceKind == SourceKind.POLARIS_FILE && sourceFile == null)) {
            this.sourceKind = SourceKind.UNKNOWN;
        } else {
            this.sourceKind = sourceKind;
        }
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

    public SourceKind getSourceKind() {
        return sourceKind;
    }
}
