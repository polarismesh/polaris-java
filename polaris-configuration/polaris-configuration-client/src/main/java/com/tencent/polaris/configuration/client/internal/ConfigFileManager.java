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

package com.tencent.polaris.configuration.client.internal;

import com.tencent.polaris.configuration.api.core.ConfigFile;
import com.tencent.polaris.configuration.api.core.ConfigFileFormat;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.api.core.ConfigKVFile;

/**
 * @author lepdou 2022-03-02
 */
public interface ConfigFileManager {

    ConfigFile getConfigFile(ConfigFileMetadata configFileMetadata);

    ConfigKVFile getConfigKVFile(ConfigFileMetadata configFileMetadata, ConfigFileFormat fileFormat);

    void createConfigFile(ConfigFileMetadata configFileMetadata, String content);

    void updateConfigFile(ConfigFileMetadata configFileMetadata, String content);

    void releaseConfigFile(ConfigFileMetadata configFileMetadata);
}
