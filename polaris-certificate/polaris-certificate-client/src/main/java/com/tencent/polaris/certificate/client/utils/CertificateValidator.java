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

package com.tencent.polaris.certificate.client.utils;

import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.certificate.api.pojo.CsrRequest;
import com.tencent.polaris.client.util.CommonValidator;

public class CertificateValidator {

    /**
     * 验证CSR请求
     *
     * @param csrRequest CSR请求
     * @throws PolarisException 异常
     */
    public static void validateCsrRequest(CsrRequest csrRequest) throws PolarisException {
        if (null == csrRequest) {
            throw new PolarisException(ErrorCode.API_INVALID_ARGUMENT, "CsrRequest can not be null");
        }
        CommonValidator.validateText(csrRequest.getCommonName(), "commonName");
        if (csrRequest.getKeyPair() == null) {
            throw new PolarisException(ErrorCode.API_INVALID_ARGUMENT,
                    String.format("field %s can not be null", "keyPair"));
        }
    }
}
