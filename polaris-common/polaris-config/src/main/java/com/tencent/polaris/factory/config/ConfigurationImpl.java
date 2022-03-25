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

package com.tencent.polaris.factory.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.config.ConfigProvider;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.factory.config.configuration.ConfigFileConfigImpl;
import com.tencent.polaris.factory.config.consumer.ConsumerConfigImpl;
import com.tencent.polaris.factory.config.global.GlobalConfigImpl;
import com.tencent.polaris.factory.config.provider.ProviderConfigImpl;
import com.tencent.polaris.factory.util.ConfigUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * SDK全量配置对象
 *
 * @author andrewshan
 * @date 2019/8/20
 */
public class ConfigurationImpl implements Configuration {

    private static final Map<String, Configuration> configProviders = new HashMap<>();

    static {
        ServiceLoader<ConfigProvider> providers = ServiceLoader.load(ConfigProvider.class);
        for (ConfigProvider provider : providers) {
            configProviders.put(provider.getName(), provider.getDefaultConfig());
        }
    }

    @JsonIgnore
    private final String defaultConfigName;

    @JsonProperty
    private GlobalConfigImpl global;

    @JsonProperty
    private ConsumerConfigImpl consumer;

    @JsonProperty
    private ProviderConfigImpl provider;

    @JsonProperty
    private ConfigFileConfigImpl configFile;

    @Override
    public GlobalConfigImpl getGlobal() {
        return global;
    }

    public void setGlobal(GlobalConfigImpl global) {
        this.global = global;
    }

    @Override
    public ConsumerConfigImpl getConsumer() {
        return consumer;
    }

    public void setConsumer(ConsumerConfigImpl consumer) {
        this.consumer = consumer;
    }

    @Override
    public ProviderConfigImpl getProvider() {
        return provider;
    }

    public void setProvider(ProviderConfigImpl provider) {
        this.provider = provider;
    }

    @Override
    public ConfigFileConfigImpl getConfigFile() {
        return configFile;
    }

    public void setConfigFile(ConfigFileConfigImpl configFile) {
        this.configFile = configFile;
    }

    @Override
    public void verify() {
        ConfigUtils.validateNull(global, "global");
        ConfigUtils.validateNull(consumer, "consumer");
        ConfigUtils.validateNull(provider, "provider");
        global.verify();
        consumer.verify();
        provider.verify();
        configFile.verify();
    }

    public ConfigurationImpl() {
        defaultConfigName = ConfigProvider.DEFAULT_CONFIG;
    }

    public ConfigurationImpl(String defaultConfigName) {
        this.defaultConfigName = defaultConfigName;
    }

    private Configuration getDefaultConfig() {
        Configuration configuration = null;
        if (StringUtils.isNotBlank(defaultConfigName)) {
            configuration = configProviders.get(defaultConfigName);
        }
        if (null == configuration) {
            return configProviders.get(ConfigProvider.DEFAULT_CONFIG);
        }
        return configuration;
    }

    public void setDefault() {
        setDefault(getDefaultConfig());
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (null == global) {
            global = new GlobalConfigImpl();
        }
        if (null == consumer) {
            consumer = new ConsumerConfigImpl();
        }
        if (null == provider) {
            provider = new ProviderConfigImpl();
        }
        if (null == configFile) {
            configFile = new ConfigFileConfigImpl();
        }
        if (null != defaultObject) {
            Configuration configuration = (Configuration) defaultObject;
            global.setDefault(configuration.getGlobal());
            consumer.setDefault(configuration.getConsumer());
            provider.setDefault(configuration.getProvider());
            configFile.setDefault(configuration.getConfigFile());
        }
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public String toString() {
        return "ConfigurationImpl{" +
                "global=" + global +
                ", consumer=" + consumer +
                ", provider=" + provider +
                '}';
    }
}
