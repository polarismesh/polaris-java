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

package com.tencent.polaris.api.exception;

/**
 * 基础异常，通过SDK API所抛出的所有异常的基类型
 *
 * @author andrewshan
 * @date 2019/8/20
 */
public class PolarisException extends RuntimeException {

    private final ErrorCode code;

    public PolarisException(ErrorCode code) {
        this.code = code;
    }

    public PolarisException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public PolarisException(ErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    @Override
    public String getMessage() {
        StringBuilder builder = new StringBuilder(String.format("ERR-%d(%s), ", code.code, code.name()));
        builder.append(super.getMessage());
        Throwable cause = getCause();
        if (null != cause) {
            builder.append(", cause: ").append(cause.getMessage());
        }
        return builder.toString();
    }

    public ErrorCode getCode() {
        return code;
    }
}
