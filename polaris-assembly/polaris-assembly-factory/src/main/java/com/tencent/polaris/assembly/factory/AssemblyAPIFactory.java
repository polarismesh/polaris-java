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

package com.tencent.polaris.assembly.factory;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.assembly.api.AssemblyAPI;
import com.tencent.polaris.assembly.client.DefaultAssemblyAPI;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.ConfigAPIFactory;
import java.util.Arrays;

public class AssemblyAPIFactory {

    /**
     * 创建服务集成的API对象，使用默认配置
     *
     * @return AssemblyAPI
     * @throws PolarisException 内部错误
     */
    public static AssemblyAPI createAssemblyAPI() throws PolarisException {
        Configuration configuration = ConfigAPIFactory.defaultConfig();
        return createAssemblyAPIByConfig(configuration);
    }

    /**
     * 创建服务集成的API对象，根据SDK上下文
     *
     * @param sdkContext SDK上下文信息
     * @return AssemblyAPI
     * @throws PolarisException 校验失败或者内部错误
     */
    public static AssemblyAPI createAssemblyAPIByContext(SDKContext sdkContext) throws PolarisException {
        DefaultAssemblyAPI defaultAssemblyAPI = new DefaultAssemblyAPI(sdkContext);
        defaultAssemblyAPI.init();
        return defaultAssemblyAPI;
    }

    /**
     * 根据配置对象创建服务集成的API对象
     * @param config 配置对象
     * @return AssemblyAPI
     * @throws PolarisException 校验失败或者内部错误
     */
    public static AssemblyAPI createAssemblyAPIByConfig(Configuration config) throws PolarisException {
        SDKContext context = SDKContext.initContextByConfig(config);
        return createAssemblyAPIByContext(context);
    }

    /**
     * 通过注册地址创建AssemblyAPI
     *
     * @param addresses 地址
     * @return AssemblyAPI对象
     */
    public static AssemblyAPI createAssemblyAPIByAddress(String... addresses) {
        return createAssemblyAPIByConfig(ConfigAPIFactory.createConfigurationByAddress(Arrays.asList(addresses)));
    }
}
