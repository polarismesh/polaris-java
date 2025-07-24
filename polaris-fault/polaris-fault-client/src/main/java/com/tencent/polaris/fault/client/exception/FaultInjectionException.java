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

package com.tencent.polaris.fault.client.exception;

import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.pojo.CircuitBreakerStatus.FallbackInfo;

public class FaultInjectionException extends PolarisException {

    private final FallbackInfo fallbackInfo;

    public FaultInjectionException(FallbackInfo fallbackInfo) {
        super(ErrorCode.CLIENT_FAULT_INJECTED, String.format("fallbackInfo %s", fallbackInfo));
        this.fallbackInfo = fallbackInfo;
    }

    public FallbackInfo getFallbackInfo() {
        return fallbackInfo;
    }
}
