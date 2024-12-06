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

package com.tencent.polaris.configuration.client.util;

import com.google.common.collect.Maps;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * @author lepdou 2022-03-04
 */
public class ConfigFileUtils {

    public static void checkConfigFileMetadata(ConfigFileMetadata configFileMetadata) {
        if (StringUtils.isBlank(configFileMetadata.getNamespace())) {
            throw new IllegalArgumentException("namespace cannot be empty.");
        }
        if (StringUtils.isBlank(configFileMetadata.getFileGroup())) {
            throw new IllegalArgumentException("file group cannot be empty.");
        }
        if (StringUtils.isBlank(configFileMetadata.getFileName())) {
            throw new IllegalArgumentException("file name cannot be empty.");
        }
    }

    public static Set<String> stringPropertyNames(Properties properties) {
        if (properties == null) {
            return Collections.emptySet();
        }
        Map<String, Object> map = Maps.newLinkedHashMapWithExpectedSize(properties.size());
        for (Map.Entry<Object, Object> e : properties.entrySet()) {
            Object k = e.getKey();
            Object v = e.getValue();
            if (k instanceof String) {
                map.put((String) k, v);
            }
        }
        return map.keySet();
    }
}
