package com.tencent.polaris.plugins.configuration.connector.polaris;

import com.tencent.polaris.api.config.global.ClusterType;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.Plugin;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.plugins.connector.grpc.ConnectionManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractPolarisConfigConnector implements Plugin {
    protected ConnectionManager connectionManager;

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
