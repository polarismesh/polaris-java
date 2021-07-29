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
 * Server端返回的错误码信息
 *
 * @author andrewshan
 * @date 2019/8/28
 */
public interface ServerCodes {

    /**
     * 代表请求执行成功
     */
    int EXECUTE_SUCCESS = 200000;

    /**
     * 代表缓存数据未变更
     */
    int DATA_NO_CHANGE = 200001;

    /**
     * 资源已经存在，无需重复注册
     */
    int EXISTED_RESOURCE = 400201;

    /**
     * 资源不存在
     */
    int NOT_FOUND_RESOURCE = 400202;

    /**
     * 服务不存在
     */
    int NOT_FOUND_SERVICE = 400301;

    /**
     * 转换为http格式的返回码
     *
     * @param code http code
     * @return 返回码
     */
    static int toHttpCode(int code) {
        return (code / 100000) * 100;
    }

    /**
     * 将server错误码转换为内部服务错误
     *
     * @param code server错误码
     * @return 服务错误信息
     */
    static ErrorCode convertServerErrorToRpcError(int code) {
        int typCode = code / 1000;
        switch (typCode) {
            case 200:
                return ErrorCode.Success;
            case 400:
                return ErrorCode.INVALID_REQUEST;
            case 401:
                return ErrorCode.UNAUTHORIZED;
            case 403:
                return ErrorCode.REQUEST_LIMIT;
            case 404:
                return ErrorCode.CMDB_NOT_FOUND;
            case 500:
                return ErrorCode.SERVER_ERROR;
            default:
                return ErrorCode.UNKNOWN_SERVER_ERROR;
        }
    }

}
