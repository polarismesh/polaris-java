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

package com.tencent.polaris.plugins.stat.common.model;

import com.tencent.polaris.api.plugin.stat.CircuitBreakGauge;
import com.tencent.polaris.api.plugin.stat.RateLimitGauge;
import com.tencent.polaris.api.pojo.InstanceGauge;
import com.tencent.polaris.api.pojo.RetStatus;

import static com.tencent.polaris.api.pojo.CircuitBreakerStatus.Status.OPEN;
import static com.tencent.polaris.api.pojo.CircuitBreakerStatus.Status.HALF_OPEN;

public class MetricValueAggregationStrategyCollections {
    public static MetricValueAggregationStrategy<InstanceGauge>[] SERVICE_CALL_STRATEGY;
    public static MetricValueAggregationStrategy<RateLimitGauge>[] RATE_LIMIT_STRATEGY;
    public static MetricValueAggregationStrategy<CircuitBreakGauge>[] CIRCUIT_BREAK_STRATEGY;

    static {
        SERVICE_CALL_STRATEGY = new MetricValueAggregationStrategy[]{
                new UpstreamRequestTotalStrategy(),
                new UpstreamRequestSuccessStrategy(),
                new UpstreamRequestTimeoutStrategy(),
                new UpstreamRequestMaxTimeoutStrategy(),
        };

        RATE_LIMIT_STRATEGY = new MetricValueAggregationStrategy[]{
                new RateLimitRequestTotalStrategy(),
                new RateLimitRequestPassStrategy(),
                new RateLimitRequestLimitStrategy(),
        };

        CIRCUIT_BREAK_STRATEGY = new MetricValueAggregationStrategy[]{
                new CircuitBreakerOpenStrategy(),
                new CircuitBreakerHalfOpenStrategy(),
        };
    }

    /**
     * 服务调用总请求数
     */
    public static class UpstreamRequestTotalStrategy implements MetricValueAggregationStrategy<InstanceGauge> {

        @Override
        public String getStrategyDescription() {
            return "total of request per period";
        }

        @Override
        public String getStrategyName() {
            return "upstream_rq_total";
        }

        @Override
        public void updateMetricValue(StatMetric targetValue, InstanceGauge dataSource) {
            targetValue.incValue();
        }

        @Override
        public double initMetricValue(InstanceGauge dataSource) {
            return 1.0;
        }
    }

    /**
     * 服务调用总成功数
     */
    public static class UpstreamRequestSuccessStrategy implements MetricValueAggregationStrategy<InstanceGauge> {
        @Override
        public String getStrategyDescription() {
            return "total of success request per period";
        }

        public String getStrategyName() {
            return "upstream_rq_success";
        }

        @Override
        public void updateMetricValue(StatMetric targetValue, InstanceGauge dataSource) {
            if (RetStatus.RetSuccess == dataSource.getRetStatus()) {
                targetValue.incValue();
            }
        }

        @Override
        public double initMetricValue(InstanceGauge dataSource) {
            return RetStatus.RetSuccess == dataSource.getRetStatus() ? 1 : 0;
        }
    }

    /**
     * 服务调用总时延
     */
    public static class UpstreamRequestTimeoutStrategy implements MetricValueAggregationStrategy<InstanceGauge> {

        @Override
        public String getStrategyDescription() {
            return "total of request delay per period";
        }

        @Override
        public String getStrategyName() {
            return "upstream_rq_timeout";
        }

        @Override
        public void updateMetricValue(StatMetric targetValue, InstanceGauge dataSource) {
            if (null == dataSource.getDelay()) {
                return;
            }

            targetValue.addValue(dataSource.getDelay());
        }

        @Override
        public double initMetricValue(InstanceGauge dataSource) {
            if (null == dataSource.getDelay()) {
                return 0.0;
            }

            return dataSource.getDelay();
        }
    }

    /**
     * 服务调用最大时延
     */
    public static class UpstreamRequestMaxTimeoutStrategy implements MetricValueAggregationStrategy<InstanceGauge> {
        @Override
        public String getStrategyDescription() {
            return "maximum request delay per period";
        }

        @Override
        public String getStrategyName() {
            return "upstream_rq_max_timeout";
        }

        @Override
        public void updateMetricValue(StatMetric targetValue, InstanceGauge dataSource) {
            if (null == dataSource.getDelay()) {
                return;
            }

            while (true) {
                if (dataSource.getDelay() > targetValue.getValue()) {
                    if (targetValue.compareAndSet(targetValue.getValue(), dataSource.getDelay())) {
                        return;
                    }
                } else {
                    return;
                }
            }
        }

        @Override
        public double initMetricValue(InstanceGauge dataSource) {
            if (null == dataSource.getDelay()) {
                return 0.0;
            }

            return dataSource.getDelay();
        }
    }

