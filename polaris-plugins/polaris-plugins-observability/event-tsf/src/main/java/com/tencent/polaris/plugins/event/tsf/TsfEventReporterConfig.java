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

package com.tencent.polaris.plugins.event.tsf;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.polaris.api.config.verify.Verifier;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.factory.util.ConfigUtils;

/**
 * @author Haotian Zhang
 */
public class TsfEventReporterConfig implements Verifier {

    @JsonProperty
    private Boolean enable;

    @JsonProperty
    private String eventMasterIp;

    @JsonProperty
    private Integer eventMasterPort;

    @JsonProperty
    private String appId;

    @JsonProperty
    private String region;

    @JsonProperty
    private String instanceId;

    @JsonProperty
    private String tsfNamespaceId;

    @JsonProperty
    private String serviceName;

    @JsonProperty
    private String token;

    @JsonProperty
    private String applicationId;

    @Override
    public void verify() {
        ConfigUtils.validateNull(enable, "global.eventReporter.plugin.tsf.enable");
        if (!enable) {
            return;
        }
        ConfigUtils.validateString(eventMasterIp, "global.eventReporter.plugin.tsf.eventMasterIp");
        ConfigUtils.validatePositiveInteger(eventMasterPort, "global.eventReporter.plugin.tsf.eventMasterPort");
        ConfigUtils.validateString(appId, "global.eventReporter.plugin.tsf.appId");
        ConfigUtils.validateString(region, "global.eventReporter.plugin.tsf.region");
        ConfigUtils.validateString(instanceId, "global.eventReporter.plugin.tsf.instanceId");
        ConfigUtils.validateString(tsfNamespaceId, "global.eventReporter.plugin.tsf.tsfNamespaceId");
        ConfigUtils.validateString(serviceName, "global.eventReporter.plugin.tsf.serviceName");
        ConfigUtils.validateString(token, "global.eventReporter.plugin.tsf.token");
        ConfigUtils.validateString(applicationId, "global.eventReporter.plugin.tsf.applicationId");
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (defaultObject instanceof TsfEventReporterConfig) {
            TsfEventReporterConfig tsfEventReporterConfig = (TsfEventReporterConfig) defaultObject;
            if (null == enable) {
                setEnable(tsfEventReporterConfig.isEnable());
            }
            if (StringUtils.isBlank(eventMasterIp)) {
                setEventMasterIp(tsfEventReporterConfig.getEventMasterIp());
            }
            if (eventMasterPort == null || 0 == eventMasterPort) {
                setEventMasterPort(tsfEventReporterConfig.getEventMasterPort());
            }
            if (StringUtils.isBlank(appId)) {
                setAppId(tsfEventReporterConfig.getAppId());
            }
            if (StringUtils.isBlank(region)) {
                setRegion(tsfEventReporterConfig.getRegion());
            }
            if (StringUtils.isBlank(instanceId)) {
                setInstanceId(tsfEventReporterConfig.getInstanceId());
            }
            if (StringUtils.isBlank(tsfNamespaceId)) {
                setTsfNamespaceId(tsfEventReporterConfig.getTsfNamespaceId());
            }
            if (StringUtils.isBlank(serviceName)) {
                setServiceName(tsfEventReporterConfig.getServiceName());
            }
            if (StringUtils.isBlank(token)) {
                setToken(tsfEventReporterConfig.getToken());
            }
            if (StringUtils.isBlank(applicationId)) {
                setApplicationId(tsfEventReporterConfig.getApplicationId());
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

    public String getEventMasterIp() {
        return eventMasterIp;
    }

    public void setEventMasterIp(String eventMasterIp) {
        this.eventMasterIp = eventMasterIp;
    }

    public Integer getEventMasterPort() {
        return eventMasterPort;
    }

    public void setEventMasterPort(Integer eventMasterPort) {
        this.eventMasterPort = eventMasterPort;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getTsfNamespaceId() {
        return tsfNamespaceId;
    }

    public void setTsfNamespaceId(String tsfNamespaceId) {
        this.tsfNamespaceId = tsfNamespaceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    @Override
    public String toString() {
        return "TsfEventReporterConfig{" +
                "enable=" + enable +
                ", eventMasterIp='" + eventMasterIp + '\'' +
                ", eventMasterPort=" + eventMasterPort +
                ", appId='" + appId + '\'' +
                ", region='" + region + '\'' +
                ", instanceId='" + instanceId + '\'' +
                ", tsfNamespaceId='" + tsfNamespaceId + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", token='" + token + '\'' +
                ", applicationId='" + applicationId + '\'' +
                '}';
    }
}
