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

package com.tencent.polaris.api.plugin.configuration;

import java.util.Comparator;
import java.util.List;

public class ConfigFileGroup extends ConfigFileGroupMetadata {
    private List<ConfigFile> configFileList;

    public List<ConfigFile> getConfigFileList() {
        return this.getConfigFileList(defaultComparator);
    }

    public List<ConfigFile> getConfigFileList(Comparator<ConfigFile> comparator) {
        if (configFileList != null && comparator != null) {
            configFileList.sort(comparator);
        }
        return configFileList;
    }

    public void setConfigFileList(List<ConfigFile> configFileList) {
        this.configFileList = configFileList;
    }

    public static final Comparator<ConfigFile> defaultComparator = Comparator.comparing(ConfigFile::getReleaseTime).reversed();
}
