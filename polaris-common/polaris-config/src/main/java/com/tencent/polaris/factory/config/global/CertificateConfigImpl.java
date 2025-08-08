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

package com.tencent.polaris.factory.config.global;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tencent.polaris.api.config.global.CertificateConfig;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.factory.config.plugin.PluginConfigImpl;
import com.tencent.polaris.factory.util.ConfigUtils;
import com.tencent.polaris.factory.util.TimeStrJsonDeserializer;

public class CertificateConfigImpl extends PluginConfigImpl implements CertificateConfig {

    @JsonProperty
    private Boolean enable;

    @JsonProperty
    private String commonName;

    @JsonProperty
    @JsonDeserialize(using = TimeStrJsonDeserializer.class)
    private Long validityDuration;

    @JsonProperty
    @JsonDeserialize(using = TimeStrJsonDeserializer.class)
    private Long refreshBefore;

    @JsonProperty
    @JsonDeserialize(using = TimeStrJsonDeserializer.class)
    private Long watchInterval;

    @JsonProperty
    private String pluginName;

    @Override
    public boolean isEnable() {
        return enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    @Override
    public String getCommonName() {
        return commonName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    @Override
    public Long getValidityDuration() {
        return validityDuration;
    }

    public void setValidityDuration(Long validityDuration) {
        this.validityDuration = validityDuration;
    }

    @Override
    public Long getRefreshBefore() {
        return refreshBefore;
    }

    public void setRefreshBefore(Long refreshBefore) {
        this.refreshBefore = refreshBefore;
    }

    @Override
    public Long getWatchInterval() {
        return watchInterval;
    }

    public void setWatchInterval(Long watchInterval) {
        this.watchInterval = watchInterval;
    }

    @Override
    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    @Override
    public void verify() {
        ConfigUtils.validateNull(enable, "certificate.enable");
        if (isEnable()) {
            ConfigUtils.validateString(commonName, "certificate.commonName");
            ConfigUtils.validateIntervalWithMin(validityDuration, 1800000, "certificate.validityDuration");
            ConfigUtils.validateIntervalWithMin(refreshBefore, 60000, "certificate.refreshBefore");
            ConfigUtils.validateIntervalWithMin(watchInterval, 10000, "certificate.watchInterval");
            ConfigUtils.validateString(pluginName, "certificate.pluginName");
        }
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (null != defaultObject) {
            CertificateConfig certificateConfig = (CertificateConfig) defaultObject;
            if (null == enable) {
                setEnable(certificateConfig.isEnable());
            }
            if (StringUtils.isBlank(commonName)) {
                setCommonName(certificateConfig.getCommonName());
            }
            if (validityDuration == null) {
                setValidityDuration(certificateConfig.getValidityDuration());
            }
            if (refreshBefore == null) {
                setRefreshBefore(certificateConfig.getRefreshBefore());
            }
            if (watchInterval == null) {
                setWatchInterval(certificateConfig.getWatchInterval());
            }
            if (StringUtils.isBlank(pluginName)) {
                setPluginName(certificateConfig.getPluginName());
            }
        }
    }

    @Override
    public String toString() {
        return "CertificateConfigImpl{" +
                "enable=" + enable +
                ", commonName='" + commonName + '\'' +
                ", validityDuration=" + validityDuration +
                ", refreshBefore=" + refreshBefore +
                ", watchInterval=" + watchInterval +
                ", pluginName='" + pluginName + '\'' +
                '}';
    }
}
