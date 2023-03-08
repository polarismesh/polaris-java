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

package com.tencent.polaris.plugins.configuration.connector.openapi.rest;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.tencent.polaris.api.plugin.configuration.ConfigFile;
import com.tencent.polaris.plugins.configuration.connector.openapi.model.ConfigClientResponse;
import okhttp3.HttpUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * @author fabian4 2023-02-28
 */
public class RestService {

    private static final Logger LOG = LoggerFactory.getLogger(RestService.class);c

    private static final Gson gson = new Gson();

    private static final RestOperator restOperator = new RestOperator();

    public static ConfigClientResponse getConfigFile(String url, String token, ConfigFile configFile) {
        url = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder()
                .addQueryParameter("name", configFile.getFileName())
                .addQueryParameter("group", configFile.getFileGroup())
                .addQueryParameter("namespace", configFile.getNamespace())
                .build().toString();

        return restOperator.doGet(url, token);
    }

    public static ConfigClientResponse createConfigFile(String url, String token, ConfigFile configFile) {
        JsonObject params = RestUtils.getParams(configFile);
        params.addProperty("content", configFile.getContent());
        ConfigClientResponse response = restOperator.doPost(url, token, params.toString());
        LOG.info("[Polaris] creat configuration file success: Namespace={}, FileGroup={}, FileName={}, Content={}",
                configFile.getNamespace(), configFile.getFileGroup(), configFile.getFileName(), configFile.getContent());
        return response;
    }

    public static ConfigClientResponse updateConfigFile(String url, String token, ConfigFile configFile) {
        JsonObject params = RestUtils.getParams(configFile);
        params.addProperty("content", configFile.getContent());
        ConfigClientResponse response = restOperator.doPut(url, token, params.toString());
        LOG.info("[Polaris] update configuration file success: Namespace={}, FileGroup={}, FileName={}, Content={}",
                configFile.getNamespace(), configFile.getFileGroup(), configFile.getFileName(), configFile.getContent());
        return response;
    }

    public static ConfigClientResponse releaseConfigFile(String url, String token, ConfigFile configFile) {
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
