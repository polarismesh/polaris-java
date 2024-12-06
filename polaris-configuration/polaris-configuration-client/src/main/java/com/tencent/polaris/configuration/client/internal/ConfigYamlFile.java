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

package com.tencent.polaris.configuration.client.internal;

import com.tencent.polaris.api.config.configuration.ConfigFileConfig;
import com.tencent.polaris.configuration.client.util.YamlParser;
import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;

import java.util.Properties;

/**
 * The yaml/yml file.
 *
 * @author lepdou 2022-03-08
 */
public class ConfigYamlFile extends ConfigPropertiesFile {

    private static final YamlParser YAML_PARSER = new YamlParser();

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigYamlFile.class);


    public ConfigYamlFile(String namespace, String fileGroup, String fileName,
                          ConfigFileRepo configFileRepo,
                          ConfigFileConfig configFileConfig) {
        super(namespace, fileGroup, fileName, configFileRepo, configFileConfig);
    }

    @Override
    protected Properties convertToProperties(Properties properties, String content) {
        try {
            properties = YAML_PARSER.yamlToProperties(content);
        } catch (Throwable t) {
            String msg = String.format("[Config] failed to convert content to properties. namespace = %s, "
                            + "file group = %s, file name = %s",
                    getNamespace(), getFileGroup(), getFileName());
            LOGGER.error(msg, t);
            throw new IllegalStateException(msg);
        }
        return properties;
    }
}
