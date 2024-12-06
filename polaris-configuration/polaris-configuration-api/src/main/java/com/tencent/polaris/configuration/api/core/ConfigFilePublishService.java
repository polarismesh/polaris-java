/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

import com.tencent.polaris.api.plugin.configuration.ConfigFileResponse;
import com.tencent.polaris.configuration.api.rpc.ConfigPublishRequest;
import com.tencent.polaris.configuration.api.rpc.CreateConfigFileRequest;
import com.tencent.polaris.configuration.api.rpc.ReleaseConfigFileRequest;
import com.tencent.polaris.configuration.api.rpc.UpdateConfigFileRequest;

/**
 * @author fabian4 2023-03-08
 */
public interface ConfigFilePublishService {

    /**
     * Create the configuration file
     *
     * @param namespace namespace of config file
     * @param fileGroup file group of config file
     * @param fileName file name
     */
    @Deprecated
    ConfigFileResponse createConfigFile(String namespace, String fileGroup, String fileName, String content);

    /**
     * Create the configuration file
     *
     * @param configFileMetadata config file metadata
     */
    @Deprecated
    ConfigFileResponse createConfigFile(ConfigFileMetadata configFileMetadata, String content);

    /**
     * Update the configuration file
     *
     * @param namespace namespace of config file
     * @param fileGroup file group of config file
     * @param fileName file name
     */
    @Deprecated
    ConfigFileResponse updateConfigFile(String namespace, String fileGroup, String fileName, String content);

    /**
     * Update the configuration file
     *
     * @param configFileMetadata config file metadata
     */
    @Deprecated
    ConfigFileResponse updateConfigFile(ConfigFileMetadata configFileMetadata, String content);

    /**
     * Release the configuration file
     *
     * @param namespace namespace of config file
     * @param fileGroup file group of config file
     * @param fileName file name
     */
    @Deprecated
    ConfigFileResponse releaseConfigFile(String namespace, String fileGroup, String fileName);

    /**
     * Release the configuration file
     *
     * @param configFileMetadata config file metadata
     */
    @Deprecated
    ConfigFileResponse releaseConfigFile(ConfigFileMetadata configFileMetadata);

    /**
     * 创建一个配置文件
     *
     * @param request {@link CreateConfigFileRequest}
     * @return {@link ConfigFileResponse}
     */
    ConfigFileResponse create(CreateConfigFileRequest request);

    /**
     * 更新配置文件
     *
     * @param request {@link UpdateConfigFileRequest}
     * @return {@link ConfigFileResponse}
     */
    ConfigFileResponse update(UpdateConfigFileRequest request);

    /**
     * 发布对应的配置文件，发布后客户端可见
     *
     * @param request {@link ReleaseConfigFileRequest}
     * @return {@link ConfigFileResponse}
     */
    ConfigFileResponse release(ReleaseConfigFileRequest request);

    /**
     * 创建/更新并发布配置文件，如果携带了 casMd5 参数，表示会根据 case 能力进行选自行动哦配置发布
     *
     * @param request {@link ConfigPublishRequest}
     * @return {@link ConfigFileResponse}
     */
    ConfigFileResponse upsertAndPublish(ConfigPublishRequest request);
}
