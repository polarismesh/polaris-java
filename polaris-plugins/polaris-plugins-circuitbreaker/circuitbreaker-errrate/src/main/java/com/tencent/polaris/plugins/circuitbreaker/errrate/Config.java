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

package com.tencent.polaris.plugins.circuitbreaker.errrate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.polaris.api.config.verify.Verifier;
import com.tencent.polaris.factory.util.ConfigUtils;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto.CbPolicy.ErrRateConfig;

/**
 * 错误率熔断插件的特定配置
 *
 * @author andrewshan
 * @date 2019/8/26
 */
public class Config implements Verifier {

    @JsonProperty
    private Integer requestVolumeThreshold;

    @JsonProperty
    private Integer errorRateThreshold;

    @JsonProperty
    private Integer metricNumBuckets;

    @JsonIgnore
    private double errRate;

    public Config() {

    }

    public Config(Config config, ErrRateConfig errRateConfig) {
        setRequestVolumeThreshold(config.getRequestVolumeThreshold());
        setMetricNumBuckets(config.getMetricNumBuckets());
        setErrorRateThreshold(config.getErrorRateThreshold());
        if (null != errRateConfig) {
            setErrorRateThreshold(errRateConfig.getErrorRateToOpen().getValue());
        }
    }

    public Integer getRequestVolumeThreshold() {
        return requestVolumeThreshold;
    }

    public void setRequestVolumeThreshold(Integer requestVolumeThreshold) {
        this.requestVolumeThreshold = requestVolumeThreshold;
    }

    public Integer getErrorRateThreshold() {
        return errorRateThreshold;
    }

    public double getErrRate() {
        return errRate;
    }

    public void setErrorRateThreshold(Integer errorRateThreshold) {
        this.errorRateThreshold = errorRateThreshold;
        if (null == this.errorRateThreshold) {
            return;
        }
        double errorRate = (double) this.errorRateThreshold / (double) 100;
        if (errorRate > 1.0) {
            errorRate = 1.0;
        }
        this.errRate = errorRate;
    }

    public Integer getMetricNumBuckets() {
        return metricNumBuckets;
    }

    public void setMetricNumBuckets(Integer metricNumBuckets) {
        this.metricNumBuckets = metricNumBuckets;
    }

    @Override
    public void verify() {
        ConfigUtils.validatePositive(requestVolumeThreshold, "requestVolumeThreshold");
        ConfigUtils.validatePositive(errorRateThreshold, "errorRateThreshold");
        ConfigUtils.validatePositive(metricNumBuckets, "metricNumBuckets");
        if (errorRateThreshold > 100) {
            throw new IllegalArgumentException("errorRateThreshold should be less than or equals to 100");
        }
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (null != defaultObject) {
            Config config = (Config) defaultObject;
            if (null == requestVolumeThreshold) {
                setRequestVolumeThreshold(config.getRequestVolumeThreshold());
            }
            if (null == errorRateThreshold) {
                setErrorRateThreshold(config.getErrorRateThreshold());
            }
            if (null == metricNumBuckets) {
                setMetricNumBuckets(config.getMetricNumBuckets());
            }
        }
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public String toString() {
        return "Config{" +
                "requestVolumeThreshold=" + requestVolumeThreshold +
                ", errorRateThreshold=" + errorRateThreshold +
                ", metricNumBuckets=" + metricNumBuckets +
                '}';
    }
}

