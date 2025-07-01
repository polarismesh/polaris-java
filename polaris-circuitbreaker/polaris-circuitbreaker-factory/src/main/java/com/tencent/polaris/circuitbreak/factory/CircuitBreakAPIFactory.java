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

package com.tencent.polaris.circuitbreak.factory;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.circuitbreak.api.CircuitBreakAPI;
import com.tencent.polaris.circuitbreak.client.api.DefaultCircuitBreakAPI;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.ConfigAPIFactory;
import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;

import java.util.Arrays;

public class CircuitBreakAPIFactory {

    private static final Logger LOG = LoggerFactory.getLogger(CircuitBreakAPIFactory.class);

    public static CircuitBreakAPI createCircuitBreakAPI() throws PolarisException {
        Configuration configuration = ConfigAPIFactory.defaultConfig();
        return createCircuitBreakAPIByConfig(configuration);
    }

    /**
     * 创建服务熔断的API对象
     *
     * @param sdkContext SDK上下文信息
     * @return 熔断API
     * @throws PolarisException 校验失败
     */
    public static CircuitBreakAPI createCircuitBreakAPIByContext(SDKContext sdkContext) throws PolarisException {
        DefaultCircuitBreakAPI defaultCircuitBreakAPI = new DefaultCircuitBreakAPI(sdkContext);
        defaultCircuitBreakAPI.init();
        return defaultCircuitBreakAPI;
    }

    public static CircuitBreakAPI createCircuitBreakAPIByConfig(Configuration config) throws PolarisException {
        SDKContext context = SDKContext.initContextByConfig(config);
        return createCircuitBreakAPIByContext(context);
    }

    /**
     * 通过注册地址创建CircuitBreakAPI
     *
     * @param addresses 地址
     * @return CircuitBreakAPI对象
     */
    public static CircuitBreakAPI createCircuitBreakAPIByAddress(String... addresses) {
        return createCircuitBreakAPIByConfig(ConfigAPIFactory.createConfigurationByAddress(Arrays.asList(addresses)));
    }
}