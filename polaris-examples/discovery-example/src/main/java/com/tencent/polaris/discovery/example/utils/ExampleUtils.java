/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
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

package com.tencent.polaris.discovery.example.utils;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.core.ProviderAPI;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.ConfigAPIFactory;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class ExampleUtils {

    public static class InitResult {

        private final String namespace;
        private final String service;
        private final String host;
        private final int port;
        private final int ttl;
        private final String token;
        private final String config;

        public InitResult(String namespace, String service, String config) {
            this(namespace, service, "", 0, "", 0, config);
        }

        public InitResult(
                String namespace, String service, String host, int port, String token, int ttl, String config) {
            this.namespace = namespace;
            this.service = service;
            this.host = host;
            this.port = port;
            this.token = token;
            this.ttl = ttl;
            this.config = config;
        }

        public String getNamespace() {
            return namespace;
        }

        public String getService() {
            return service;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public String getToken() {
            return token;
        }

        public int getTtl() {
            return ttl;
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
    public static InitResult initConsumerConfiguration(String[] args, boolean ignoreServiceCheck) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        options.addOption("namespace", "service_namespace", true, "namespace for service");
        options.addOption("service", "service_name", true, "service name");
        options.addOption("config", "config_path", true, "config file path");

        CommandLine commandLine = parser.parse(options, args);
        String namespace = commandLine.getOptionValue("namespace");
        String service = commandLine.getOptionValue("service");
        String config = commandLine.getOptionValue("config");
        if (StringUtils.isBlank(namespace)) {
            if (!ignoreServiceCheck && StringUtils.isBlank(service)) {
                System.out.println("namespace or service is required");
                System.exit(1);
            }
        }
        return new InitResult(namespace, service, config);
    }

    /**
     * 初始化被调方配置对象
     *
     * @param args 命名行参数
     * @return 配置对象
     * @throws ParseException 解析异常
     */
    public static InitResult initProviderConfiguration(String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        options.addOption("namespace", "service_namespace", true, "namespace for service");
        options.addOption("service", "service_name", true, "service name");
        options.addOption("host", "host", true, "host");
        options.addOption("port", "port", true, "port");
        options.addOption("token", "token", true, "token");
        options.addOption("ttl", "ttl", true, "ttl");
        options.addOption("config", "config_path", true, "config file path");

        CommandLine commandLine = parser.parse(options, args);
        String namespace = commandLine.getOptionValue("namespace");
        String service = commandLine.getOptionValue("service");
        String host = commandLine.getOptionValue("host");
        String token = commandLine.getOptionValue("token");
        String portStr = commandLine.getOptionValue("port");
        String config = commandLine.getOptionValue("config");
        if (StringUtils.isBlank(namespace) || StringUtils.isBlank(service) || StringUtils.isBlank(host) || StringUtils
                .isBlank(portStr)) {
            System.out.println("namespace / service / host/ token / port is required");
            System.exit(1);
        }
        int port = Integer.parseInt(portStr);
        int ttl = 0;
        String ttlStr = commandLine.getOptionValue("ttl");
        if (StringUtils.isNotBlank(ttlStr)) {
            ttl = Integer.parseInt(ttlStr);
        }
        return new InitResult(namespace, service, host, port, token, ttl, config);
    }

    public static SDKContext createSDKContext(String config) throws IOException {
        if (StringUtils.isNotBlank(config)) {
            try (InputStream inputStream = new FileInputStream(config)) {
                Configuration configuration = ConfigAPIFactory.loadConfig(inputStream);
                return SDKContext.initContextByConfig(configuration);
            }
        }
        return SDKContext.initContext();
    }


    /**
     * 创建ConsumerAPI
     * @param config 配置文件路径
     * @return api object
     * @throws IOException
     */
    public static ConsumerAPI createConsumerAPI(String config) throws IOException {
        if (StringUtils.isNotBlank(config)) {
            try (InputStream inputStream = new FileInputStream(config)) {
                return DiscoveryAPIFactory.createConsumerAPIByFile(inputStream);
            }
        }
        return DiscoveryAPIFactory.createConsumerAPI();
    }

    /**
     * 创建ProviderAPI
     * @param config 配置文件路径
     * @return api object
     * @throws IOException
     */
    public static ProviderAPI createProviderAPI(String config) throws IOException {
        if (StringUtils.isNotBlank(config)) {
            try (InputStream inputStream = new FileInputStream(config)) {
                return DiscoveryAPIFactory.createProviderAPIByFile(inputStream);
            }
        }
        return DiscoveryAPIFactory.createProviderAPI();
    }
}
