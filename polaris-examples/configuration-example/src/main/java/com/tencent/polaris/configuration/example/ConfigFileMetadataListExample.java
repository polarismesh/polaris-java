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

package com.tencent.polaris.configuration.example;

import com.tencent.polaris.configuration.api.core.*;

public class ConfigFileMetadataListExample {
    public static void main(String[] args) throws Exception {
        Utils.InitResult initResult = Utils.initConfiguration(args);
        String namespace = "default";
        String fileGroup = "test";

        ConfigFileService configFileService = Utils.createConfigFileService(initResult.getConfig());
        for (int i = 0; i < 10; i++) {
            ConfigFileGroup configFileGroup = configFileService.getConfigFileGroup(namespace, fileGroup);
            Utils.print(configFileGroup == null? "loopbackup: null": "loopbackup:" + configFileGroup.toString());
        }

        ConfigFileGroup configFileGroup = configFileService.getConfigFileGroup(namespace, fileGroup);
        if (configFileGroup != null) {
            configFileGroup.addChangeListener(new ConfigFileGroupChangeListener() {
                @Override
                public void onChange(ConfigFileGroupChangedEvent event) {
                    Utils.print(event.toString());
                }
            });
        }

        ConfigFile configFile = configFileService.getConfigFile(namespace, fileGroup, "application.json");
        Utils.print(configFile.getContent());
    }
}
