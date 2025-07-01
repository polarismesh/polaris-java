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

package com.tencent.polaris.factory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.factory.config.global.GlobalConfigImpl;
import com.tencent.polaris.factory.config.global.ServerConnectorConfigImpl;
import com.tencent.polaris.factory.replace.SystemPropertyPlaceholderResolver;
import com.tencent.polaris.factory.util.PropertyPlaceholderHelper;
import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigAPIFactory {


    private static final Logger LOG = LoggerFactory.getLogger(ConfigAPIFactory.class);

    /**
     * Default placeholder prefix: {@value}.
     */
    public static final String DEFAULT_PLACEHOLDER_PREFIX = "${";

    /**
     * Default placeholder suffix: {@value}.
     */
    public static final String DEFAULT_PLACEHOLDER_SUFFIX = "}";

    /**
     * Default value separator: {@value}.
     */
    public static final String DEFAULT_VALUE_SEPARATOR = ":";

    /**
     * 通过配置文件加载配置对象
     *
     * @param configStream 配置文件流
     * @return 配置对象
     * @throws PolarisException 文件加载异常
     */
    public static Configuration loadConfig(InputStream configStream) throws PolarisException {
        String configText;
        try {
            configText = replaceConfigText(configStream);
        } catch (Throwable e) {
            throw new PolarisException(ErrorCode.INVALID_CONFIG, "fail to preprocess config", e);
        }
        YAMLFactory yamlFactory = new YAMLFactory();
        ObjectMapper mapper = new ObjectMapper();

        try {
            YAMLParser yamlParser = yamlFactory.createParser(configText);
            final JsonNode node = mapper.readTree(yamlParser);
            TreeTraversingParser treeTraversingParser = new TreeTraversingParser(node);
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return mapper.readValue(treeTraversingParser, ConfigurationImpl.class);
        } catch (Exception e) {
            throw new PolarisException(
                    ErrorCode.INVALID_CONFIG, "fail to load config from stream", e);
        }
    }

    private static String replaceConfigText(InputStream configStream) {
        String result = new BufferedReader(new InputStreamReader(configStream))
                .lines().collect(Collectors.joining("\n"));
        PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper(DEFAULT_PLACEHOLDER_PREFIX,
                DEFAULT_PLACEHOLDER_SUFFIX, DEFAULT_VALUE_SEPARATOR, false);
        return propertyPlaceholderHelper.replacePlaceholders(result, new SystemPropertyPlaceholderResolver());
    }

    public static final String DEFAULT_CONFIG_PATH = "polaris.yml";

    /**
     * 默认配置对象，优先获取当前目录下polaris.yml配置文件，假如没有，则创建默认的配置对象
     *
     * @return 默认配置对象
     * @throws PolarisException 加载异常
     */
    public static Configuration defaultConfig() throws PolarisException {
        return defaultConfig("");
    }

    /**
     * 默认配置对象，优先获取当前目录下polaris.yml配置文件，假如没有，则创建默认的配置对象
     *
     * @param defaultConfigName 默认配置名
     * @return 默认配置对象
     * @throws PolarisException 加载异常
     */
    public static Configuration defaultConfig(String defaultConfigName) throws PolarisException {
        InputStream inputStream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(DEFAULT_CONFIG_PATH);
        ConfigurationImpl configuration;
        if (null != inputStream) {
            LOG.info("[Configuration]success to load config stream from {}", DEFAULT_CONFIG_PATH);
            configuration = (ConfigurationImpl) loadConfig(inputStream);
        } else {
            configuration = new ConfigurationImpl(defaultConfigName);
        }
        configuration.setDefault();
        return configuration;
    }

    /**
     * 通过设置地址创建配置
     *
     * @param addressList 地址
     * @return 配置
     */
    public static Configuration createConfigurationByAddress(List<String> addressList) {
        ConfigurationImpl configuration = (ConfigurationImpl) defaultConfig();
        GlobalConfigImpl globalConfig = configuration.getGlobal();
        ServerConnectorConfigImpl serverConnector = globalConfig.getServerConnector();
        serverConnector.setAddresses(addressList);
        return configuration;
    }

    /**
     * 通过设置地址创建配置
     *
     * @param addresses 地址
     * @return 配置
     */
    public static Configuration createConfigurationByAddress(String... addresses) {
        return createConfigurationByAddress(Arrays.asList(addresses));
    }

}
