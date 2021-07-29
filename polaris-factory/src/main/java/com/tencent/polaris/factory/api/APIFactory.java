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
import com.tencent.polaris.factory.ConfigAPIFactory;
import com.tencent.polaris.ratelimit.api.core.LimitAPI;
import com.tencent.polaris.ratelimit.factory.LimitAPIFactory;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class APIFactory {

    private static final Logger LOG = LoggerFactory.getLogger(APIFactory.class);

    /**
     * 创建默认配置的对象，优先获取conf/polaris.yaml配置文件，假如没有，则创建默认的配置对象
     *
     * @return 配置对象
     * @throws PolarisException 初始化异常
     */
    public static Configuration defaultConfig() throws PolarisException {
        return ConfigAPIFactory.defaultConfig();
    }

    /**
     * 通过配置文件加载配置对象
     *
     * @param configStream 配置文件流
     * @return 配置对象
     * @throws PolarisException 文件加载异常
     */
    public static Configuration loadConfig(InputStream configStream) throws PolarisException {
        return ConfigAPIFactory.loadConfig(configStream);
    }

    /**
     * 通过默认配置初始化SDK上下文
     *
     * @return SDK上下文
     * @throws PolarisException 初始化过程的异常
     */
    public static SDKContext initContext() throws PolarisException {
        return SDKContext.initContext();
    }

    /**
     * 通过配置对象初始化SDK上下文
     *
     * @param config 配置对象
     * @return SDK上下文
     * @throws PolarisException 初始化过程的异常
     */
    public static SDKContext initContextByConfig(Configuration config) throws PolarisException {
        return SDKContext.initContextByConfig(config);
    }

    /**
     * 通过配置文件初始化SDK上下文
     *
     * @param inputStream 配置文件
     * @return SDK上下文
     * @throws PolarisException 初始化过程的异常
     */
    public static SDKContext initContextByFile(InputStream inputStream) throws PolarisException {
        Configuration config = ConfigAPIFactory.loadConfig(inputStream);
        return SDKContext.initContextByConfig(config);
    }

    /**
     * 通过默认配置创建ConsumerAPI
     *
     * @return ConsumerAPI对象
     * @throws PolarisException 初始化过程的异常
     */
    public static ConsumerAPI createConsumerAPI() throws PolarisException {
        return DiscoveryAPIFactory.createConsumerAPI();
    }

    /**
     * 通过SDK上下文创建ConsumerAPI
     *
     * @param context SDK上下文，包含插件列表，配置对象等信息
     * @return ConsumerAPI对象
     * @throws PolarisException 初始化过程的异常
     */
    public static ConsumerAPI createConsumerAPIByContext(SDKContext context) throws PolarisException {
        return DiscoveryAPIFactory.createConsumerAPIByContext(context);
    }

    /**
     * 通过配置文件创建ConsumerAPI
     *
     * @param inputStream 文件
     * @return ConsumerAPI对象
     * @throws PolarisException 初始化过程的异常
     */
    public static ConsumerAPI createConsumerAPIByFile(InputStream inputStream) throws PolarisException {
        SDKContext context = initContextByFile(inputStream);
        return createConsumerAPIByContext(context);
    }

    /**
     * 通过配置对象创建ConsumerAPI
     *
     * @param config 配置对象
     * @return ConsumerAPI对象
     * @throws PolarisException 初始化过程的异常
     */
    public static ConsumerAPI createConsumerAPIByConfig(Configuration config) throws PolarisException {
        return DiscoveryAPIFactory.createConsumerAPIByConfig(config);
    }

    /**
     * 通过默认配置创建ProviderAPI
     *
     * @return ProviderAPI对象
     * @throws PolarisException 初始化过程的异常
     */
    public static ProviderAPI createProviderAPI() throws PolarisException {
        return DiscoveryAPIFactory.createProviderAPI();
    }

    /**
     * 通过SDK上下文创建ProviderAPI
     *
     * @param context SDK上下文，包含插件列表，配置对象等信息
     * @return ProviderAPI对象
     * @throws PolarisException 初始化过程的异常
     */
    public static ProviderAPI createProviderAPIByContext(SDKContext context) throws PolarisException {
        return DiscoveryAPIFactory.createProviderAPIByContext(context);
    }

    /**
     * 通过配置对象创建ProviderAPI
     *
     * @param config 配置对象
     * @return ProviderAPI对象
     * @throws PolarisException 初始化过程的异常
     */
    public static ProviderAPI createProviderAPIByConfig(Configuration config) throws PolarisException {
        return DiscoveryAPIFactory.createProviderAPIByConfig(config);
    }

    /**
     * 通过配置文件创建ProviderAPI
     *
     * @param inputStream 文件
     * @return ProviderAPI对象
     * @throws PolarisException 初始化过程的异常
     */
    public static ProviderAPI createProviderAPIByFile(InputStream inputStream) throws PolarisException {
        SDKContext context = initContextByFile(inputStream);
        return createProviderAPIByContext(context);
    }

    /**
     * 通过默认配置创建LimitAPI
     *
     * @return LimitAPI对象
     * @throws PolarisException 初始化过程的异常
     */
    public static LimitAPI createLimitAPI() throws PolarisException {
        return LimitAPIFactory.createLimitAPI();
    }

    /**
     * 通过SDK上下文创建LimitAPI
     *
     * @param context SDK上下文，包含插件列表，配置对象等信息
     * @return LimitAPI对象
     * @throws PolarisException 初始化过程的异常
     */
    public static LimitAPI createLimitAPIByContext(SDKContext context) throws PolarisException {
        return LimitAPIFactory.createLimitAPIByContext(context);
    }

    /**
     * 通过配置对象创建LimitAPI
     *
     * @param config 配置对象
     * @return LimitAPI对象
     * @throws PolarisException 初始化过程的异常
     */
    public static LimitAPI createLimitAPIByConfig(Configuration config) throws PolarisException {
        return LimitAPIFactory.createLimitAPIByConfig(config);
    }
}
