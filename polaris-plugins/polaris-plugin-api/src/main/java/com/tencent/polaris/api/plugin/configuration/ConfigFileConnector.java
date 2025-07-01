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

package com.tencent.polaris.api.plugin.configuration;

import com.tencent.polaris.api.plugin.Plugin;

import java.util.List;

public interface ConfigFileConnector extends Plugin {

    /**
     * 获取配置文件
     *
     * @param configFile 配置文件元信息
     * @return 配置文件信息
     */
    ConfigFileResponse getConfigFile(ConfigFile configFile);

    /**
     * 监听配置文件变更
     *
     * @param configFiles 监听的配置文件列表
     * @return 变更的配置文件
     */
    ConfigFileResponse watchConfigFiles(List<ConfigFile> configFiles);

    /**
     * 创建配置文件
     *
     * @param configFile 配置文件元信息
     * @return 配置文件信息
     */
    ConfigFileResponse createConfigFile(ConfigFile configFile);

    /**
     * 更新配置文件
     *
     * @param configFile 配置文件元信息
     * @return 配置文件信息
     */
    ConfigFileResponse updateConfigFile(ConfigFile configFile);

    /**
     * 发布配置文件
     *
     * @param configFile 配置文件元信息
     * @return 配置文件信息
     */
    ConfigFileResponse releaseConfigFile(ConfigFile configFile);

    /**
     * @param request {@link ConfigPublishFile}
     * @return {@link ConfigFileResponse}
     */
    ConfigFileResponse upsertAndPublishConfigFile(ConfigPublishFile request);

    /**
     * 是否严格限制NotifiedVersion增长才更新配置。
     *
     * @return boolean
     */
    default boolean isNotifiedVersionIncreaseStrictly() {
        return true;
    }
}
