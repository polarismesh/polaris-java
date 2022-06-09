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

package com.tencent.polaris.factory.api;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.core.ProviderAPI;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.discovery.client.api.DefaultConsumerAPI;
import com.tencent.polaris.discovery.client.api.DefaultProviderAPI;
import com.tencent.polaris.factory.ConfigAPIFactory;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.factory.config.global.GlobalConfigImpl;
import com.tencent.polaris.factory.config.global.ServerConnectorConfigImpl;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * 用于创建API的工厂类型
 *
 * @author andrewshan
 * @date 2019/8/21
 */
public class DiscoveryAPIFactory {

    /**
     * 使用默认配置创建ConsumerAPI
     *
     * @return ConsumerAPI对象
     * @throws PolarisException 初始化过程的异常
     */
    public static ConsumerAPI createConsumerAPI() throws PolarisException {
        Configuration configuration = ConfigAPIFactory.defaultConfig();
        return createConsumerAPIByConfig(configuration);
    }

    /**
     * 通过SDK上下文创建ConsumerAPI
     *
     * @param context SDK上下文，包含插件列表，配置对象等信息
     * @return ConsumerAPI对象
     * @throws PolarisException 初始化过程的异常
     */
    public static ConsumerAPI createConsumerAPIByContext(SDKContext context) throws PolarisException {
        DefaultConsumerAPI defaultValue = new DefaultConsumerAPI(context);
        defaultValue.init();
        return defaultValue;
    }

    /**
     * 通过配置文件创建ConsumerAPI
     *
     * @param configStream 配置文件流
     * @return ConsumerAPI对象
     * @throws PolarisException 初始化过程的异常
     */
    public static ConsumerAPI createConsumerAPIByFile(InputStream configStream) throws PolarisException {
        Configuration configuration = ConfigAPIFactory.loadConfig(configStream);
        return DiscoveryAPIFactory.createConsumerAPIByConfig(configuration);
    }

    /**
     * 通过配置对象创建ConsumerAPI
     *
     * @param config 配置对象
     * @return ConsumerAPI对象
     * @throws PolarisException 初始化过程的异常
     */
    public static ConsumerAPI createConsumerAPIByConfig(Configuration config) throws PolarisException {
        SDKContext context = SDKContext.initContextByConfig(config);
        return DiscoveryAPIFactory.createConsumerAPIByContext(context);
    }


    /**
     * 通过注册地址创建ConsumerAPI
     *
     * @param addresses 地址
     * @return ConsumerAPI对象
     */
    public static ConsumerAPI createConsumerAPIByAddress(String... addresses) {
        return createConsumerAPIByAddress(Arrays.asList(addresses));
    }

    /**
     * 通过注册地址创建ConsumerAPI
     *
     * @param addressList 地址
     * @return ConsumerAPI对象
     */
    public static ConsumerAPI createConsumerAPIByAddress(List<String> addressList) {
        ConfigurationImpl configuration = new ConfigurationImpl("");
        GlobalConfigImpl globalConfig = configuration.getGlobal();
        ServerConnectorConfigImpl serverConnector = globalConfig.getServerConnector();
        serverConnector.setAddresses(addressList);
        return createConsumerAPIByConfig(configuration);
    }

    /**
     * 通过默认配置创建ProviderAPI
     *
     * @return ProviderAPI对象
     * @throws PolarisException 初始化过程异常
     */
    public static ProviderAPI createProviderAPI() throws PolarisException {
        Configuration configuration = ConfigAPIFactory.defaultConfig();
        return createProviderAPIByConfig(configuration);
    }

    /**
     * 通过SDK上下文创建ProviderAPI
     *
     * @param context SDK上下文，包含插件列表，配置对象等信息
     * @return ProviderAPI对象
     * @throws PolarisException 初始化过程的异常
     */
    public static ProviderAPI createProviderAPIByContext(SDKContext context) throws PolarisException {
        DefaultProviderAPI defaultValue = new DefaultProviderAPI(context);
        defaultValue.init();
        return defaultValue;
    }

    /**
     * 通过配置文件创建ProviderAPI
     *
     * @param configStream 配置文件流
     * @return ProviderAPI对象
     * @throws PolarisException 初始化过程的异常
     */
    public static ProviderAPI createProviderAPIByFile(InputStream configStream) throws PolarisException {
        Configuration configuration = ConfigAPIFactory.loadConfig(configStream);
        return DiscoveryAPIFactory.createProviderAPIByConfig(configuration);
    }

    /**
     * 通过配置对象创建ProviderAPI
     *
     * @param config 配置对象
     * @return ProviderAPI对象
     * @throws PolarisException 初始化过程的异常
     */
    public static ProviderAPI createProviderAPIByConfig(Configuration config) throws PolarisException {
        SDKContext context = SDKContext.initContextByConfig(config);
        return createProviderAPIByContext(context);
    }

    /**
     * 通过注册地址创建ProviderAPI
     *
     * @param addresses 地址
     * @return ProviderAPI对象
     */
    public static ProviderAPI createProviderAPIByAddress(String... addresses) {
        return createProviderAPIByAddress(Arrays.asList(addresses));
    }

    /**
     * 通过注册地址创建ProviderAPI
     *
     * @param addressList 地址
     * @return ProviderAPI对象
     */
    public static ProviderAPI createProviderAPIByAddress(List<String> addressList) {
        ConfigurationImpl configuration = new ConfigurationImpl("");
        GlobalConfigImpl globalConfig = configuration.getGlobal();
        ServerConnectorConfigImpl serverConnector = globalConfig.getServerConnector();
        serverConnector.setAddresses(addressList);
        return createProviderAPIByConfig(configuration);
    }

}
