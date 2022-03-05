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

import com.tencent.polaris.api.config.consumer.LoadBalanceConfig;
import com.tencent.polaris.api.config.consumer.ServiceRouterConfig;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.flow.BaseFlow;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceDiscoveryProvider {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceDiscoveryProvider.class);

    private final Extensions extensions;

    private final ServiceKey pushService;

    private final List<String> coreRouters = new ArrayList<>();

    private final String pushAddress;

    public ServiceDiscoveryProvider(Extensions extensions, PrometheusPushHandlerConfig pushHandlerConfig) {
        this.extensions = extensions;
        if (!StringUtils.isBlank(pushHandlerConfig.getPushgatewayService()) && !StringUtils
                .isBlank(pushHandlerConfig.getPushgatewayNamespace())) {
            pushService = new ServiceKey(pushHandlerConfig.getPushgatewayNamespace(),
                    pushHandlerConfig.getPushgatewayService());
            pushAddress = null;
        } else {
            pushService = null;
            pushAddress = pushHandlerConfig.getPushgatewayAddress();
        }
        coreRouters.add(ServiceRouterConfig.DEFAULT_ROUTER_METADATA);
    }

    public String getAddress() {
        if (null == pushService && StringUtils.isBlank(pushAddress)) {
            return null;
        }
        if (!StringUtils.isBlank(pushAddress)) {
            return pushAddress;
        }
        Instance instance = null;
        try {
            instance = BaseFlow.commonGetOneInstance(extensions,
                    pushService,
                    coreRouters,
                    LoadBalanceConfig.LOAD_BALANCE_WEIGHTED_RANDOM,
                    extensions.getConfiguration().getGlobal().getServerConnector().getProtocol(),
                    extensions.getValueContext().getClientId());
        } catch (Exception e) {
            LOG.error("fail to discover service " + pushService, e);
        }
        if (null != instance) {
            return instance.getHost() + ":" + instance.getPort();
        }
        return null;
    }
}
