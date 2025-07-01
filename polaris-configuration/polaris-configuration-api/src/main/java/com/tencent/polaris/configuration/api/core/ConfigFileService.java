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
 * The service of config file.
 * <p>
 * Property and yaml file can be convert to ConfigKVFile which provide a series of common tools and methods.
 *
 * @author lepdou 2022-03-01
 */
public interface ConfigFileService {

    /**
     * Automatically parse the config file into properties format, and provide a series of common tools and methods
     *
     * @param namespace namespace of config file
     * @param fileGroup file group of config file
     * @param fileName file name
     * @return config properties file
     */
    ConfigKVFile getConfigPropertiesFile(String namespace, String fileGroup, String fileName);

    /**
     * Automatically parse the config file into properties format, and provide a series of common tools and methods
     *
     * @param configFileMetadata config file metadata
     * @return properties file
     */
    ConfigKVFile getConfigPropertiesFile(ConfigFileMetadata configFileMetadata);

    /**
     * Automatically parse the configuration file into yaml format, and provide a series of common tools and methods
     *
     * @param namespace namespace of config file
     * @param fileGroup file group of config file
     * @param fileName file name
     * @return config yaml file
     */
    ConfigKVFile getConfigYamlFile(String namespace, String fileGroup, String fileName);

    /**
     * Automatically parse the configuration file into yaml format, and provide a series of common tools and methods
     *
     * @param configFileMetadata config file metadata
     * @return yaml file
     */
    ConfigKVFile getConfigYamlFile(ConfigFileMetadata configFileMetadata);

    /**
     * Get the original configuration file
     *
     * @param namespace namespace of config file
     * @param fileGroup file group of config file
     * @param fileName file name
     * @return config file
     */
    ConfigFile getConfigFile(String namespace, String fileGroup, String fileName);

    /**
     * Get the original configuration file
     *
     * @param configFileMetadata config file metadata
     * @return config file
     */
    ConfigFile getConfigFile(ConfigFileMetadata configFileMetadata);

    /**
     * Get the configuration file group with config file metadata list
     *
     * @param namespace namespace of config file
     * @param fileGroup file group of config file
     * @return config file group with config file metadata list
     */
    ConfigFileGroup getConfigFileGroup(String namespace, String fileGroup);

    /**
     * Get the configuration file group with config file metadata list
     *
     * @param configFileGroupMetadata config file group metadata
     * @return config file group with config file metadata list
     */
    ConfigFileGroup getConfigFileGroup(ConfigFileGroupMetadata configFileGroupMetadata);
}
