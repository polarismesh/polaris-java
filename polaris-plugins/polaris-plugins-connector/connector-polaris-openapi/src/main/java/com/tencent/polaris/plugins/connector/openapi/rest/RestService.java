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
import com.tencent.polaris.api.plugin.configuration.ConfigFile;
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

    public static RestResponse<String> getConfigFile(String url, String token, ConfigFile configFile) {
        JSONObject params = new JSONObject();
        params.put("name", configFile.getFileName());
        params.put("group", configFile.getFileGroup());
        params.put("namespace", configFile.getNamespace());
        RestResponse<String> restResponse = sendPost(HttpMethod.GET, url, token, params);
        System.out.println(restResponse);
        return null;
    }

    public static RestResponse<String> sendPost(HttpMethod method, String url, String token, JSONObject params) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Polaris-Token", token);
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        if (method == HttpMethod.GET && Objects.nonNull(params)) {
            url = RestUtils.encodeUrl(url, params);
        } else if (method == HttpMethod.POST) {
            entity = new HttpEntity<>(params.toString(), headers);
        }

        LOG.warn("[Polaris] server error to send request {}, body {}, method {}, token {}",
                url, params.toString(), method, token);


        RestResponse<String> restResponse = restOperator
                .curlRemoteEndpoint(url, method, entity, String.class);

        if (restResponse.hasServerError()) {
            LOG.error("[Polaris] server error to send request {}, body {}, method {}, reason {}",
                    url, params.toString(), method, restResponse.getException().getMessage());
            throw new PolarisException(ErrorCode.SERVER_EXCEPTION, restResponse.getException().getMessage());
        }
        return restResponse;
    }
}