    /**
     * 限流调用总请求数
     */
    public static class RateLimitRequestTotalStrategy implements MetricValueAggregationStrategy<RateLimitGauge> {
        @Override
        public String getStrategyDescription() {
            return "total of rate limit per period";
        }

        @Override
        public String getStrategyName() {
            return "ratelimit_rq_total";
        }

        @Override
        public void updateMetricValue(StatMetric targetValue, RateLimitGauge dataSource) {
            targetValue.incValue();
        }

        @Override
        public double initMetricValue(RateLimitGauge dataSource) {
            return 1.0;
        }
    }

    /**
     * 限流调用总成功数
     */
    public static class RateLimitRequestPassStrategy implements MetricValueAggregationStrategy<RateLimitGauge> {
        @Override
        public String getStrategyDescription() {
            return "total of passed request per period";
        }

        @Override
        public String getStrategyName() {
            return "ratelimit_rq_pass";
        }

        @Override
        public void updateMetricValue(StatMetric targetValue, RateLimitGauge dataSource) {
            if (RateLimitGauge.Result.PASSED == dataSource.getResult()) {
                targetValue.incValue();
            }
        }

        @Override
        public double initMetricValue(RateLimitGauge dataSource) {
            return RateLimitGauge.Result.PASSED == dataSource.getResult() ? 1.0 : 0.0;
        }
    }

    /**
     * 限流调用总限流数
     */
    public static class RateLimitRequestLimitStrategy implements MetricValueAggregationStrategy<RateLimitGauge> {
        @Override
        public String getStrategyDescription() {
            return "total of limited request per period";
        }

        @Override
        public String getStrategyName() {
            return "ratelimit_rq_limit";
        }

        @Override
        public void updateMetricValue(StatMetric targetValue, RateLimitGauge dataSource) {
            if (RateLimitGauge.Result.LIMITED == dataSource.getResult()) {
                targetValue.incValue();
            }
        }

        @Override
        public double initMetricValue(RateLimitGauge dataSource) {
            return RateLimitGauge.Result.LIMITED == dataSource.getResult() ? 1.0 : 0.0;
        }
    }

    /**
     * 熔断总数
     */
    public static class CircuitBreakerOpenStrategy implements MetricValueAggregationStrategy<CircuitBreakGauge> {

        @Override
        public String getStrategyDescription() {
            return "total of opened circuit breaker";
        }

        @Override
        public String getStrategyName() {
            return "circuitbreaker_open";
        }

        @Override
        public void updateMetricValue(StatMetric targetValue, CircuitBreakGauge dataSource) {
            if (null == dataSource.getCircuitBreakStatus()) {
                return;
            }

            if (OPEN == dataSource.getCircuitBreakStatus().getStatus()) {
                targetValue.incValue();
            } else if (HALF_OPEN == dataSource.getCircuitBreakStatus().getStatus()) {
                targetValue.addValue(-1);
            }
        }

        @Override
        public double initMetricValue(CircuitBreakGauge dataSource) {
            if (null == dataSource.getCircuitBreakStatus()) {
                return 0.0;
            }

            return dataSource.getCircuitBreakStatus().getStatus() == OPEN ? 1.0 : 0.0;
        }
    }

    /**
     * 熔断半开数
     */
    public static class CircuitBreakerHalfOpenStrategy implements MetricValueAggregationStrategy<CircuitBreakGauge> {
        @Override
        public String getStrategyDescription() {
            return "total of half-open circuit breaker";
        }

        @Override
        public String getStrategyName() {
            return "circuitbreaker_halfopen";
        }

        @Override
        public void updateMetricValue(StatMetric targetValue, CircuitBreakGauge dataSource) {
            if (null == dataSource.getCircuitBreakStatus()) {
                return;
            }

            if (targetValue instanceof StatStatefulMetric) {
                StatStatefulMetric markMetric = ((StatStatefulMetric)targetValue);
                switch (dataSource.getCircuitBreakStatus().getStatus()) {
                    case OPEN:
                        if (markMetric.contain(dataSource.getCircuitBreakStatus().getCircuitBreaker())) {
                            markMetric.addValue(-1);
                        }
                        break;
                    case HALF_OPEN:
                        markMetric.addMarkedName(dataSource.getCircuitBreakStatus().getCircuitBreaker());
                        markMetric.incValue();
                        break;
                    case CLOSE:
                        markMetric.removeMarkedName(dataSource.getCircuitBreakStatus().getCircuitBreaker());
                        targetValue.addValue(-1);
                        break;
                    default:
                }
            }
        }

        @Override
        public double initMetricValue(CircuitBreakGauge dataSource) {
            if (null == dataSource.getCircuitBreakStatus()) {
                return 0;
            }

            return dataSource.getCircuitBreakStatus().getStatus() == HALF_OPEN ? 1 : 0;
        }
    }
}
