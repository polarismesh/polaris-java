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
import com.tencent.polaris.plugins.connector.openapi.model.LoginResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * @author fabian4 2023-02-28
 */
public class RestUtils {

    private static final Logger LOG = LoggerFactory.getLogger(RestUtils.class);

    public static String toLogin(String address) {
        return String.format("http://%s/core/v1/user/login", address);
    }

    public static String toCreateConfigFileUrl(String address) {
        return String.format("http://%s/config/v1/configfiles", address);
    }

    public static String toReleaseConfigFileUrl(String address) {
        return String.format("http://%s/config/v1/configfiles/release", address);
    }

    public static String phraseToken(RestResponse<String> restResponse) {
        String jsonStr = restResponse.getResponseEntity().getBody();
        LoginResponse loginResponse = JSONObject.parseObject(Objects.requireNonNull(jsonStr), LoginResponse.class);
        return loginResponse.getToken();
    }

}

