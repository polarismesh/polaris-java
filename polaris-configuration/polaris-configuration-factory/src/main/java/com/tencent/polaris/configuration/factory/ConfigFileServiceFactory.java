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
import com.tencent.polaris.configuration.api.core.ConfigFileService;
import com.tencent.polaris.configuration.client.DefaultConfigFileService;
import com.tencent.polaris.factory.ConfigAPIFactory;

/**
 * @author lepdou 2022-03-01
 */
public class ConfigFileServiceFactory {

    public static ConfigFileService createConfigFileService() throws PolarisException {
        Configuration configuration = ConfigAPIFactory.defaultConfig();
        return createConfigFileService(configuration);
    }

    public static ConfigFileService createConfigFileService(Configuration config) throws PolarisException {
        SDKContext context = SDKContext.initContextByConfig(config);
        return createConfigFileService(context);
    }

    public static ConfigFileService createConfigFileService(SDKContext sdkContext) throws PolarisException {
        DefaultConfigFileService configFileService = new DefaultConfigFileService(sdkContext);
        configFileService.init();
        return configFileService;
    }

}
