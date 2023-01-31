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

package com.tencent.polaris.api.pojo;

import java.util.Map;

/**
 * 实例熔断状态及数据
 *
 * @author andrewshan
 * @date 2019/8/22
 */
public class CircuitBreakerStatus {

    /**
     * 标识被哪个熔断器熔断
     */
    private final String circuitBreaker;

    /**
     * 熔断器状态
     */
    private final Status status;

    /**
     * 开始被熔断的时间
     */
    private final long startTimeMs;

    /**
     * fallback configuration
     */
    private final FallbackInfo fallbackInfo;

    public CircuitBreakerStatus(String circuitBreaker, Status status, long startTimeMs) {
        this(circuitBreaker, status, startTimeMs, null);
    }

    public CircuitBreakerStatus(String circuitBreaker, Status status, long startTimeMs, FallbackInfo fallbackInfo) {
        this.circuitBreaker = circuitBreaker;
        this.status = status;
        this.startTimeMs = startTimeMs;
        this.fallbackInfo = fallbackInfo;
    }

    public String getCircuitBreaker() {
        return circuitBreaker;
    }

    public Status getStatus() {
        return status;
    }

    public long getStartTimeMs() {
        return startTimeMs;
    }

    public FallbackInfo getFallbackInfo() {
        return fallbackInfo;
    }

    /**
     * 是否可以继续分配请求
     *
     * @return boolean
     */
    public boolean isAvailable() {
        if (status == Status.CLOSE) {
            return true;
        } else if (status == Status.OPEN) {
            return false;
        }
        //TODO:判断是否已经到达半开最大请求放量阈值
        return true;
    }

    @Override
    public String toString() {
        return "CircuitBreakerStatus{"
                +
                "circuitBreaker='" + circuitBreaker + '\''
                +
                ", status=" + status
                +
                ", startTimeMs=" + startTimeMs
                +
                '}';
    }

    /**
     * Circuit break status.
     *
     * @author andrewshan
     * @date 2019/8/21
     */
    public enum Status {
        /**
         * 熔断器关闭，实例可提供服务
         */
        CLOSE,
        /**
         * 熔断器半开，实例仅提供有限的服务
         */
        HALF_OPEN,
        /**
         * 熔断器打开状态，实例不提供服务
         */
        OPEN
    }


    public static class FallbackInfo {

        private final int code;

        private final Map<String, String> headers;

        private final String body;

        public FallbackInfo(int code, Map<String, String> headers, String body) {
            this.code = code;
            this.headers = headers;
            this.body = body;
        }

        public int getCode() {
            return code;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public String getBody() {
            return body;
        }

        @Override
        public String toString() {
            return "FallbackInfo{" +
                    "code=" + code +
                    ", headers=" + headers +
                    ", body='" + body + '\'' +
                    '}';
        }
    }
}
