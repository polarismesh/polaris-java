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

package com.tencent.polaris.factory.config.consumer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tencent.polaris.api.config.consumer.OutlierDetectionConfig;
import com.tencent.polaris.api.config.verify.DefaultValues;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.factory.config.plugin.PluginConfigImpl;
import com.tencent.polaris.factory.util.ConfigUtils;
import com.tencent.polaris.factory.util.TimeStrJsonDeserializer;
import java.util.List;

/**
 * OutlierDetectionConfigImpl.java
 *
 * @author andrewshan
 * @date 2019/9/18
 */
public class OutlierDetectionConfigImpl extends PluginConfigImpl implements OutlierDetectionConfig {

    @JsonProperty
    private When when;

    @JsonProperty
    private List<String> chain;

    @JsonProperty
    @JsonDeserialize(using = TimeStrJsonDeserializer.class)
    private Long checkPeriod;

    @Override
    public When getWhen() {
        return when;
    }

    public void setWhen(When when) {
        this.when = when;
    }

    @Override
    public List<String> getChain() {
        return chain;
    }

    public void setChain(List<String> chain) {
        this.chain = chain;
    }

    @Override
    public long getCheckPeriod() {
        if (null == checkPeriod) {
            return 0;
        }
        return checkPeriod;
    }

    public void setCheckPeriod(long checkPeriod) {
        this.checkPeriod = checkPeriod;
    }

    @Override
    public void verify() {
        ConfigUtils.validateNull(when, "outlierDetection.enable");
        if (when == When.never) {
            return;
        }
        ConfigUtils.validateIntervalWithMin(checkPeriod, DefaultValues.MIN_TIMING_INTERVAL_MS,
                "outlierDetection.checkPeriod");
        if (CollectionUtils.isEmpty(chain)) {
            throw new IllegalArgumentException("outlierDetection.chain cannot be empty");
        }
        verifyPluginConfig();
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (null != defaultObject) {
            OutlierDetectionConfig outlierDetectionConfig = (OutlierDetectionConfig) defaultObject;
            if (null == when) {
                setWhen(outlierDetectionConfig.getWhen());
            }
            if (null == checkPeriod) {
                setCheckPeriod(outlierDetectionConfig.getCheckPeriod());
            }
            if (CollectionUtils.isEmpty(chain)) {
                setChain(outlierDetectionConfig.getChain());
            }
            if (when != When.never) {
                setDefaultPluginConfig(outlierDetectionConfig);
            }
        }

    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public String toString() {
        return "OutlierDetectionConfigImpl{" +
                "when=" + when +
                ", chain=" + chain +
                ", checkPeriod=" + checkPeriod +
                "} " + super.toString();
    }
}