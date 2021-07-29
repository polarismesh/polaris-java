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

package com.tencent.polaris.api.config.plugin;

import com.tencent.polaris.api.config.verify.Verifier;
import com.tencent.polaris.api.exception.PolarisException;
import java.util.Map;

/**
 * 插件配置对象
 *
 * @author andrewshan
 * @date 2019/8/20
 */
public interface PluginConfig {

    /**
     * 获取插件配置
     *
     * @param pluginName 插件名
     * @param clazz 目标对象的类实例
     * @param <T> 反序列化的目标类型
     * @return 插件配置对象
     * @throws PolarisException 异常
     */
    <T extends Verifier> T getPluginConfig(String pluginName, Class<T> clazz) throws PolarisException;

    /**
     * 获取所有的插件配置
     *
     * @return 所有插件配合
     * @throws PolarisException 解码异常
     */
    Map<String, Verifier> getPluginConfigs() throws PolarisException;
}
