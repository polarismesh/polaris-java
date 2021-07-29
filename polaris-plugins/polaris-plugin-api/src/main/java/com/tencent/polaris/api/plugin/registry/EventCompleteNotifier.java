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

package com.tencent.polaris.api.plugin.registry;

import com.tencent.polaris.api.pojo.ServiceEventKey;

/**
 * 事件通知回调
 */
public interface EventCompleteNotifier {

    /**
     * 任务正常完成时的回调函数
     *
     * @param svcEventKey 服务信息
     */
    void complete(ServiceEventKey svcEventKey);

    /**
     * 任务异常完成时候的回调函数
     *
     * @param svcEventKey 服务信息
     * @param throwable 异常数据
     */
    void completeExceptionally(ServiceEventKey svcEventKey, Throwable throwable);
}
