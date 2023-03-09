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
import com.tencent.polaris.api.exception.ServerCodes;
import com.tencent.polaris.api.exception.ServerErrorResponseException;
import com.tencent.polaris.plugins.configuration.connector.openapi.model.ConfigClientResponse;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author fabian4 2023-02-28
 */
public class RestOperator {

    private static final Logger LOG = LoggerFactory.getLogger(RestOperator.class);

    private static final int DEFAULT_HTTP_TIMEOUT = 5000;

    private static final int DEFAULT_HTTP_READ_TIMEOUT = 10000;

    private static final Gson gson = new Gson();

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient client;

    public RestOperator() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(DEFAULT_HTTP_TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(DEFAULT_HTTP_READ_TIMEOUT, TimeUnit.MILLISECONDS)
                .build();
    }

    public ConfigClientResponse doGet(String url, String token) {
        LOG.debug("[Polaris] server send get request={}", url);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("X-Polaris-Token", token)
                .get()
                .build();

        return doExecute(request);
    }

    public ConfigClientResponse doPost(String url, String token, String body) {
        LOG.debug("[Polaris] server send post request={}, body={}",
                url, body);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("X-Polaris-Token", token)
                .post(RequestBody.create(body, JSON))
                .build();

        return doExecute(request);
    }

    public ConfigClientResponse doPut(String url, String token, String body) {
        LOG.debug("[Polaris] server send put request={}, body={}",
                url, body);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("X-Polaris-Token", token)
                .put(RequestBody.create(body, JSON))
                .build();

        return doExecute(request);
    }

    private ConfigClientResponse doExecute(Request request) {
        ConfigClientResponse configClientResponse;

        try {
            Response response = client.newCall(request).execute();
            configClientResponse = gson.fromJson(Objects.requireNonNull(response.body()).string(), ConfigClientResponse.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int code = Integer.parseInt(Objects.requireNonNull(configClientResponse).getCode());
        if (code != ServerCodes.EXECUTE_SUCCESS) {
            LOG.error("[Polaris] server error to execute request={}, method={}, reason={}",
                    request.url(), request.method(), configClientResponse.getInfo());
            throw ServerErrorResponseException.build(code, configClientResponse.getInfo());
        }
        return configClientResponse;
    }
}

