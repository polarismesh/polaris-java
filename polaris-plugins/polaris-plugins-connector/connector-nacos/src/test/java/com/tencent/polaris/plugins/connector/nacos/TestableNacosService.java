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

import com.tencent.polaris.plugins.connector.common.ServiceUpdateTask;

import java.lang.reflect.Method;

/**
 * 测试辅助类，通过反射暴露 NacosService 的 private 方法。
 */
public class TestableNacosService extends NacosService {

    public TestableNacosService(NacosContext nacosContext) {
        super(null, nacosContext);
    }

    /**
     * 暴露私有 resolveSubscribeName() 方法供测试使用。
     */
    public String exposeResolveSubscribeName(ServiceUpdateTask task) {
        try {
            Method method = NacosService.class.getDeclaredMethod("resolveSubscribeName", ServiceUpdateTask.class);
            method.setAccessible(true);
            return (String) method.invoke(this, task);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke resolveSubscribeName", e);
        }
    }
}
