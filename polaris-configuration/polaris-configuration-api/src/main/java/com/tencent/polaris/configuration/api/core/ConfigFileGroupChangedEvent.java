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

import java.util.List;

public class ConfigFileGroupChangedEvent {
    private final ConfigFileGroupMetadata configFileGroupMetadata;
    private final List<ConfigFileMetadata> oldConfigFileMetadataList;
    private final List<ConfigFileMetadata> newConfigFileMetadataList;

    public ConfigFileGroupChangedEvent(ConfigFileGroupMetadata configFileGroupMetadata,
                                       List<ConfigFileMetadata> oldConfigFileMetadataList,
                                       List<ConfigFileMetadata> newConfigFileMetadataList) {
        this.configFileGroupMetadata = configFileGroupMetadata;
        this.oldConfigFileMetadataList = oldConfigFileMetadataList;
        this.newConfigFileMetadataList = newConfigFileMetadataList;
    }

    public ConfigFileGroupMetadata getConfigFileGroupMetadata() {
        return configFileGroupMetadata;
    }

    public List<ConfigFileMetadata> getOldConfigFileMetadataList() {
        return oldConfigFileMetadataList;
    }

    public List<ConfigFileMetadata> getNewConfigFileMetadataList() {
        return newConfigFileMetadataList;
    }

    @Override
    public String toString() {
        return "ConfigFileGroupChangedEvent{" +
                "configFileGroupMetadata=" + configFileGroupMetadata +
                ", oldConfigFileMetadataList=" + oldConfigFileMetadataList +
                ", newConfigFileMetadataList=" + newConfigFileMetadataList +
                '}';
    }
}
