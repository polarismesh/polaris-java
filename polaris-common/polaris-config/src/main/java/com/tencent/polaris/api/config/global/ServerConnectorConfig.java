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

package com.tencent.polaris.api.config.global;

import com.tencent.polaris.api.config.plugin.PluginConfig;
import com.tencent.polaris.api.config.verify.Verifier;
import java.util.List;
import java.util.Map;

/**
 * 与名字服务服务端的连接配置
 *
 * @author andrewshan
 */
public interface ServerConnectorConfig extends PluginConfig, Verifier {

    /**
     * 远端server地址
     *
     * @return 地址列表
     */
    List<String> getAddresses();

    /**
     * 与server对接的协议，默认GRPC
     *
     * @return 协议名称
     */
    String getProtocol();

    /**
     * 与server的连接超时时间
     *
     * @return long, 毫秒
     */
    long getConnectTimeout();

    /**
     * server的切换时延
     *
     * @return long, 毫秒
     */
    long getServerSwitchInterval();

    /**
     * 获取消息等待最长超时时间
     *
     * @return long, 毫秒
     */
    long getMessageTimeout();

    /**
     * 空闲连接过期时间
     *
     * @return long, 毫秒
     */
    long getConnectionIdleTimeout();

    /**
     * 获取重连间隔
     *
     * @return long, 毫秒
     */
    long getReconnectInterval();

    /**
     * Get metadata map.
     *
     * @return metadata
     */
    Map<String, String> getMetadata();
}
