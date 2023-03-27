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

import com.google.gson.JsonObject;
import com.tencent.polaris.api.exception.ServerCodes;
import com.tencent.polaris.api.exception.ServerErrorResponseException;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.configuration.ConfigFile;
import com.tencent.polaris.api.plugin.configuration.ConfigFileResponse;
import com.tencent.polaris.plugins.configuration.connector.polaris.model.ConfigClientResponse;
import com.tencent.polaris.plugins.configuration.connector.polaris.rest.RestOperator;
import com.tencent.polaris.plugins.configuration.connector.polaris.rest.RestUtils;
import okhttp3.HttpUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * @author fabian4 2023-03-01
 */
public class OpenapiService {

    public static OpenapiService INSTANCE;

    private static final Logger LOG = LoggerFactory.getLogger(OpenapiService.class);

    private final String token;

    private final List<String> address;

    private final RestOperator restOperator;

    private OpenapiService(InitContext ctx) {
        this.restOperator = new RestOperator();
        this.address = RestUtils.getAddress(ctx.getConfig().getConfigFile().getServerConnector());
        this.token = ctx.getConfig().getConfigFile().getServerConnector().getToken();
    }

    public static void initInstance(InitContext ctx) {
        INSTANCE = new OpenapiService(ctx);
    }

    public ConfigFileResponse getConfigFile(ConfigFile configFile) {
        ConfigClientResponse configClientResponse = getConfigFile(RestUtils.toConfigFileUrl(address), token, configFile);
        int code = Integer.parseInt(configClientResponse.getCode());
        //预期code，正常响应
        if (code == ServerCodes.EXECUTE_SUCCESS ||
                code == ServerCodes.NOT_FOUND_RESOURCE ||
                code == ServerCodes.DATA_NO_CHANGE) {
            ConfigFile loadedConfigFile = RestUtils.transferFromDTO(configClientResponse.getConfigFile());
            return new ConfigFileResponse(code, configClientResponse.getInfo(), loadedConfigFile);
        }
        throw ServerErrorResponseException.build(code, configClientResponse.getInfo());
    }

    public void createConfigFile(ConfigFile configFile) {
        createConfigFile(RestUtils.toConfigFileUrl(address), token, configFile);
    }

    public void updateConfigFile(ConfigFile configFile) {
        updateConfigFile(RestUtils.toConfigFileUrl(address), token, configFile);
    }

    public void upsertConfigFile(ConfigFile configFile) {
        upsertConfigFile(RestUtils.toConfigFileUrl(address), token, configFile);
    }

    public void releaseConfigFile(ConfigFile configFile) {
        releaseConfigFile(RestUtils.toReleaseConfigFileUrl(address), token, configFile);
    }

    public ConfigClientResponse getConfigFile(String url, String token, ConfigFile configFile) {
        url = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder()
                .addQueryParameter("name", configFile.getFileName())
                .addQueryParameter("group", configFile.getFileGroup())
                .addQueryParameter("namespace", configFile.getNamespace())
                .build().toString();

        return restOperator.doGet(url, token);
    }

    public ConfigClientResponse createConfigFile(String url, String token, ConfigFile configFile) {
        JsonObject params = RestUtils.getParams(configFile);
        params.addProperty("content", configFile.getContent());
        ConfigClientResponse response = restOperator.doPost(url, token, params.toString());
        LOG.info("[Polaris] creat configuration file success: Namespace={}, FileGroup={}, FileName={}, Content={}",
                configFile.getNamespace(), configFile.getFileGroup(), configFile.getFileName(), configFile.getContent());
        return response;
    }

    public ConfigClientResponse updateConfigFile(String url, String token, ConfigFile configFile) {
        JsonObject params = RestUtils.getParams(configFile);
        params.addProperty("content", configFile.getContent());
        ConfigClientResponse response = restOperator.doPut(url, token, params.toString());
        LOG.info("[Polaris] update configuration file success: Namespace={}, FileGroup={}, FileName={}, Content={}",
                configFile.getNamespace(), configFile.getFileGroup(), configFile.getFileName(), configFile.getContent());
        return response;
    }

    public ConfigClientResponse upsertConfigFile(String url, String token, ConfigFile configFile) {
        JsonObject params = RestUtils.getParams(configFile);
        params.addProperty("content", configFile.getContent());
        ConfigClientResponse response = null;
        try {
            response = restOperator.doPost(url, token, params.toString());
        } catch (ServerErrorResponseException e) {
            if (e.getServerCode() == ServerCodes.EXISTED_RESOURCE) {
                response = restOperator.doPut(url, token, params.toString());
            } else {
                throw e;
            }
        }

        LOG.info("[Polaris] upsert configuration file success: Namespace={}, FileGroup={}, FileName={}, Content={}",
                configFile.getNamespace(), configFile.getFileGroup(), configFile.getFileName(), configFile.getContent());
        return response;
    }

    public ConfigClientResponse releaseConfigFile(String url, String token, ConfigFile configFile) {
        JsonObject params = new JsonObject();
        params.addProperty("fileName", configFile.getFileName());
        params.addProperty("group", configFile.getFileGroup());
        params.addProperty("namespace", configFile.getNamespace());
        ConfigClientResponse response = restOperator.doPost(url, token, params.toString());
        LOG.info("[Polaris] release configuration file success: Namespace={}, FileGroup={}, FileName={}",
                configFile.getNamespace(), configFile.getFileGroup(), configFile.getFileName());
        return response;
    }
}
