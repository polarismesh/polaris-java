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

package com.tencent.polaris.plugins.configuration.connector.openapi;

import com.tencent.polaris.api.config.verify.DefaultValues;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.configuration.ConfigFile;
import com.tencent.polaris.api.plugin.configuration.ConfigFileConnector;
import com.tencent.polaris.api.plugin.configuration.ConfigFileResponse;
import com.tencent.polaris.plugins.connector.openapi.OpenapiServices;

import java.util.List;

/**
 * @author fabian4 2023-02-28
 */
public class OpenapiConfigFileConnector implements ConfigFileConnector {

    @Override
    public String getName() {
        return DefaultValues.OPENAPI_CONNECTOR_TYPE;
    }

    @Override
    public PluginType getType() {
        return PluginTypes.CONFIG_FILE_CONNECTOR.getBaseType();
    }

    @Override
    public void init(InitContext ctx) throws PolarisException {
        OpenapiServices.initInstance(ctx);
    }

    @Override
    public void postContextInit(Extensions ctx) throws PolarisException {

    }

    @Override
    public void destroy() {

    }

    @Override
    public ConfigFileResponse getConfigFile(ConfigFile configFile) {
        return OpenapiServices.instance.getConfigFile(configFile);
    }

    @Override
    public ConfigFileResponse watchConfigFiles(List<ConfigFile> configFiles) {
        throw new PolarisException(ErrorCode.NOT_SUPPORT, "OpenapiConfigFileConnector does not support config file watch through HTTP.");
    }

    @Override
    public void createConfigFileAndRelease(ConfigFile configFile) {
        OpenapiServices.instance.createConfigFileAndRelease(configFile);
    }

    @Override
    public void updateConfigFileAndRelease(ConfigFile configFile) {
        OpenapiServices.instance.updateConfigFileAndRelease(configFile);
    }
}
