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

package com.tencent.polaris.fault.factory;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.ConfigAPIFactory;
import com.tencent.polaris.fault.api.core.FaultAPI;
import com.tencent.polaris.fault.client.api.DefaultFaultAPI;

/**
 * Factory to create FaultAPI.
 *
 * @author Haotian Zhang
 */
public class FaultAPIFactory {

    /**
     * 通过默认配置创建 FaultAPI
     *
     * @return FaultAPI 对象
     * @throws PolarisException 初始化过程异常
     */
    public static FaultAPI createFaultAPI() throws PolarisException {
        Configuration configuration = ConfigAPIFactory.defaultConfig();
        return createFaultAPIByConfig(configuration);
    }

    /**
     * 通过 SDKContext 创建 FaultAPI
     *
     * @param sdkContext 上下文信息
     * @return FaultAPI 对象
     * @throws PolarisException 创建过程的初始化异常
     */
    public static FaultAPI createFaultAPIByContext(SDKContext sdkContext) throws PolarisException {
        DefaultFaultAPI defaultFaultAPI = new DefaultFaultAPI(sdkContext);
        defaultFaultAPI.init();
        return defaultFaultAPI;
    }

    /**
     * 通过配置对象创建 FaultAPI
     *
     * @param config 配置对象
     * @return FaultAPI 对象
     * @throws PolarisException 初始化过程的异常
     */
    public static FaultAPI createFaultAPIByConfig(Configuration config) throws PolarisException {
        SDKContext context = SDKContext.initContextByConfig(config);
        return createFaultAPIByContext(context);
    }
}
