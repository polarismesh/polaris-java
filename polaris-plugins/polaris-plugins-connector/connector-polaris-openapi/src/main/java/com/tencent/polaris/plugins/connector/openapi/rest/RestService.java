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

package com.tencent.polaris.plugins.connector.openapi.rest;

import com.alibaba.fastjson.JSONObject;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.exception.ServerCodes;
import com.tencent.polaris.api.exception.ServerErrorResponseException;
import com.tencent.polaris.api.plugin.configuration.ConfigFile;
import com.tencent.polaris.plugins.connector.openapi.model.ConfigClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.Objects;

/**
 * @author fabian4 2023-02-28
 */
public class RestService {

    private static final Logger LOG = LoggerFactory.getLogger(RestService.class);

    private static final RestOperator restOperator = new RestOperator();

    public static ConfigClientResponse getConfigFile(String url, String token, ConfigFile configFile) {
        return sendPost(HttpMethod.GET, url, token, RestUtils.getParams(configFile));
    }

    public static ConfigClientResponse createConfigFile(String url, String token, ConfigFile configFile) {
        JSONObject params = RestUtils.getParams(configFile);
        params.put("content", configFile.getContent());
        ConfigClientResponse response = sendPost(HttpMethod.POST, url, token, params);
        LOG.info("[Polaris] creat configuration file success: Namespace {}, FileGroup {}, FileName {}, Content {}",
                configFile.getNamespace(), configFile.getFileGroup(), configFile.getFileName(), configFile.getContent());
        return response;
    }

    public static ConfigClientResponse updateConfigFile(String url, String token, ConfigFile configFile) {
        JSONObject params = RestUtils.getParams(configFile);
        params.put("content", configFile.getContent());
        ConfigClientResponse response = sendPost(HttpMethod.PUT, url, token, params);
        LOG.info("[Polaris] update configuration file success: Namespace {}, FileGroup {}, FileName {}, Content {}",
                configFile.getNamespace(), configFile.getFileGroup(), configFile.getFileName(), configFile.getContent());
        return response;
    }

    public static ConfigClientResponse releaseConfigFile(String url, String token, ConfigFile configFile) {
        JSONObject params = new JSONObject();
        params.put("fileName", configFile.getFileName());
        params.put("group", configFile.getFileGroup());
        params.put("namespace", configFile.getNamespace());
        ConfigClientResponse response = sendPost(HttpMethod.POST, url, token, params);
        LOG.info("[Polaris] release configuration file success: Namespace {}, FileGroup {}, FileName {}, Content {}",
                configFile.getNamespace(), configFile.getFileGroup(), configFile.getFileName(), configFile.getContent());
        return response;
    }

    public static ConfigClientResponse sendPost(HttpMethod method, String url, String token, JSONObject params) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Polaris-Token", token);
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        if (method == HttpMethod.GET && Objects.nonNull(params)) {
            url = RestUtils.encodeUrl(url, params);
        } else if (method == HttpMethod.POST || method == HttpMethod.PUT) {
            entity = new HttpEntity<>(params.toString(), headers);
        }
        RestResponse<String> restResponse = restOperator
                .curlRemoteEndpoint(url, method, entity, String.class);

        if (restResponse.hasServerError()) {
            LOG.error("[Polaris] server error to send request {}, body {}, method {}, reason {}",
                    url, params, method, restResponse.getException().getMessage());
            throw new PolarisException(ErrorCode.SERVER_EXCEPTION, restResponse.getException().getMessage());
        }

        ConfigClientResponse configClientResponse = JSONObject.parseObject(restResponse.getResponseEntity().getBody(), ConfigClientResponse.class);
        int code = Integer.parseInt(Objects.requireNonNull(configClientResponse).getCode());
        if (code != ServerCodes.EXECUTE_SUCCESS) {
            LOG.error("[Polaris] server error to execute request {}, params {}, method {}, reason {}",
                    url, params, method, configClientResponse.getInfo());
            throw ServerErrorResponseException.build(code, configClientResponse.getInfo());
        }

        return configClientResponse;
    }
}
