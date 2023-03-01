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

import com.tencent.polaris.api.plugin.configuration.ConfigFileResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

/**
 * @author fabian4 2023-02-28
 */
public class RestService {

    private static final Logger LOG = LoggerFactory.getLogger(RestService.class);

    private static final RestOperator restOperator = new RestOperator();

    public static RestOperator getRestOperator() {
        return restOperator;
    }

    public static void sendPost(HttpMethod method,
                                              String url, String token, String body) {

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Polaris-Token", token);
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        RestResponse<String> restResponse = restOperator
                .curlRemoteEndpoint(url, method, entity, String.class);

        System.out.println(restResponse.getResponseEntity().toString());
//        if (restResponse.hasServerError()) {
//            LOG.error("[Polaris] server error to send post {}, method {}, reason {}",
//                    url, method, restResponse.getException().getMessage());
//            return ResponseUtils.toConfigFilesResponse(null, StatusCodes.CONNECT_EXCEPTION);
//        }
//        if (restResponse.hasTextError()) {
//            if (StringUtils.contains(restResponse.getStatusText(), "existed resource")) {
//                LOG.debug("[Polaris] text error to send post {}, method {}, code {}, reason {}",
//                        url, method, restResponse.getRawStatusCode(),
//                        restResponse.getStatusText());
//            } else {
//                LOG.warn("[Polaris] text error to send post {}, method {}, code {}, reason {}",
//                        url, method, restResponse.getRawStatusCode(),
//                        restResponse.getStatusText());
//            }
//            return ResponseUtils.toConfigFilesResponse(null, ResponseUtils.normalizeStatusCode(
//                    restResponse.getRawStatusCode()));
//        }
//        LOG.info("[Polaris] success to send post {}, method {}", url, method);
//        return ResponseUtils.toConfigFilesResponse(null, StatusCodes.SUCCESS);
    }

    private ConfigFileResponse handleResponse(RestResponse<String> response) {
//        int code = response.getCode().getValue();
//        int code = response.getRawStatusCode();
//        //预期code，正常响应
//        if (code == ServerCodes.EXECUTE_SUCCESS ||
//                code == ServerCodes.NOT_FOUND_RESOURCE ||
//                code == ServerCodes.DATA_NO_CHANGE) {
//            ConfigFile loadedConfigFile = transferFromDTO(response.getConfigFile());
//            return new ConfigFileResponse(code, response.getInfo().getValue(), loadedConfigFile);
//        }
//        //服务端异常
//        throw ServerErrorResponseException.build(code, response.getInfo().getValue());
//    }
        return null;
    }
}
