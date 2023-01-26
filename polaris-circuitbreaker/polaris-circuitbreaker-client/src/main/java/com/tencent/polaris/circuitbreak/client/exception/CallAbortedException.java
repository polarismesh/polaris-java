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

package com.tencent.polaris.circuitbreak.client.exception;

import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.pojo.CircuitBreakerStatus.FallbackInfo;

public class CallAbortedException extends PolarisException {

    private final String ruleName;

    private final FallbackInfo fallbackInfo;

    public CallAbortedException(String ruleName, FallbackInfo fallbackInfo) {
        super(ErrorCode.CLIENT_CIRCUIT_BREAKING, String.format("rule %s, fallbackInfo %s", ruleName, fallbackInfo));
        this.ruleName = ruleName;
        this.fallbackInfo = fallbackInfo;
    }

    public String getRuleName() {
        return ruleName;
    }

    public FallbackInfo getFallbackInfo() {
        return fallbackInfo;
    }

}
