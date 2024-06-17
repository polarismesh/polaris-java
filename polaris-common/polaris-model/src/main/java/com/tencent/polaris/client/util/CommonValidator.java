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

package com.tencent.polaris.client.util;

import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.utils.StringUtils;

public class CommonValidator {

    public static final int MAX_PORT = 65536;

    /**
     * 校验命名空间和服务名
     *
     * @param namespace 命名空间
     * @param service 服务名
     * @throws PolarisException 异常
     */
    public static void validateNamespaceService(String namespace, String service) throws PolarisException {
        if (StringUtils.isBlank(namespace)) {
            throw new PolarisException(ErrorCode.API_INVALID_ARGUMENT, "namespace can not be blank");
        }
        if (StringUtils.isBlank(service)) {
            throw new PolarisException(ErrorCode.API_INVALID_ARGUMENT, "service can not be blank");
        }
    }

    public static void validateService(ServiceKey service) {
        if (null == service) {
            throw new PolarisException(ErrorCode.API_INVALID_ARGUMENT, "service can not be empty");
        }
    }

    public static void validateText(String text, String fieldName) {
        if (StringUtils.isBlank(text)) {
            throw new PolarisException(ErrorCode.API_INVALID_ARGUMENT,
                    String.format("field %s can not be empty", fieldName));
        }
    }
}
