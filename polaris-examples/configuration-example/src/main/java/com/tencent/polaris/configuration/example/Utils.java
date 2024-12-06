/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.polaris.configuration.example;

import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.configuration.api.core.ConfigFilePublishService;
import com.tencent.polaris.configuration.api.core.ConfigFileService;
import com.tencent.polaris.configuration.factory.ConfigFileServiceFactory;
import com.tencent.polaris.configuration.factory.ConfigFileServicePublishFactory;
import com.tencent.polaris.factory.ConfigAPIFactory;
import org.apache.commons.cli.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author lepdou 2022-03-09
 */
public class Utils {

    public static void print(String content) {
        System.out.println("----------------content---------------------");
        System.out.println(content);
        System.out.println("--------------------------------------------");
    }

    public static class InitResult {

        private final String config;

        public InitResult(String config) {
            this.config = config;
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
    public static InitResult initConfiguration(String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        options.addOption("config", "config_path", true, "config file path");

        CommandLine commandLine = parser.parse(options, args);
        String config = commandLine.getOptionValue("config");
        return new InitResult(config);
    }

    /**
     * 创建配置中心服务类
     * @param config 配置文件路径
     * @return 配置中心服务类
     * @throws IOException
     */
    public static ConfigFileService createConfigFileService(String config) throws IOException {
        if (StringUtils.isNotBlank(config)) {
            try (InputStream inputStream = new FileInputStream(config)) {
                return ConfigFileServiceFactory.createConfigFileService(ConfigAPIFactory.loadConfig(inputStream));
            }
        }
        return ConfigFileServiceFactory.createConfigFileService();
    }

    public static ConfigFilePublishService createConfigFilePublishService(String config) throws IOException {
        if (StringUtils.isNotBlank(config)) {
            try (InputStream inputStream = new FileInputStream(config)) {
                return ConfigFileServicePublishFactory.createConfigFilePublishService(ConfigAPIFactory.loadConfig(inputStream));
            }
        }
        return ConfigFileServicePublishFactory.createConfigFilePublishService();
    }

}
