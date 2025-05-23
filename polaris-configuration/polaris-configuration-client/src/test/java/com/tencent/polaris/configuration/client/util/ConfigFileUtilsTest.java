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

package com.tencent.polaris.configuration.client.util;

import com.tencent.polaris.api.exception.ServerCodes;
import com.tencent.polaris.api.plugin.configuration.ConfigFile;
import com.tencent.polaris.api.plugin.configuration.ConfigFileResponse;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigFileUtils}.
 *
 * @author Haotian Zhang
 */
public class ConfigFileUtilsTest {

    @Test
    public void testCheckConfigContentEmpty_WhenResponseIsNull() {
        // 准备：设置输入为null
        ConfigFileResponse response = null;

        // 执行：调用待测方法
        boolean result = ConfigFileUtils.checkConfigContentEmpty(response);

        // 验证：预期返回true
        assertThat(result).isTrue();
    }

    @Test
    public void testCheckConfigContentEmpty_WhenResponseCodeIsNotFound() {
        // 准备：创建响应对象，设置响应码为NOT_FOUND_RESOURCE
        ConfigFileResponse response = new ConfigFileResponse(ServerCodes.NOT_FOUND_RESOURCE, "not found", null);

        // 执行：调用待测方法
        boolean result = ConfigFileUtils.checkConfigContentEmpty(response);

        // 验证：预期返回true
        assertThat(result).isTrue();
    }

    @Test
    public void testCheckConfigContentEmpty_WhenConfigFileIsNull() {
        // 准备：创建响应对象，不设置configFile
        ConfigFileResponse response = new ConfigFileResponse(ServerCodes.EXECUTE_SUCCESS, "success", null);

        // 执行：调用待测方法
        boolean result = ConfigFileUtils.checkConfigContentEmpty(response);

        // 验证：预期返回true
        assertThat(result).isTrue();
    }

    @Test
    public void testCheckConfigContentEmpty_WhenContentIsEmpty() {
        // 准备：创建完整的响应对象，但内容为空
        ConfigFile configFile = new ConfigFile("namespace", "group", "name");
        configFile.setContent("");

        ConfigFileResponse response = new ConfigFileResponse(ServerCodes.EXECUTE_SUCCESS, "success", configFile);

        // 执行：调用待测方法
        boolean result = ConfigFileUtils.checkConfigContentEmpty(response);

        // 验证：预期返回true
        assertThat(result).isTrue();
    }

    @Test
    public void testCheckConfigContentEmpty_WhenContentIsBlank() {
        // 准备：创建完整的响应对象，但内容为空白字符
        ConfigFile configFile = new ConfigFile("namespace", "group", "name");
        configFile.setContent("   ");

        ConfigFileResponse response = new ConfigFileResponse(ServerCodes.EXECUTE_SUCCESS, "success", configFile);

        // 执行：调用待测方法
        boolean result = ConfigFileUtils.checkConfigContentEmpty(response);

        // 验证：预期返回true
        assertThat(result).isTrue();
    }

    @Test
    public void testCheckConfigContentEmpty_WhenContentIsValid() {
        // 准备：创建完整的响应对象，且有有效内容
        ConfigFile configFile = new ConfigFile("namespace", "group", "name");
        configFile.setContent("valid content");

        ConfigFileResponse response = new ConfigFileResponse(ServerCodes.EXECUTE_SUCCESS, "success", configFile);

        // 执行：调用待测方法
        boolean result = ConfigFileUtils.checkConfigContentEmpty(response);

        // 验证：预期返回false
        assertThat(result).isFalse();
    }

    @Test
    public void testCheckConfigContentEmpty_WhenResponseCodeIsSuccessButContentIsNull() {
        // 准备：创建响应对象，设置成功响应码但内容为null
        ConfigFile configFile = new ConfigFile("namespace", "group", "name");
        configFile.setContent(null);

        ConfigFileResponse response = new ConfigFileResponse(ServerCodes.EXECUTE_SUCCESS, "success", configFile);

        // 执行：调用待测方法
        boolean result = ConfigFileUtils.checkConfigContentEmpty(response);

        // 验证：预期返回true
        assertThat(result).isTrue();
    }
}
