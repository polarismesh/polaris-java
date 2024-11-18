/*
 * Tencent is pleased to support the open source community by making Polaris available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
 *
 *  Licensed under the BSD 3-Clause License (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.tencent.polaris.api.plugin.auth;

/**
 * 鉴权结果
 *
 * @author Haotian Zhang
 */
public class AuthResult {

    /**
     * 限流返回码
     */
    public enum Code {
        /**
         * OK，代表请求可以通过
         */
        AuthResultOk,
        /**
         * FORBIDDEN，代表本次请求被禁止
         */
        AuthResultForbidden,
    }

    private final Code code;

    public AuthResult(Code code) {
        this.code = code;
    }

    public Code getCode() {
        return code;
    }

    @Override
    public String toString() {
        return "AuthResult{" +
                "code=" + code +
                '}';
    }
}
