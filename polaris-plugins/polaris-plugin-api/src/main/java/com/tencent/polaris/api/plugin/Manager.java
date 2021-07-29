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

package com.tencent.polaris.api.plugin;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.compose.Extensions;

/**
 * 插件管理器
 *
 * @author andrewshan
 * @date 2019/8/21
 */
public interface Manager extends Supplier {

    /**
     * 初始化插件列表
     *
     * @param context 插件初始化上下文
     * @throws PolarisException 插件初始化过程中抛出异常
     */
    void initPlugins(InitContext context) throws PolarisException;

    /**
     * 在应用上下文初始化完毕后进行调用
     *
     * @param extensions 插件实例
     * @throws PolarisException 执行任务过程抛出异常
     */
    void postContextInitPlugins(Extensions extensions) throws PolarisException;

    /**
     * 销毁已初始化的插件列表
     */
    void destroyPlugins();


}
