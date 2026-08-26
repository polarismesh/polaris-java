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
 * 配置生效值提供者，由上层框架（如 Spring Cloud Tencent）注入，提供 key 在运行时的生效值与来源。
 * <p>
 * 不用 Java SPI：实现必须持有运行时的 Environment（容器刷新后才存在），SPI 的无参构造无法满足。
 * 注册走显式 API {@link ConfigFileService#registerEffectiveValueProvider(ConfigEffectiveValueProvider)}。
 *
 * @author evelynwei
 */
public interface ConfigEffectiveValueProvider {

    /**
     * 返回目标配置文件中参与生效查询的 key 列表。通常为该文件解析出的全部配置键。
     *
     * @param configFile 目标配置文件坐标
     * @return key 列表；空列表表示该文件无可解析的 key
     */
    List<String> getKeys(ConfigFileMetadata configFile);

    /**
     * 解析某个 key 在目标文件中的原始值，以及当前运行时的生效值与来源。
     *
     * @param key        配置键
     * @param configFile 目标配置文件坐标，用于定位该 key 所属文件以取 fileValue
     * @return 生效值；未接入或单个 key 解析失败时返回 null，不应抛出异常
     */
    EffectiveValue resolve(String key, ConfigFileMetadata configFile);

    /**
     * 采集其他被监听配置文件中同名 key 的冲突上下文。
     *
     * @param key          目标 key
     * @param excludeFile  需排除的自身文件坐标
     * @return 冲突项列表；无冲突返回空列表
     */
    List<ConfigKeyConflict> resolveConflicts(String key, ConfigFileMetadata excludeFile);
}
