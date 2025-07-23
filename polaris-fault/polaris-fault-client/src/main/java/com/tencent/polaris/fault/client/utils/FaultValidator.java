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

package com.tencent.polaris.fault.client.utils;

import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.client.util.CommonValidator;
import com.tencent.polaris.fault.api.rpc.FaultRequest;

public class FaultValidator {

    /**
     * 校验故障注入请求参数
     *
     * @param faultRequest 故障注入请求参数
     * @throws PolarisException 校验失败
     */
    public static void validateFaultRequest(FaultRequest faultRequest) throws PolarisException {
        if (null == faultRequest) {
            throw new PolarisException(ErrorCode.API_INVALID_ARGUMENT, "FaultRequest can not be null");
        }
        CommonValidator.validateService(faultRequest.getSourceService());
        CommonValidator.validateService(faultRequest.getTargetService());
    }
}
