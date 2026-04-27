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

package com.tencent.polaris.plugins.connector.nacos;

import com.tencent.polaris.api.plugin.server.CommonProviderRequest;

import java.lang.reflect.Method;

/**
 * 测试辅助类，通过反射暴露 NacosConnector 的 package-private/private 方法。
 */
public class TestableNacosConnector extends NacosConnector {

    public TestableNacosConnector(NacosContext nacosContext) {
        // 将 nacosContext 注入父类（通过反射，因为字段为 private）
        try {
            java.lang.reflect.Field field = NacosConnector.class.getDeclaredField("nacosContext");
            field.setAccessible(true);
            field.set(this, nacosContext);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject nacosContext", e);
        }
    }

    /**
     * 暴露父类私有 getServiceName() 方法供测试使用。
     */
    public String exposeGetServiceName(CommonProviderRequest req) {
        try {
            Method method = NacosConnector.class.getDeclaredMethod("getServiceName", CommonProviderRequest.class);
            method.setAccessible(true);
            return (String) method.invoke(this, req);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke getServiceName", e);
        }
    }
}
