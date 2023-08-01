package com.tencent.polaris.plugins.configuration.connector.polaris;

import com.google.protobuf.StringValue;
import com.tencent.polaris.api.config.global.ClusterType;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.RetriableException;
import com.tencent.polaris.api.exception.ServerCodes;
import com.tencent.polaris.api.exception.ServerErrorResponseException;
import com.tencent.polaris.api.plugin.configuration.*;
import com.tencent.polaris.plugins.connector.grpc.Connection;
import com.tencent.polaris.plugins.connector.grpc.GrpcUtil;
import com.tencent.polaris.specification.api.v1.config.manage.ConfigFileProto;
import com.tencent.polaris.specification.api.v1.config.manage.ConfigFileResponseProto;
import com.tencent.polaris.specification.api.v1.config.manage.PolarisConfigGRPCGrpc;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class PolarisConfigFileGroupConnector extends AbstractPolarisConfigConnector implements ConfigFileGroupConnector {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final String OP_KEY_GET_CONFIG_METADATA_LIST = "GetConfigMetadataList";

    @Override
    public ConfigFileGroupResponse GetConfigFileMetadataList(ConfigFileGroupMetadata metadata, String revision) {
        if (revision == null) {
            revision = "";
        }

        Connection connection = null;
        try {
            connection = connectionManager.getConnection(OP_KEY_GET_CONFIG_METADATA_LIST, ClusterType.SERVICE_CONFIG_CLUSTER);
            PolarisConfigGRPCGrpc.PolarisConfigGRPCBlockingStub stub = PolarisConfigGRPCGrpc.newBlockingStub(connection.getChannel());
            GrpcUtil.attachRequestHeader(stub, GrpcUtil.nextInstanceRegisterReqId());
            ConfigFileResponseProto.ConfigClientListResponse response = stub.getConfigFileMetadataList(configFileGroupToProto(metadata, revision));
            // 处理响应
            int code = response.getCode().getValue();
            if (code == ServerCodes.EXECUTE_SUCCESS ||
                    code == ServerCodes.NOT_FOUND_RESOURCE ||
                    code == ServerCodes.DATA_NO_CHANGE) {
                String newlyRevision = response.getRevision().getValue();
                ConfigFileGroup configFileGroup = protoToConfigFileGroup(response);
                return new ConfigFileGroupResponse(code, response.getInfo().getValue(), newlyRevision, configFileGroup);
            }
            throw ServerErrorResponseException.build(code, response.getInfo().getValue());
        } catch (Throwable t) {
            ErrorCode errorCode = ErrorCode.NETWORK_ERROR;
            if (t instanceof ParseException) {
                errorCode = ErrorCode.INTERNAL_ERROR;
            } else if (connection != null) {
                connection.reportFail(ErrorCode.NETWORK_ERROR);
            }

            throw new RetriableException(errorCode,
                    String.format(
                            "failed to get config file metadata list. namespace = %s, group = %s, reversion = %s",
                            metadata.getNamespace(), metadata.getFileGroupName(),
                            revision), t);
        } finally {
            if (connection != null) {
                connection.release(OP_KEY_GET_CONFIG_METADATA_LIST);
            }
        }
    }

    private ConfigFileProto.ConfigFileGroupRequest configFileGroupToProto(ConfigFileGroupMetadata configFileGroupMetadata, String revision) {
        ConfigFileProto.ConfigFileGroup.Builder configFileGroupProto = ConfigFileProto.ConfigFileGroup.newBuilder();
        configFileGroupProto.setNamespace(StringValue.newBuilder().setValue(configFileGroupMetadata.getNamespace()).build());
        configFileGroupProto.setName(StringValue.newBuilder().setValue(configFileGroupMetadata.getFileGroupName()).build());

        ConfigFileProto.ConfigFileGroupRequest.Builder builder = ConfigFileProto.ConfigFileGroupRequest.newBuilder();
        builder.setRevision(StringValue.newBuilder().setValue(revision).build());
        builder.setConfigFileGroup(configFileGroupProto.build());
        return builder.build();
    }

    private ConfigFileGroup protoToConfigFileGroup(ConfigFileResponseProto.ConfigClientListResponse proto) throws ParseException {
        if (proto == null) {
            return null;
        }

        ConfigFileGroup configFileGroup = new ConfigFileGroup();
        configFileGroup.setNamespace(proto.getNamespace());
        configFileGroup.setFileGroupName(proto.getGroup());

        List<ConfigFile> configFileList = new ArrayList<>();
        for (ConfigFileProto.ClientConfigFileInfo clientConfigFileInfo : proto.getConfigFileInfosList()) {
            ConfigFile configFile = new ConfigFile(clientConfigFileInfo.getNamespace().getValue(),
                    clientConfigFileInfo.getGroup().getValue(),
                    clientConfigFileInfo.getFileName().getValue());
            configFile.setReleaseTime(dateFormat.parse(clientConfigFileInfo.getReleaseTime().getValue()));
            configFileList.add(configFile);
        }
        configFileGroup.setConfigFileList(configFileList);
        return configFileGroup;
    }

    @Override
    public String getName() {
        return "polaris";
    }
}
