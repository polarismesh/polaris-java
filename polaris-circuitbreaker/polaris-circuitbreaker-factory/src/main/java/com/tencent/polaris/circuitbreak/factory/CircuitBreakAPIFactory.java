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

package com.tencent.polaris.circuitbreak.factory;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.circuitbreak.api.CircuitBreakAPI;
import com.tencent.polaris.circuitbreak.client.api.DefaultCircuitBreakAPI;
import com.tencent.polaris.client.api.SDKContext;
import org.slf4j.Logger;
import com.tencent.polaris.logging.LoggerFactory;

public class CircuitBreakAPIFactory {

    private static final Logger LOG = LoggerFactory.getLogger(CircuitBreakAPIFactory.class);

    /**
     * 创建服务熔断的API对象
     *
     * @param sdkContext SDK上下文信息
     * @return 熔断API
     * @throws PolarisException 校验失败
     */
    public static CircuitBreakAPI createLimitAPIByContext(SDKContext sdkContext) throws PolarisException {
        DefaultCircuitBreakAPI defaultCircuitBreakAPI = new DefaultCircuitBreakAPI(sdkContext);
        defaultCircuitBreakAPI.init();
        return defaultCircuitBreakAPI;
    }
}