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

package com.tencent.polaris.router.client.util;

import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.router.api.rpc.ProcessLoadBalanceRequest;
import com.tencent.polaris.router.api.rpc.ProcessRoutersRequest;

public class RouterValidator {

    /**
     * 校验路由链处理请求
     *
     * @param request 路由处理请求
     * @throws PolarisException 参数校验异常
     */
    public static void validateProcessRouterRequest(ProcessRoutersRequest request) throws PolarisException {
        if (null == request.getDstInstances()) {
            throw new PolarisException(ErrorCode.API_INVALID_ARGUMENT, "dstInstances is null");
        }
    }

    /**
     * 校验负载均处理请求
     *
     * @param request 负载均衡请求
     * @throws PolarisException 参数校验异常
     */
    public static void validateProcessLoadBalanceRequest(ProcessLoadBalanceRequest request) throws PolarisException {
        if (null == request.getDstInstances()) {
            throw new PolarisException(ErrorCode.API_INVALID_ARGUMENT, "dstInstances is null");
        }
    }
}
