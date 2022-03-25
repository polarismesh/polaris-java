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

package com.tencent.polaris.ratelimit.factory;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.ConfigAPIFactory;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.ratelimit.api.core.LimitAPI;
import com.tencent.polaris.ratelimit.client.api.DefaultLimitAPI;
import org.slf4j.Logger;

public class LimitAPIFactory {

    private static final Logger LOG = LoggerFactory.getLogger(LimitAPIFactory.class);

    /**
     * 通过默认配置创建LimitAPI
     *
     * @return LimitAPI对象
     * @throws PolarisException 初始化过程异常
     */
    public static LimitAPI createLimitAPI() throws PolarisException {
        Configuration configuration = ConfigAPIFactory.defaultConfig();
        return createLimitAPIByConfig(configuration);
    }

    public static LimitAPI createLimitAPIByContext(SDKContext sdkContext) throws PolarisException {
        DefaultLimitAPI defaultLimitAPI = new DefaultLimitAPI(sdkContext);
        defaultLimitAPI.init();
        return defaultLimitAPI;
    }

    /**
     * 通过配置对象创建LimitAPI
     *
     * @param config 配置对象
     * @return ConsumerAPI对象
     * @throws PolarisException 初始化过程的异常
     */
    public static LimitAPI createLimitAPIByConfig(Configuration config) throws PolarisException {
        SDKContext context = SDKContext.initContextByConfig(config);
        return createLimitAPIByContext(context);
    }
}
