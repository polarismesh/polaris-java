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

package com.tencent.polaris.configuration.client.flow;

import com.tencent.polaris.api.config.global.FlowConfig;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.configuration.api.core.ConfigFileGroup;
import com.tencent.polaris.configuration.api.core.ConfigFileGroupMetadata;
import com.tencent.polaris.configuration.api.flow.ConfigFileGroupFlow;
import com.tencent.polaris.configuration.client.internal.ConfigFileGroupManager;

public class DefaultConfigFileGroupFlow implements ConfigFileGroupFlow {
    private ConfigFileGroupManager manager;
    @Override
    public String getName() {
        return FlowConfig.DEFAULT_FLOW_NAME;
    }

    @Override
    public void setSDKContext(SDKContext sdkContext) {
        this.manager = new ConfigFileGroupManager(sdkContext);
    }

    @Override
    public ConfigFileGroup getConfigFileGroup(ConfigFileGroupMetadata metadata) {
        return manager.getConfigFileGroup(metadata);
    }
}
