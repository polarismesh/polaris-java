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

package com.tencent.polaris.configuration.factory;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.configuration.api.core.ConfigFilePublishService;
import com.tencent.polaris.configuration.client.DefaultConfigFilePublishService;
import com.tencent.polaris.factory.ConfigAPIFactory;

/**
 * @author fabian4 2022-03-08
 */
public class ConfigFileServicePublishFactory {

    public static ConfigFilePublishService createConfigFilePublishService() throws PolarisException {
        Configuration configuration = ConfigAPIFactory.defaultConfig();
        return createConfigFilePublishService(configuration);
    }

    public static ConfigFilePublishService createConfigFilePublishService(Configuration config) throws PolarisException {
        SDKContext context = SDKContext.initContextByConfig(config);
        return createConfigFilePublishService(context);
    }

    public static ConfigFilePublishService createConfigFilePublishService(SDKContext sdkContext) throws PolarisException {
        DefaultConfigFilePublishService configFilePublishService = new DefaultConfigFilePublishService(sdkContext);
        configFilePublishService.init();
        return configFilePublishService;
    }
}
