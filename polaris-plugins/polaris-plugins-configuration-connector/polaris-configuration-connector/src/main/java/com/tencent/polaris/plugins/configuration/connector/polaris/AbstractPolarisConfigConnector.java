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

package com.tencent.polaris.plugins.configuration.connector.polaris;

import com.tencent.polaris.api.config.global.ClusterType;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.Plugin;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.utils.MapUtils;
import com.tencent.polaris.factory.config.global.ServerConnectorConfigImpl;
import com.tencent.polaris.plugins.connector.grpc.ConnectionManager;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.AbstractStub;
import io.grpc.stub.MetadataUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractPolarisConfigConnector implements Plugin {
    protected ConnectionManager connectionManager;

    protected ServerConnectorConfigImpl connectorConfig;

    public String getName() {
        return getClass().getSimpleName();
    }

    public PluginType getType() {
        return PluginTypes.CONFIG_FILE_CONNECTOR.getBaseType();
    }

    public void init(InitContext ctx) throws PolarisException {
        CompletableFuture<String> readyFuture = new CompletableFuture<>();
        Map<ClusterType, CompletableFuture<String>> futures = new HashMap<>();
        futures.put(ClusterType.SERVICE_CONFIG_CLUSTER, readyFuture);
        connectionManager = new ConnectionManager(ctx, ctx.getConfig().getConfigFile().getServerConnector(), futures);
        this.connectorConfig = ctx.getConfig().getConfigFile().getServerConnector();
    }

    public void postContextInit(Extensions extensions) throws PolarisException {
        connectionManager.setExtensions(extensions);
    }

    public void destroy() {
        if (connectionManager != null) {
            connectionManager.destroy();
        }
    }
}
