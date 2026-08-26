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

package com.tencent.polaris.configuration.api.core;

/**
 * 配置生效值提供者的注册句柄，实现 {@link AutoCloseable}，close() 时注销提供者。
 * <p>
 * 上层框架（如 Spring Cloud Tencent）将其注册为 Bean 并声明 destroyMethod = "close"，
 * 容器关闭时自动注销，避免 SDKContext 持有已销毁容器的引用。
 *
 * @author evelynwei
 */
public interface ConfigEffectiveValueRegistration extends AutoCloseable {

    /**
     * 注销对应的 {@link ConfigEffectiveValueProvider}。
     */
    @Override
    void close();
}
