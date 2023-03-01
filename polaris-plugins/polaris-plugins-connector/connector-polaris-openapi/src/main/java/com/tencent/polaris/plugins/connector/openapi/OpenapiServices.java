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

package com.tencent.polaris.plugins.connector.openapi;

import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.configuration.ConfigFile;
import com.tencent.polaris.plugins.connector.openapi.config.Authorization;
import com.tencent.polaris.plugins.connector.openapi.rest.RestService;
import com.tencent.polaris.plugins.connector.openapi.rest.RestUtils;

import java.util.List;

/**
 * @author fabian4 2023-03-01
 */
public class OpenapiServices {

    public static OpenapiServices instance;

    private String token;

    private List<String> address;

    private OpenapiServices(InitContext ctx) {
        this.address = ctx.getConfig().getConfigFile().getServerConnector().getAddresses();
        Authorization authorization = new Authorization(ctx);
        this.token = authorization.generateToken(address);
    }

    public static void initInstance(InitContext ctx) {
        instance = new OpenapiServices(ctx);
    }

    public void getConfigFile(ConfigFile configFile) {
        System.out.println("token: " + token);
        RestService.getConfigFile(RestUtils.toConfigFileUrl(address), "******", configFile);
    }
}
