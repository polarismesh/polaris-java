package com.tencent.polaris.plugins.configuration.connector.polaris;

import com.google.common.collect.Lists;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt64Value;

import com.tencent.polaris.api.config.global.ClusterType;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.exception.RetriableException;
import com.tencent.polaris.api.exception.ServerCodes;
import com.tencent.polaris.api.exception.ServerErrorResponseException;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.configuration.ConfigFile;
import com.tencent.polaris.api.plugin.configuration.ConfigFileConnector;
import com.tencent.polaris.api.plugin.configuration.ConfigFileResponse;
import com.tencent.polaris.client.pb.ConfigFileProto;
import com.tencent.polaris.client.pb.PolarisConfigGRPCGrpc;
import com.tencent.polaris.plugins.connector.grpc.Connection;
import com.tencent.polaris.plugins.connector.grpc.ConnectionManager;
import com.tencent.polaris.plugins.connector.grpc.GrpcUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author lepdou 2022-03-02
 */
public class PolarisConfigFileConnector implements ConfigFileConnector {

    private static final String OP_KEY_GET_CONFIG_FILE = "GetConfigFile";

    private ConnectionManager connectionManager;

    @Override
    public void init(InitContext ctx) throws PolarisException {
        CompletableFuture<String> readyFuture = new CompletableFuture<>();
        Map<ClusterType, CompletableFuture<String>> futures = new HashMap<>();
        futures.put(ClusterType.SERVICE_CONFIG_CLUSTER, readyFuture);
        connectionManager = new ConnectionManager(ctx, ctx.getConfig().getConfigFile().getServerConnector(), futures);
    }

    @Override
    public ConfigFileResponse getConfigFile(ConfigFile configFile) {
        Connection connection = null;

        try {
            connection = connectionManager.getConnection(OP_KEY_GET_CONFIG_FILE, ClusterType.SERVICE_CONFIG_CLUSTER);

            //grpc 调用
            PolarisConfigGRPCGrpc.PolarisConfigGRPCBlockingStub stub =
                PolarisConfigGRPCGrpc.newBlockingStub(connection.getChannel());
            //附加通用 header
            GrpcUtil.attachRequestHeader(stub, GrpcUtil.nextInstanceRegisterReqId());
            //执行调用
            ConfigFileProto.ConfigFileResponse response = stub.getConfigFile(transfer2DTO(configFile));

            return handleResponse(response);
        } catch (Throwable t) {
            // 网络访问异常
            if (connection != null) {
                connection.reportFail();
            }
            throw new RetriableException(ErrorCode.NETWORK_ERROR,
                                         String.format(
                                             "failed to load config file. namespace = %s, group = %s, file = %s",
                                             configFile.getNamespace(), configFile.getFileGroup(),
                                             configFile.getFileName()), t);
        } finally {
            if (connection != null) {
                connection.release(OP_KEY_GET_CONFIG_FILE);
            }
        }
    }

    @Override
    public ConfigFileResponse watchConfigFiles(List<ConfigFile> configFiles) {
        Connection connection = null;

        try {
            connection = connectionManager.getConnection(OP_KEY_GET_CONFIG_FILE, ClusterType.SERVICE_CONFIG_CLUSTER);

            //grpc 调用
            PolarisConfigGRPCGrpc.PolarisConfigGRPCBlockingStub stub =
                PolarisConfigGRPCGrpc.newBlockingStub(connection.getChannel());
            //附加通用 header
            GrpcUtil.attachRequestHeader(stub, GrpcUtil.nextInstanceRegisterReqId());
            //执行调用
            List<ConfigFileProto.ConfigFileDTO> dtos = Lists.newLinkedList();
            for (ConfigFile configFile : configFiles) {
                dtos.add(transfer2DTO(configFile));
            }
            ConfigFileProto.WatchConfigFileRequest request =
                ConfigFileProto.WatchConfigFileRequest.newBuilder().addAllWatchFiles(dtos).build();

            ConfigFileProto.ConfigFileResponse response = stub.watchConfigFiles(request);

            return handleResponse(response);
        } catch (Throwable t) {
            // 网络访问异常
            if (connection != null) {
                connection.reportFail();
            }
            throw new RetriableException(ErrorCode.NETWORK_ERROR, "[Config] failed to watch config file", t);
        } finally {
            if (connection != null) {
                connection.release(OP_KEY_GET_CONFIG_FILE);
            }
        }
    }

    @Override
    public String getName() {
        return "polaris";
    }

    @Override
    public PluginType getType() {
        return PluginTypes.CONFIG_FILE_CONNECTOR.getBaseType();
    }


    @Override
    public void postContextInit(Extensions extensions) throws PolarisException {
        connectionManager.setExtensions(extensions);
    }

    @Override
    public void destroy() {
        if (connectionManager != null) {
            connectionManager.destroy();
        }
    }

    private ConfigFileProto.ConfigFileDTO transfer2DTO(ConfigFile configFile) {
        ConfigFileProto.ConfigFileDTO.Builder builder = ConfigFileProto.ConfigFileDTO.newBuilder();

        builder.setNamespace(StringValue.newBuilder().setValue(configFile.getNamespace()).build());
        builder.setGroup(StringValue.newBuilder().setValue(configFile.getFileGroup()).build());
        builder.setFileName(StringValue.newBuilder().setValue(configFile.getFileName()).build());
        builder.setVersion(UInt64Value.newBuilder().setValue(configFile.getVersion()).build());

        return builder.build();
    }

    private ConfigFile transferFromDTO(ConfigFileProto.ConfigFileDTO configFileDTO) {
        if (configFileDTO == null) {
            return null;
        }

        ConfigFile configFile = new ConfigFile(configFileDTO.getNamespace().getValue(),
                                               configFileDTO.getGroup().getValue(),
                                               configFileDTO.getFileName().getValue());
        configFile.setContent(configFileDTO.getContent().getValue());
        configFile.setMd5(configFileDTO.getMd5().getValue());
        configFile.setVersion(configFileDTO.getVersion().getValue());

        return configFile;
    }

    private ConfigFileResponse handleResponse(ConfigFileProto.ConfigFileResponse response) {
        int code = response.getCode().getValue();
        //预期code，正常响应
        if (code == ServerCodes.EXECUTE_SUCCESS ||
            code == ServerCodes.NOT_FOUND_RESOURCE ||
            code == ServerCodes.DATA_NO_CHANGE) {
            ConfigFile loadedConfigFile = transferFromDTO(response.getConfigFile());
            return new ConfigFileResponse(code, response.getInfo().getValue(), loadedConfigFile);
        }
        //服务端异常
        throw ServerErrorResponseException.build(code, response.getInfo().getValue());
    }
}
