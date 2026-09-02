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

package com.tencent.polaris.plugins.ai.connector;

import com.tencent.polaris.api.config.global.ClusterType;
import com.tencent.polaris.api.config.plugin.DefaultPlugins;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.exception.RetriableException;
import com.tencent.polaris.api.exception.ServerCodes;
import com.tencent.polaris.api.exception.ServerErrorResponseException;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.skill.SkillConnector;
import com.tencent.polaris.api.plugin.skill.SkillDownloadRequest;
import com.tencent.polaris.api.plugin.skill.SkillDownloadResponse;
import com.tencent.polaris.api.plugin.skill.SkillGetRequest;
import com.tencent.polaris.api.plugin.skill.SkillGetResponse;
import com.tencent.polaris.api.plugin.skill.SkillListRequest;
import com.tencent.polaris.api.plugin.skill.SkillListResponse;
import com.tencent.polaris.factory.config.ai.AiConnectorConfigImpl;
import com.tencent.polaris.plugins.connector.grpc.Connection;
import com.tencent.polaris.plugins.connector.grpc.ConnectionManager;
import com.tencent.polaris.plugins.connector.grpc.GrpcUtil;
import com.tencent.polaris.specification.api.v1.skill.manage.PolarisSkillGrpc;
import com.tencent.polaris.specification.api.v1.skill.manage.PolarisSkillGRPCService;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * gRPC SkillConnector against PolarisSkill.
 */
public class PolarisSkillConnector implements SkillConnector {

    private static final String OP_GET_SKILL = "GetSkill";

    private static final String OP_LIST_SKILLS = "GetSkillList";

    private static final String OP_DOWNLOAD_SKILL = "DownloadSkill";

    private ConnectionManager connectionManager;

    private AiConnectorConfigImpl connectorConfig;

    @Override
    public String getName() {
        return DefaultPlugins.POLARIS_SKILL_CONNECTOR_TYPE;
    }

    @Override
    public PluginType getType() {
        return PluginTypes.SKILL_CONNECTOR.getBaseType();
    }

    @Override
    public void init(InitContext ctx) throws PolarisException {
        CompletableFuture<String> readyFuture = new CompletableFuture<>();
        Map<ClusterType, CompletableFuture<String>> futures = new HashMap<>();
        futures.put(ClusterType.SERVICE_DISCOVER_CLUSTER, readyFuture);
        this.connectorConfig = ctx.getConfig().getAi().getServerConnector();
        this.connectionManager = new ConnectionManager(ctx, connectorConfig, futures);
    }

    @Override
    public void postContextInit(Extensions ctx) throws PolarisException {
        connectionManager.setExtensions(ctx);
    }

    @Override
    public void destroy() {
        if (connectionManager != null) {
            connectionManager.destroy();
        }
    }

    @Override
    public SkillGetResponse getSkill(SkillGetRequest request) {
        Connection connection = null;
        SkillGetResponse result;
        try {
            connection = connectionManager.getConnection(OP_GET_SKILL, ClusterType.SERVICE_DISCOVER_CLUSTER);
            PolarisSkillGrpc.PolarisSkillBlockingStub stub = newStub(connection);
            PolarisSkillGRPCService.GetSkillResponse proto = stub.getSkill(SkillProtoConverter.toGetRequest(request));
            result = handleGetResponse(proto);
        } catch (Throwable throwable) {
            reportFail(connection, throwable);
            throw wrapNetworkError("failed to get skill", request.getNamespace(), request.getName(), throwable);
        } finally {
            release(connection, OP_GET_SKILL);
        }
        return result;
    }

