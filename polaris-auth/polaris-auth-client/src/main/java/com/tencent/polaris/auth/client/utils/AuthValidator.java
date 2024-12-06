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

package com.tencent.polaris.auth.client.utils;

import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.auth.api.rpc.AuthRequest;
import com.tencent.polaris.client.util.CommonValidator;

public class AuthValidator {

    /**
     * 校验鉴权请求
     *
     * @param authRequest 鉴权请求参数
     * @throws PolarisException 校验失败
     */
    public static void validateAuthRequest(AuthRequest authRequest) throws PolarisException {
        if (null == authRequest) {
            throw new PolarisException(ErrorCode.API_INVALID_ARGUMENT, "AuthRequest can not be null");
        }
        CommonValidator.validateNamespaceService(authRequest.getNamespace(), authRequest.getService());
    }
}
