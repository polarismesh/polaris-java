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

package com.tencent.polaris.plugin.lossless.deregister;

import com.sun.net.httpserver.HttpHandler;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.HttpServerAware;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.lossless.InstanceProperties;
import com.tencent.polaris.api.plugin.lossless.LosslessPolicy;
import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;

import java.util.Map;

public class DeregisterLosslessPolicy  implements LosslessPolicy, HttpServerAware {

    private static final Logger LOG = LoggerFactory.getLogger(DeregisterLosslessPolicy.class);

    private static final String OFFLINE_PATH = "/offline";

    @Override
    public String getHost() {

    }

    @Override
    public int getPort() {
        return 0;
    }

    @Override
    public Map<String, HttpHandler> getHandlers() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public PluginType getType() {
        return null;
    }

    @Override
    public void init(InitContext ctx) throws PolarisException {

    }

    @Override
    public void postContextInit(Extensions ctx) throws PolarisException {

    }

    @Override
    public void destroy() {

    }

    @Override
    public int getOrder() {
        return 1;
    }

    @Override
    public void buildInstanceProperties(InstanceProperties instanceProperties) {

    }

    @Override
    public void losslessRegister(InstanceProperties instanceProperties) {

    }
}