    @Override
    public SkillListResponse listSkills(SkillListRequest request) {
        Connection connection = null;
        SkillListResponse result;
        try {
            connection = connectionManager.getConnection(OP_LIST_SKILLS, ClusterType.SERVICE_DISCOVER_CLUSTER);
            PolarisSkillGrpc.PolarisSkillBlockingStub stub = newStub(connection);
            PolarisSkillGRPCService.ListSkillsResponse proto = stub.getSkillList(SkillProtoConverter.toListRequest(request));
            result = handleListResponse(proto);
        } catch (Throwable throwable) {
            reportFail(connection, throwable);
            throw wrapNetworkError("failed to list skills", request.getNamespace(), request.getName(), throwable);
        } finally {
            release(connection, OP_LIST_SKILLS);
        }
        return result;
    }

    @Override
    public SkillDownloadResponse downloadSkill(SkillDownloadRequest request) {
        Connection connection = null;
        SkillDownloadResponse result;
        try {
            connection = connectionManager.getConnection(OP_DOWNLOAD_SKILL, ClusterType.SERVICE_DISCOVER_CLUSTER);
            PolarisSkillGrpc.PolarisSkillBlockingStub stub = newStub(connection);
            Iterator<PolarisSkillGRPCService.DownloadSkillResponse> iterator =
                    stub.downloadSkill(SkillProtoConverter.toDownloadRequest(request));
            result = handleDownloadResponse(SkillProtoConverter.assembleDownload(iterator));
        } catch (Throwable throwable) {
            reportFail(connection, throwable);
            throw wrapNetworkError("failed to download skill", request.getNamespace(), request.getName(), throwable);
        } finally {
            release(connection, OP_DOWNLOAD_SKILL);
        }
        return result;
    }

    private PolarisSkillGrpc.PolarisSkillBlockingStub newStub(Connection connection) {
        PolarisSkillGrpc.PolarisSkillBlockingStub stub = PolarisSkillGrpc.newBlockingStub(connection.getChannel());
        stub = GrpcUtil.attachRequestHeader(stub, GrpcUtil.nextInstanceRegisterReqId());
        stub = GrpcUtil.attachAccessToken(connectorConfig.getToken(), stub);
        return stub;
    }

    private SkillGetResponse handleGetResponse(PolarisSkillGRPCService.GetSkillResponse proto) {
        int code = proto.getCode();
        SkillGetResponse result;
        if (isExpectedCode(code)) {
            result = SkillProtoConverter.toGetResponse(proto);
        } else {
            throw ServerErrorResponseException.build(code, proto.getInfo());
        }
        return result;
    }

    private SkillListResponse handleListResponse(PolarisSkillGRPCService.ListSkillsResponse proto) {
        int code = proto.getCode();
        SkillListResponse result;
        if (isExpectedCode(code)) {
            result = SkillProtoConverter.toListResponse(proto);
        } else {
            throw ServerErrorResponseException.build(code, proto.getInfo());
        }
        return result;
    }

    private SkillDownloadResponse handleDownloadResponse(SkillDownloadResponse response) {
        SkillDownloadResponse result;
        if (isExpectedCode(response.getCode())) {
            result = response;
        } else {
            throw ServerErrorResponseException.build(response.getCode(), response.getInfo());
        }
        return result;
    }

    private boolean isExpectedCode(int code) {
        return code == ServerCodes.EXECUTE_SUCCESS || code == ServerCodes.NOT_FOUND_RESOURCE || code == 0;
    }

    private void reportFail(Connection connection, Throwable throwable) {
        if (connection != null && !(throwable instanceof PolarisException)) {
            connection.reportFail(ErrorCode.NETWORK_ERROR);
        }
        if (throwable instanceof PolarisException) {
            throw (PolarisException) throwable;
        }
        GrpcUtil.checkGrpcException(throwable);
    }

    private RetriableException wrapNetworkError(String message, String namespace, String name, Throwable throwable) {
        return new RetriableException(ErrorCode.NETWORK_ERROR,
                String.format("%s. namespace = %s, name = %s", message, namespace, name), throwable);
    }

    private void release(Connection connection, String opKey) {
        if (connection != null) {
            connection.release(opKey);
        }
    }
}
