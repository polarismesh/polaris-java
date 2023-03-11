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

package com.tencent.polaris.configuration.api.core;

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
    void createConfigFile(String namespace, String fileGroup, String fileName, String content);

    /**
     * Create the configuration file
     *
     * @param configFileMetadata config file metadata
     */
    void createConfigFile(ConfigFileMetadata configFileMetadata, String content);

    /**
     * Update the configuration file
     *
     * @param namespace namespace of config file
     * @param fileGroup file group of config file
     * @param fileName file name
     */
    void updateConfigFile(String namespace, String fileGroup, String fileName, String content);

    /**
     * Update the configuration file
     *
     * @param configFileMetadata config file metadata
     */
    void updateConfigFile(ConfigFileMetadata configFileMetadata, String content);

    /**
     * Release the configuration file
     *
     * @param namespace namespace of config file
     * @param fileGroup file group of config file
     * @param fileName file name
     */
    void releaseConfigFile(String namespace, String fileGroup, String fileName);

    /**
     * Release the configuration file
     *
     * @param configFileMetadata config file metadata
     */
    void releaseConfigFile(ConfigFileMetadata configFileMetadata);
}
