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

package com.tencent.polaris.factory.api;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.ConfigAPIFactory;
import com.tencent.polaris.router.api.core.RouterAPI;
import com.tencent.polaris.router.client.api.DefaultRouterAPI;
import java.io.InputStream;

public class RouterAPIFactory {


    /**
     * 使用默认配置创建EngineAPI
     *
     * @return RouterAPI对象
     * @throws PolarisException 创建过程的初始化异常
     */
    public static RouterAPI createRouterAPI() throws PolarisException {
        Configuration configuration = ConfigAPIFactory.defaultConfig();
        return createRouterAPIByConfig(configuration);
    }

    /**
     * 创建引擎的API对象
     *
     * @param sdkContext 上下文信息
     * @return RouterAPI对象
     * @throws PolarisException 创建过程的初始化异常
     */
    public static RouterAPI createRouterAPIByContext(SDKContext sdkContext) throws PolarisException {
        DefaultRouterAPI defaultEngineAPI = new DefaultRouterAPI(sdkContext);
        defaultEngineAPI.init();
        return defaultEngineAPI;
    }

    /**
     * 通过配置对象创建EngineAPI
     *
     * @param config 配置对象
     * @return API对象
     * @throws PolarisException 初始化异常
     */
    public static RouterAPI createRouterAPIByConfig(Configuration config) throws PolarisException {
        SDKContext context = SDKContext.initContextByConfig(config);
        return createRouterAPIByContext(context);
    }

    /**
     * 通过配置文件创建EngineAPI对象
     *
     * @param stream 文件流
     * @return RouterAPI对象
     * @throws PolarisException 初始化异常
     */
    public static RouterAPI createRouterAPIByFile(InputStream stream) throws PolarisException {
        Configuration configuration = ConfigAPIFactory.loadConfig(stream);
        return createRouterAPIByConfig(configuration);
    }
}
