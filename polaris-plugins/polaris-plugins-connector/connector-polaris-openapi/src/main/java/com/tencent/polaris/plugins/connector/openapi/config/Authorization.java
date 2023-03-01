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

package com.tencent.polaris.plugins.connector.openapi.config;

import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.client.api.SDKContext;

/**
 * @author fabian4 2023-03-01
 */
public class Authorization {

    private static Authorization instance;

    private String name;

    private String password;

    private String token;

    private Authorization(InitContext ctx) {
        System.out.println(ctx.getConfig().getConfigFile().getServerConnector().getUsername());
        System.out.println(ctx.getConfig().getConfigFile().getServerConnector().getPassword());

    }

    public static Authorization getInstance(InitContext ctx) {
        if (instance == null) {
            synchronized (Authorization.class) {
                if (instance == null) {
                    instance = new Authorization(ctx);
                }
            }
        }
        return instance;
    }
}
