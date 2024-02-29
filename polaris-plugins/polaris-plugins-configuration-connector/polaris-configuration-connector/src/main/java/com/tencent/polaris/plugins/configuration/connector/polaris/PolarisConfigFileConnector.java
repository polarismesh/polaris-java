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

import com.google.common.collect.Lists;
import com.google.protobuf.BoolValue;
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
import com.tencent.polaris.api.plugin.configuration.*;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.plugins.connector.grpc.Connection;
import com.tencent.polaris.plugins.connector.grpc.ConnectionManager;
import com.tencent.polaris.plugins.connector.grpc.GrpcUtil;

import com.tencent.polaris.specification.api.v1.config.manage.ConfigFileProto;
import com.tencent.polaris.specification.api.v1.config.manage.ConfigFileResponseProto;
import com.tencent.polaris.specification.api.v1.config.manage.PolarisConfigGRPCGrpc;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author lepdou 2022-03-02
 */
public class PolarisConfigFileConnector extends AbstractPolarisConfigConnector implements ConfigFileConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(PolarisConfigFileConnector.class);

    private static final String OP_KEY_GET_CONFIG_FILE = "GetConfigFile";
    private static final String OP_KEY_CREATE_CONFIG_FILE = "CreateConfigFile";
    private static final String OP_KEY_UPDATE_CONFIG_FILE = "UpdateConfigFile";
    private static final String OP_KEY_RELEASE_CONFIG_FILE = "ReleaseConfigFile";

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
            ConfigFileResponseProto.ConfigClientResponse response = stub.getConfigFile(configFile.toClientConfigFileInfo());
            LOGGER.debug("[Config] get getConfigFile response from remote. fileName = {}, response = {}", configFile.getFileName(), response);

            return handleResponse(response);
        } catch (Throwable t) {
            // 网络访问异常
            if (connection != null) {
                connection.reportFail(ErrorCode.NETWORK_ERROR);
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
            List<ConfigFileProto.ClientConfigFileInfo> dtos = Lists.newLinkedList();
            for (ConfigFile configFile : configFiles) {
                dtos.add(configFile.toClientConfigFileInfo());
            }
            ConfigFileProto.ClientWatchConfigFileRequest request =
                    ConfigFileProto.ClientWatchConfigFileRequest.newBuilder().addAllWatchFiles(dtos).build();

            ConfigFileResponseProto.ConfigClientResponse response = stub.watchConfigFiles(request);

            return handleResponse(response);
        } catch (Throwable t) {
            // 网络访问异常
            if (connection != null) {
                connection.reportFail(ErrorCode.NETWORK_ERROR);
            }
            throw new RetriableException(ErrorCode.NETWORK_ERROR, "[Config] failed to watch config file", t);
        } finally {
            if (connection != null) {
                connection.release(OP_KEY_GET_CONFIG_FILE);
            }
        }
    }

    @Override
    public ConfigFileResponse createConfigFile(ConfigFile configFile) {
        Connection connection = null;

        try {
            connection = connectionManager.getConnection(OP_KEY_CREATE_CONFIG_FILE, ClusterType.SERVICE_CONFIG_CLUSTER);

            //grpc 调用
            PolarisConfigGRPCGrpc.PolarisConfigGRPCBlockingStub stub =
                    PolarisConfigGRPCGrpc.newBlockingStub(connection.getChannel());
            //附加通用 header
            GrpcUtil.attachRequestHeader(stub, GrpcUtil.nextInstanceRegisterReqId());
            //执行调用
            ConfigFileResponseProto.ConfigClientResponse response = stub.createConfigFile(transfer2ConfigFile(configFile));

            return handleResponse(response);
        } catch (Throwable t) {
            // 网络访问异常
            if (connection != null) {
                connection.reportFail(ErrorCode.NETWORK_ERROR);
            }
            checkGrpcUnImplement(t);
            throw new RetriableException(ErrorCode.NETWORK_ERROR,
                    String.format(
                            "failed to create config file. namespace = %s, group = %s, file = %s, content = %s",
                            configFile.getNamespace(), configFile.getFileGroup(),
                            configFile.getFileName(), configFile.getContent()), t);
        } finally {
            if (connection != null) {
                connection.release(OP_KEY_CREATE_CONFIG_FILE);
            }
        }
    }

    @Override
    public ConfigFileResponse updateConfigFile(ConfigFile configFile) {
        Connection connection = null;

        try {
            connection = connectionManager.getConnection(OP_KEY_UPDATE_CONFIG_FILE, ClusterType.SERVICE_CONFIG_CLUSTER);

            //grpc 调用
            PolarisConfigGRPCGrpc.PolarisConfigGRPCBlockingStub stub =
                    PolarisConfigGRPCGrpc.newBlockingStub(connection.getChannel());
            //附加通用 header
            GrpcUtil.attachRequestHeader(stub, GrpcUtil.nextInstanceRegisterReqId());
            //执行调用
            ConfigFileResponseProto.ConfigClientResponse response = stub.updateConfigFile(transfer2ConfigFile(configFile));

            return handleResponse(response);
        } catch (Throwable t) {
            // 网络访问异常
            if (connection != null) {
                connection.reportFail(ErrorCode.NETWORK_ERROR);
            }
            checkGrpcUnImplement(t);
            throw new RetriableException(ErrorCode.NETWORK_ERROR,
                    String.format(
                            "failed to update config file. namespace = %s, group = %s, file = %s, content = %s",
                            configFile.getNamespace(), configFile.getFileGroup(),
                            configFile.getFileName(), configFile.getContent()), t);
        } finally {
            if (connection != null) {
                connection.release(OP_KEY_UPDATE_CONFIG_FILE);
            }
        }
    }

    @Override
    public ConfigFileResponse releaseConfigFile(ConfigFile configFile) {
        Connection connection = null;

        try {
            connection = connectionManager.getConnection(OP_KEY_RELEASE_CONFIG_FILE, ClusterType.SERVICE_CONFIG_CLUSTER);

            //grpc 调用
            PolarisConfigGRPCGrpc.PolarisConfigGRPCBlockingStub stub =
                    PolarisConfigGRPCGrpc.newBlockingStub(connection.getChannel());
            //附加通用 header
            GrpcUtil.attachRequestHeader(stub, GrpcUtil.nextInstanceRegisterReqId());
            //执行调用
            ConfigFileResponseProto.ConfigClientResponse response = stub.publishConfigFile(transfer2ConfigFileRelease(configFile));

            return handleResponse(response);
        } catch (Throwable t) {
            // 网络访问异常
            if (connection != null) {
                connection.reportFail(ErrorCode.NETWORK_ERROR);
            }
            checkGrpcUnImplement(t);
            throw new RetriableException(ErrorCode.NETWORK_ERROR,
                    String.format(
                            "failed to release config file. namespace = %s, group = %s, file = %s",
                            configFile.getNamespace(), configFile.getFileGroup(),
                            configFile.getFileName()), t);
        } finally {
            if (connection != null) {
                connection.release(OP_KEY_RELEASE_CONFIG_FILE);
            }
        }
    }

    @Override
    public ConfigFileResponse upsertAndPublishConfigFile(ConfigPublishFile request) {
        Connection connection = null;

        try {
            connection = connectionManager.getConnection(OP_KEY_RELEASE_CONFIG_FILE, ClusterType.SERVICE_CONFIG_CLUSTER);

            //grpc 调用
            PolarisConfigGRPCGrpc.PolarisConfigGRPCBlockingStub stub =
                    PolarisConfigGRPCGrpc.newBlockingStub(connection.getChannel());
            //附加通用 header
            GrpcUtil.attachRequestHeader(stub, GrpcUtil.nextInstanceRegisterReqId());
            //执行调用
            ConfigFileResponseProto.ConfigClientResponse response = stub.upsertAndPublishConfigFile(request.toSpec());
            return handleResponse(response);
        } catch (Throwable t) {
            // 网络访问异常
            if (connection != null) {
                connection.reportFail(ErrorCode.NETWORK_ERROR);
            }
            checkGrpcUnImplement(t);
            throw new RetriableException(ErrorCode.NETWORK_ERROR,
                    String.format(
                            "failed to upsert and publish config file. namespace = %s, group = %s, file = %s",
                            request.getNamespace(), request.getFileGroup(),
                            request.getFileName()), t);
        } finally {
            if (connection != null) {
                connection.release(OP_KEY_RELEASE_CONFIG_FILE);
            }
        }    }

    @Override
    public String getName() {
        return "polaris";
    }

    private ConfigFile transferFromDTO(ConfigFileProto.ClientConfigFileInfo configFileDTO) {
        if (configFileDTO == null) {
            return null;
        }

        ConfigFile configFile = new ConfigFile(configFileDTO.getNamespace().getValue(),
                configFileDTO.getGroup().getValue(),
                configFileDTO.getFileName().getValue());
        configFile.setContent(configFileDTO.getContent().getValue());
        configFile.setMd5(configFileDTO.getMd5().getValue());
        configFile.setVersion(configFileDTO.getVersion().getValue());
        if (configFileDTO.getEncrypted().getValue()) {
            configFileDTO.getTagsList().forEach(tag -> {
                if (tag.getKey().getValue().equals("internal-datakey")) {
                    configFile.setDataKey(tag.getValue().getValue());
                }
            });
        }

        return configFile;
    }

    private ConfigFileProto.ConfigFile transfer2ConfigFile(ConfigFile configFile) {
        ConfigFileProto.ConfigFile.Builder builder = ConfigFileProto.ConfigFile.newBuilder();

        builder.setNamespace(StringValue.newBuilder().setValue(configFile.getNamespace()).build());
        builder.setGroup(StringValue.newBuilder().setValue(configFile.getFileGroup()).build());
        builder.setName(StringValue.newBuilder().setValue(configFile.getFileName()).build());
        builder.setContent(StringValue.newBuilder().setValue(configFile.getContent()).build());

        return builder.build();
    }

    private ConfigFileProto.ConfigFileRelease transfer2ConfigFileRelease(ConfigFile configFile) {
        ConfigFileProto.ConfigFileRelease.Builder builder = ConfigFileProto.ConfigFileRelease.newBuilder();

        builder.setNamespace(StringValue.newBuilder().setValue(configFile.getNamespace()).build());
        builder.setGroup(StringValue.newBuilder().setValue(configFile.getFileGroup()).build());
        builder.setFileName(StringValue.newBuilder().setValue(configFile.getFileName()).build());

        return builder.build();
    }

    private ConfigFileResponse handleResponse(ConfigFileResponseProto.ConfigClientResponse response) {
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
