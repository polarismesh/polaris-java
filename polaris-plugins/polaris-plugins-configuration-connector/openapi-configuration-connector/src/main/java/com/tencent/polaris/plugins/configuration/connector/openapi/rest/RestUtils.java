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

import com.google.gson.JsonObject;
import com.tencent.polaris.api.plugin.configuration.ConfigFile;
import com.tencent.polaris.factory.config.configuration.ConnectorConfigImpl;
import com.tencent.polaris.plugins.configuration.connector.openapi.model.ConfigClientFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * @author fabian4 2023-02-28
 */
public class RestUtils {

    private static final Logger LOG = LoggerFactory.getLogger(RestUtils.class);

    public static String toConfigFileUrl(List<String> addresses) {
        String address = pickAddress(addresses);
        return String.format("http://%s/config/v1/configfiles", address);
    }

    public static String toReleaseConfigFileUrl(List<String> addresses) {
        String address = pickAddress(addresses);
        return String.format("http://%s/config/v1/configfiles/release", address);
    }

    public static List<String> getAddress(ConnectorConfigImpl config) {
        List<String> addresses = config.getOpenapiAddresses();
        if (addresses == null || addresses.isEmpty()) {
            addresses = config.getAddresses();
            addresses = addresses.stream().map(address -> address.replaceAll(":.*", ":8090")).collect(Collectors.toList());
        }
        return addresses;
    }

    public static String pickAddress(List<String> addresses) {
        if (addresses.size() == 1) {
            return addresses.get(0);
        }
        int i = ThreadLocalRandom.current().nextInt(addresses.size());
        return addresses.get(i);
    }

    public static JsonObject getParams(ConfigFile configFile) {
        JsonObject params = new JsonObject();
        params.addProperty("name", configFile.getFileName());
        params.addProperty("group", configFile.getFileGroup());
        params.addProperty("namespace", configFile.getNamespace());
        params.addProperty("format", RestUtils.getFormat(configFile.getFileName()));
        return params;
    }

    public static String getFormat(String filename) {
        String[] split = filename.split("\\.");
        if (split.length <= 1) {
            return "test";
        }
        return split[split.length - 1];
    }

    public static ConfigFile transferFromDTO(ConfigClientFile configClientFile) {
        if (configClientFile == null) {
            return null;
        }

        ConfigFile configFile = new ConfigFile(configClientFile.getNamespace(),
                configClientFile.getGroup(),
                configClientFile.getName());
        configFile.setContent(configClientFile.getContent());

        return configFile;
    }

}

