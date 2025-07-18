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

package com.tencent.polaris.plugins.stat.common.model;

import com.tencent.polaris.api.plugin.stat.StatInfo;

/**
 * 对StatInfo进行处理类
 */
public interface StatInfoHandler {

    /**
     * 处理StatInfo类型值
     *
     * @param statInfo 待处理值
     */
    void handle(StatInfo statInfo);

    /**
     * 停止处理StatInfo，通常用于释放资源
     */
    void stopHandle();
}
