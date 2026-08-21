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

package com.tencent.polaris.api.plugin.server;

/**
 * 客户端事件处理器，处理服务端经 WatchClientEvents 流推送的事件。协议无关，由连接器适配具体传输。
 * <p>
 * 实现方（如配置生效查询）解析服务端推送的事件内容并返回应答内容；连接器负责将应答回传给服务端。
 *
 * @author evelynwei
 */
public interface ClientEventHandler {

    /**
     * 处理一条服务端推送事件，返回应答内容。
     * <p>
     * 任何分支都必须返回可发送的内容（即便处理失败也返回降级应答），
     * 服务端在同步等待，静默会把它挂到超时。
     *
     * @param index 事件序号，回传应答时原样携带
     * @param content 事件内容
     * @return 应答内容，不可为 null
     */
    String onPush(long index, String content);
}
