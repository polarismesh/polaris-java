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

package com.tencent.polaris.api.config.provider;

import com.tencent.polaris.api.config.plugin.PluginConfig;
import com.tencent.polaris.api.config.verify.Verifier;

import java.util.List;
import java.util.Map;

public interface RateLimitConfig extends PluginConfig, Verifier {

    /**
     * 是否开启限流功能
     *
     * @return boolean
     */
    boolean isEnable();

    /**
     * 最大限流窗口数量
     *
     * @return int
     */
    int getMaxWindowCount();

    /**
     * 限流窗口超标后的降级策略
     *
     * @return fallback
     */
    Fallback getFallbackOnExceedWindowCount();

    /**
     * 获取限流服务端的集群服务信息
     *
     * @return 集群服务
     */
    String getLimiterService();

    /**
     * 获取限流服务端的集群命名空间信息
     *
     * @return 集群命名空间
     */
    String getLimiterNamespace();

    /**
     * 获取限流服务端的集群地址列表
     *
     * @return 集群地址列表
     */
    List<String> getLimiterAddresses();

    /**
     * 获取消息等待最长超时时间
     *
     * @return long, 毫秒
     */
    long getRemoteSyncTimeoutMilli();

    /**
     * 获取匀速排队时最大排队时间
     *
     * @return long，毫秒
     */
    long getMaxQueuingTime();

    long getRemoteTaskIntervalMilli();

    long getRangeDelayMilli();
    /**
     * 是否上报限流监控视图, 该开关默认关闭，如果需要使用限流的老监控视图，则需要开启此监控数据上报开关
     *
     * @return boolean
     */
    boolean isReportMetrics();

    Map<String, String> getMetadata();

    enum Fallback {
        pass, reject,
    }
}
