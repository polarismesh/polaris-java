/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

import com.google.protobuf.StringValue;
import com.tencent.polaris.api.config.global.ClusterType;
import com.tencent.polaris.api.exception.*;
import com.tencent.polaris.api.plugin.configuration.*;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.plugins.connector.grpc.Connection;
import com.tencent.polaris.plugins.connector.grpc.GrpcUtil;
import com.tencent.polaris.specification.api.v1.config.manage.ConfigFileProto;
import com.tencent.polaris.specification.api.v1.config.manage.ConfigFileResponseProto;
import com.tencent.polaris.specification.api.v1.config.manage.PolarisConfigGRPCGrpc;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PolarisConfigFileGroupConnector extends AbstractPolarisConfigConnector implements ConfigFileGroupConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(PolarisConfigFileGroupConnector.class);

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
            stub = GrpcUtil.attachRequestHeader(stub, GrpcUtil.nextInstanceRegisterReqId());
            stub = GrpcUtil.attachAccessToken(connectorConfig.getToken(), stub);
            ConfigFileResponseProto.ConfigClientListResponse response = stub.getConfigFileMetadataList(configFileGroupToProto(metadata, revision));
            LOGGER.debug("[Config] get GetConfigFileMetadataList response from remote. response = {}", response);

            // 处理响应
            int code = response.getCode().getValue();
            if (code == ServerCodes.EXECUTE_SUCCESS ||
                    code == ServerCodes.NOT_FOUND_RESOURCE ||
                    code == ServerCodes.DATA_NO_CHANGE) {
                String newRevision = response.getRevision().getValue();
                ConfigFileGroup configFileGroup = protoToConfigFileGroup(response);
                return new ConfigFileGroupResponse(code, response.getInfo().getValue(), newRevision, configFileGroup);
            }
            throw ServerErrorResponseException.build(code, response.getInfo().getValue());
        } catch (Throwable t) {
            if (t instanceof StatusRuntimeException) {
                StatusRuntimeException ex = (StatusRuntimeException) t;
                if (ex.getStatus().getCode() == Status.Code.UNIMPLEMENTED) {
                    throw new UnimplementedException(ex.getStatus().getDescription());
                }
            }

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
            configFile.setName(clientConfigFileInfo.getName().getValue());
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
