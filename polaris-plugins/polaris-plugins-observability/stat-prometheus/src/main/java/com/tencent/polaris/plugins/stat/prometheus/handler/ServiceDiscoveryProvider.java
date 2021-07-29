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

package com.tencent.polaris.plugins.stat.prometheus.handler;

import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.compose.ServerServiceInfo;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.client.flow.BaseFlow;

public class ServiceDiscoveryProvider implements PushAddressProvider {
    private final Extensions extensions;
    private final ServerServiceInfo serverServiceInfo;

    public ServiceDiscoveryProvider(Extensions extensions, ServerServiceInfo serverServiceInfo) {
        this.extensions = extensions;
        this.serverServiceInfo = serverServiceInfo;
    }

    @Override
    public String getAddress() {
        if (null == extensions || null == serverServiceInfo) {
            return null;
        }

        Instance instance = BaseFlow.commonGetOneInstance(extensions,
                serverServiceInfo.getServiceKey(),
                serverServiceInfo.getRouters(),
                serverServiceInfo.getLbPolicy(),
                extensions.getConfiguration().getGlobal().getServerConnector().getProtocol(),
                extensions.getValueContext().getClientId());

        if (null != instance) {
            return instance.getHost() + ":" + instance.getPort();
        } else {
            return null;
        }
    }
}
