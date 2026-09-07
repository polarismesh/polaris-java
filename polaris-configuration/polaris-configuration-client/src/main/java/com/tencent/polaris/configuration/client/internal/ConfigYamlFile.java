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

package com.tencent.polaris.configuration.client.internal;

import com.tencent.polaris.api.config.configuration.ConfigFileConfig;
import com.tencent.polaris.configuration.client.util.YamlParser;
import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.MarkedYAMLException;

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
            // 不把异常对象交给 logger：SnakeYAML 的 MarkedYAMLException 会把出错位置附近的原始
            // YAML 行打进日志，加密配置在此已是解密后的明文。只输出异常类型与行列号用于定位
            LOGGER.error("{}, error = {}", msg, describeParseFailure(t));
            throw new IllegalStateException(msg);
        }
        return properties;
    }

    /**
     * 描述解析失败，只含异常类型与出错行列号，绝不含正文片段。
     *
     * @param t 解析异常
     * @return 可安全写入日志的描述
     */
    private String describeParseFailure(Throwable t) {
        String description = t.getClass().getName();
        if (t instanceof MarkedYAMLException) {
            Mark mark = ((MarkedYAMLException) t).getProblemMark();
            if (mark != null) {
                description = description + " at line " + (mark.getLine() + 1)
                        + ", column " + (mark.getColumn() + 1);
            }
        }
        return description;
    }
}
