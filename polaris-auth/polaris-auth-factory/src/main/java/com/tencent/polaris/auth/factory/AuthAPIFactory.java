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

package com.tencent.polaris.auth.factory;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.auth.api.core.AuthAPI;
import com.tencent.polaris.auth.client.api.DefaultAuthAPI;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.ConfigAPIFactory;

/**
 * Factory to create AuthAPI.
 *
 * @author Haotian Zhang
 */
public class AuthAPIFactory {

    /**
     * 通过默认配置创建 AuthAPI
     *
     * @return AuthAPI 对象
     * @throws PolarisException 初始化过程异常
     */
    public static AuthAPI createAuthAPI() throws PolarisException {
        Configuration configuration = ConfigAPIFactory.defaultConfig();
        return createAuthAPIByConfig(configuration);
    }

    /**
     * 通过 SDKContext 创建 AuthAPI
     *
     * @param sdkContext 上下文信息
     * @return AuthAPI 对象
     * @throws PolarisException 创建过程的初始化异常
     */
    public static AuthAPI createAuthAPIByContext(SDKContext sdkContext) throws PolarisException {
        DefaultAuthAPI defaultAuthAPI = new DefaultAuthAPI(sdkContext);
        defaultAuthAPI.init();
        return defaultAuthAPI;
    }

    /**
     * 通过配置对象创建 AuthAPI
     *
     * @param config 配置对象
     * @return AuthAPI 对象
     * @throws PolarisException 初始化过程的异常
     */
    public static AuthAPI createAuthAPIByConfig(Configuration config) throws PolarisException {
        SDKContext context = SDKContext.initContextByConfig(config);
        return createAuthAPIByContext(context);
    }
}
