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

package com.tencent.polaris.api.exception;

/**
 * 错误码
 *
 * @author andrewshan
 * @date 2019/8/20
 */
public enum ErrorCode {

    /**
     * 操作成功
     */
    Success(0),

    /**
     * API参数非法的错误码
     */
    API_INVALID_ARGUMENT(1001),

    /**
     * 配置非法的错误码
     */
    INVALID_CONFIG(1002),

    /**
     * 获取插件失败
     */
    PLUGIN_ERROR(1003),

    /**
     * API超时错误的错误码
     */
    API_TIMEOUT(1004),

    /**
     * SDK已经destroy后，继续调API会出现的错误码
     */
    INVALID_STATE(1005),

    /**
     * 连接server时，server返回已知错误信息
     */
    SERVER_USER_ERROR(1006),

    /**
     * 连接server时所出现的未知网络异常
     */
    NETWORK_ERROR(1007),

    /**
     * 服务熔断错误
     */
    CIRCUIT_BREAK_ERROR(1008),

    /**
     * 实例信息有误，如服务权重信息为空
     */
    INSTANCE_INFO_ERROR(1009),

    /**
     * 负载均衡时发现服务实例为空
     */
    INSTANCE_NOT_FOUND(1010),

    /**
     * 规则非法
     */
    INVALID_RULE(1011),

    /**
     * 服务规则路由出错
     */
    ROUTE_RULE_NOT_MATCH(1012),

    /**
     * 服务规则路由出错
     */
    INVALID_RESPONSE(1013),

    /**
     * 内部算法及系统错误
     */
    INTERNAL_ERROR(1014),

    /**
     * 服务不存在
     */
    SERVICE_NOT_FOUND(1015),

    /**
     * server返回500错误
     */
    SERVER_EXCEPTION(1016),

    /**
     * 获取地域信息失败
     */
    LOCATION_NOT_FOUND(1017),

    /**
     * 就近路由匹配出错
     */
    LOCATION_MISMATCH(1018),

    /**
     * metadata路由匹配出错
     */
    METADATA_MISMATCH(1019),

    /**
     * client resource has been circuitbreakered
     */
    CLIENT_CIRCUIT_BREAKING(1020),

    /**
     * client resource has been fault injected
     */
    CLIENT_FAULT_INJECTED(1021),

    /**
     * 内部错误：连续错误
     */
    CONNECT_ERROR(2001),

    /**
     * 内部错误：服务端500错误
     */
    SERVER_ERROR(2002),

    RPC_ERROR(2003),

    RPC_TIMEOUT(2004),

    /**
     * 内部错误：服务端返回应答非法
     */
    INVALID_SERVER_RESPONSE(2005),

    /**
     * 内部错误：服务端返回非法请求
     */
    INVALID_REQUEST(2006),

    /**
     * 内部错误：未鉴权操作
     */
    UNAUTHORIZED(2007),

    /**
     * 内部错误：被限流
     */
    REQUEST_LIMIT(2008),

    /**
     * 内部错误：CMDB信息找不到
     */
    CMDB_NOT_FOUND(2009),

    /**
     * 内部错误：未知服务端异常
     */
    UNKNOWN_SERVER_ERROR(2100),

    /**
     * 内部错误：暂不支持
     */
    NOT_SUPPORT(20010),

    /**
     * 内部错误：RSA key 生成失败
     */
    RSA_KEY_GENERATE_ERROR(30001),

    /**
     * 内部错误：RSA 加密失败
     */
    RSA_ENCRYPT_ERROR(30002),

    /**
     * 内部错误：RSA 解密失败
     */
    RSA_DECRYPT_ERROR(30003),

    /**
     * 内部错误：AES key 生成失败
     */
    AES_KEY_GENERATE_ERROR(30004),

    /**
     * 内部错误：AES 加密失败
     */
    AES_ENCRYPT_ERROR(30005),

    /**
     * 内部错误：AES 解密失败
     */
    AES_DECRYPT_ERROR(30006),

    /**
     * 内部错误：参数错误
     */
    PARAMETER_ERROR(40000);

    private final int code;

    ErrorCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
