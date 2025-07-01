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

package com.tencent.polaris.factory.config.consumer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.polaris.api.config.consumer.DiscoveryConfig;
import com.tencent.polaris.factory.util.ConfigUtils;

/**
 * Implementation of {@link DiscoveryConfig}.
 *
 * @author Haotian Zhang
 */
public class DiscoveryConfigImpl implements DiscoveryConfig {

    @JsonProperty
    private String serverConnectorId;

    @JsonProperty
    private Boolean enable;

    @Override
    public String getServerConnectorId() {
        return serverConnectorId;
    }

    public void setServerConnectorId(String serverConnectorId) {
        this.serverConnectorId = serverConnectorId;
    }

    @Override
    public boolean isEnable() {
        return enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    @Override
    public void verify() {
        ConfigUtils.validateString(serverConnectorId,
                "discovery.serverConnectorId or discoveries[?].serverConnectorId");
        ConfigUtils.validateNull(enable, "discovery.enable or discoveries[?].enable");
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (null != defaultObject) {
            DiscoveryConfig discoveryConfig = (DiscoveryConfig) defaultObject;
            if (null == enable) {
                setEnable(discoveryConfig.isEnable());
            }
        }
    }
}
