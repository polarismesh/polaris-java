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

package com.tencent.polaris.api.core;

import com.tencent.polaris.api.plugin.lossless.LosslessActionProvider;
import com.tencent.polaris.api.plugin.lossless.RegisterStatusProvider;

public interface LosslessAPI {

    /**
     * 设置无损上下线相关的动作提供器, 不设置则使用默认的动态提供器（基于北极星SDK注册和反注册）
     * @param losslessActionProvider 无损上下线动作提供器
     */
    void setLosslessActionProvider(LosslessActionProvider losslessActionProvider);

    /**
     * 实施无损上下线
     */
    void losslessRegister();

    /**
     * 设置实例上线状态的提供器，如不设置则使用默认的提供器，基本北极星SDK的注册和反注册来实现
     * @param registerStatusProvider 上线状态获取的提供器
     */
    void setRegisterStatusProvider(RegisterStatusProvider registerStatusProvider);
}
