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

package com.tencent.polaris.factory.config.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.polaris.api.config.ai.AiConfig;
import com.tencent.polaris.factory.util.ConfigUtils;

/**
 * Implementation of AI / Skill configuration.
 */
public class AiConfigImpl implements AiConfig {

    @JsonProperty
    private AiConnectorConfigImpl serverConnector;

    @Override
    public void verify() {
        ConfigUtils.validateNull(serverConnector, "ai server connector");
        serverConnector.verify();
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (defaultObject == null) {
            return;
        }
        AiConfig sourceConfig = (AiConfig) defaultObject;
        if (serverConnector == null) {
            serverConnector = new AiConnectorConfigImpl();
        }
        serverConnector.setDefault(sourceConfig.getServerConnector());
    }

    @Override
    public AiConnectorConfigImpl getServerConnector() {
        return serverConnector;
    }

    public void setServerConnector(AiConnectorConfigImpl serverConnector) {
        this.serverConnector = serverConnector;
    }
}
