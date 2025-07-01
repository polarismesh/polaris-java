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

package com.tencent.polaris.plugins.connector.consul.service.circuitbreaker.entity;

public class TsfCircuitBreakerConstant {

    //统计滚动的时间窗口
    public static final Integer MAX_SLIDING_WINDOW_SIZE = 9999;
    public static final Integer MIN_SLIDING_WINDOW_SIZE = 1;

    // 失败请求比例
    public static final Integer MAX_FAILURE_RATE_THRESHOLD = 100;
    public static final Integer MIN_FAILURE_RATE_THRESHOLD = 1;

    // 失败请求比例
    public static final Integer MAX_EJECTION_RATE_THRESHOLD = 100;
    public static final Integer MIN_EJECTION_RATE_THRESHOLD = 0;

    // 熔断开启到半开间隔
    public static final Integer MAX_WAIT_DURATION_IN_OPEN_STATE = 9999;
    public static final Integer MIN_WAIT_DURATION_IN_OPEN_STATE = 1;

    // 最小失败请求数
    public static final Integer MINIMUN_NUMBER_OF_CALLS = 1;

    // 路由前时间
    public static final String PRE_ROUTE_TIME = "preRouteTime";
    // 路由事件标识
    public static final String IS_IN_ROUTING_STATE = "isInRoutingState";

    public static final RuntimeException MOCK_EXCEPTION = new RuntimeException();

    /**
     * 网关上下文变量
     */
    public static class GatewayContext {

        public final static String API_NAMESPACE_ID = "TsfContextApiNamespaceId";
        public final static String API_SERVICE_NAME = "TsfContextApiServiceName";
        public final static String TSF_GATEWAY_REQUEST_API = "tsf_gateway_request_api";

        /**
         * 用于记录熔断的命名空间信息
         */
        public final static String TSF_GATEWAY_TARGET_NAMESPACE_ID = "tsfGatewayTargetNamespaceId";

        /**
         * 用于传递服务实例
         */
        public final static String TSF_GATEWAY_SERVICE_INSTANCE = "tsfGatewayServiceInstance";

        /**
         * 是否是外部API代理
         */
        public static final String TSF_GATEWAY_EXTERNAL_API_PROXY = "tsf_gateway_external_api_proxy";

        /**
         * 是否是tsf网关模式
         */
        public static final String TSF_GATEWAY_ROUTE_MODE = "tsf_gateway_route_mode";

    }
}
