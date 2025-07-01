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

package com.tencent.polaris.api.plugin;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.compose.Extensions;

/**
 * 插件的通用接口，包含插件的标识
 *
 * @author andrewshan
 * @date 2019/8/21
 */
public interface Plugin {

    /**
     * 获取插件名
     *
     * @return String
     */
    String getName();

    /**
     * 获取插件类型
     *
     * @return type
     */
    PluginType getType();

    /**
     * 初始化插件，在AppContext初始化之前调用
     *
     * @param ctx 初始化上下文
     */
    void init(InitContext ctx) throws PolarisException;

    /**
     * 在整个AppContext初始化完毕后调用
     *
     * @param ctx 插件实例信息
     * @throws PolarisException 执行任务过程抛出异常
     */
    void postContextInit(Extensions ctx) throws PolarisException;

    /**
     * 销毁插件
     */
    void destroy();
}
