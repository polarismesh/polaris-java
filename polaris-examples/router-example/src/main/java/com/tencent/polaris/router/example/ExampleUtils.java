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

package com.tencent.polaris.router.example;

import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.ConfigAPIFactory;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ExampleUtils {

    public static class InitResult {

        private final String namespace;
        private final String service;
        private final String config;

        public InitResult(String namespace, String service, String config) {
            this.namespace = namespace;
            this.service = service;
            this.config = config;
        }

        public String getNamespace() {
            return namespace;
        }

        public String getService() {
            return service;
        }

        public String getConfig() {
            return config;
        }
    }

    /**
     * 初始化配置对象
     *
     * @param args 命名行参数
     * @return 配置对象
     * @throws ParseException 解析异常
     */
    public static InitResult initConsumerConfiguration(String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        options.addOption("namespace", "service namespace", true, "namespace for service");
        options.addOption("service", "service name", true, "service name");
        options.addOption("config", "config_path", true, "config file path");

        CommandLine commandLine = parser.parse(options, args);
        String namespace = commandLine.getOptionValue("namespace");
        String service = commandLine.getOptionValue("service");
        String config = commandLine.getOptionValue("config");
        if (StringUtils.isBlank(namespace) || StringUtils.isBlank(service)) {
            System.out.println("namespace or service is required");
            System.exit(1);
        }
        return new InitResult(namespace, service, config);
    }

    /**
     * 初始化 SDKContext
     * @param config 配置文件路径
     * @return sdk context
     * @throws IOException
     */
    public static SDKContext initContext(String config) throws IOException {
        if (StringUtils.isNotBlank(config)) {
            try (InputStream inputStream = new FileInputStream(config)) {
                return SDKContext.initContextByConfig(ConfigAPIFactory.loadConfig(inputStream));
            }
        }
        return SDKContext.initContext();
    }

}
