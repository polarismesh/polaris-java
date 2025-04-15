/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.polaris.plugins.event.pushgateway;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.polaris.api.config.verify.Verifier;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.factory.util.ConfigUtils;

/**
 * Polaris push gateway event reporter config.
 *
 * @author Haotian Zhang
 */
public class PushGatewayEventReporterConfig implements Verifier {

    @JsonProperty
    private Boolean enable;

    @JsonProperty
    private String address;

    @JsonProperty
    private Integer eventQueueSize;

    @JsonProperty
    private Integer maxBatchSize;

    @JsonProperty
    private String namespace;

    @JsonProperty
    private String service;

    @Override
    public void verify() {
        ConfigUtils.validateNull(enable, "global.eventReporter.plugin.pushgateway.enable");
        if (!enable) {
            return;
        }
        ConfigUtils.validateString(address, "global.eventReporter.plugin.pushgateway.address");
        ConfigUtils.validatePositive(eventQueueSize, "global.eventReporter.plugin.pushgateway.eventQueueSize");
        ConfigUtils.validatePositive(maxBatchSize, "global.eventReporter.plugin.pushgateway.maxBatchSize");
        ConfigUtils.validateString(namespace, "global.eventReporter.plugin.pushgateway.namespace");
        ConfigUtils.validateString(service, "global.eventReporter.plugin.pushgateway.service");
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (defaultObject instanceof PushGatewayEventReporterConfig) {
            PushGatewayEventReporterConfig pushGatewayEventReporterConfig = (PushGatewayEventReporterConfig) defaultObject;
            if (null == enable) {
                setEnable(pushGatewayEventReporterConfig.isEnable());
            }
            if (StringUtils.isBlank(address)) {
                setAddress(pushGatewayEventReporterConfig.getAddress());
            }
            if (null == eventQueueSize) {
                setEventQueueSize(pushGatewayEventReporterConfig.getEventQueueSize());
            }
            if (null == maxBatchSize) {
                setMaxBatchSize(pushGatewayEventReporterConfig.getMaxBatchSize());
            }
            if (StringUtils.isBlank(namespace)) {
                setNamespace(pushGatewayEventReporterConfig.getNamespace());
            }
            if (StringUtils.isBlank(service)) {
                setService(pushGatewayEventReporterConfig.getService());
            }
        }
    }

    public boolean isEnable() {
        if (null == enable) {
            enable = false;
        }
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getEventQueueSize() {
        return eventQueueSize;
    }

    public void setEventQueueSize(Integer eventQueueSize) {
        this.eventQueueSize = eventQueueSize;
    }

    public Integer getMaxBatchSize() {
        return maxBatchSize;
    }

    public void setMaxBatchSize(Integer maxBatchSize) {
        this.maxBatchSize = maxBatchSize;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    @Override
    public String toString() {
        return "PushGatewayEventReporterConfig{" +
                "enable=" + enable +
                ", address='" + address + '\'' +
                ", eventQueueSize=" + eventQueueSize +
                ", maxBatchSize=" + maxBatchSize +
                ", namespace='" + namespace + '\'' +
                ", service='" + service + '\'' +
                '}';
    }
}
