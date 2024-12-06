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

package com.tencent.polaris.plugins.configuration.connector.consul.utils;

import com.tencent.polaris.api.plugin.configuration.ConfigFile;
import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;

/**
 * @author Haotian Zhang
 */
public class ConsulConfigFileUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsulConfigFileUtils.class);

    public static String toConsulKVKeyPrefix(ConfigFile configFile) {
        String key = "/" + configFile.getNamespace() +
                "/" + configFile.getFileGroup() +
                "/" + configFile.getFileName();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Consul config file key: {}", key);
        }
        return key;
    }
}
