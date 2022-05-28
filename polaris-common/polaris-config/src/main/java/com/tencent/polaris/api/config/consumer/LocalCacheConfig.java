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

package com.tencent.polaris.api.config.consumer;

import com.tencent.polaris.api.config.plugin.PluginConfig;
import com.tencent.polaris.api.config.verify.Verifier;

/**
 * 本地缓存相关配置项
 *
 * @author andrewshan
 * @date 2019/8/20
 */
public interface LocalCacheConfig extends PluginConfig, Verifier {

    /**
     * services.consumer.localCache.service.expireTime
     * 服务的超时淘汰时间
     *
     * @return long, 毫秒
     */
    long getServiceExpireTime();

    /**
     * services.consumer.localCache.service.refreshInterval
     * 服务的定期刷新时间
     *
     * @return long, 毫秒
     */
    long getServiceRefreshInterval();

    /**
     * services.consumer.localCache.serviceList.refreshInterval
     * 拉取服务元信息列表的定期刷新时间
     *
     * @return long, 毫秒
     */
    long getServiceListRefreshInterval();

    /**
     * services.consumer.localCache.type
     * 本地缓存类型，可修改成具体的缓存插件名
     *
     * @return String
     */
    String getType();

    /**
     * services.consumer.localCache.persistEnable
     * 是否启用本地持久化缓存机制
     *
     * @return boolean
     */
    boolean isPersistEnable();

    /**
     * services.consumer.localCache.persistDir
     * 本地缓存持久化目录
     *
     * @return String
     */
    String getPersistDir();

    /**
     * services.consumer.localCache.persistMaxWriteRetry
     * 本地缓存持久化最大写重试次数, 默认5次
     *
     * @return int
     */
    int getPersistMaxWriteRetry();

    /**
     * services.consumer.localCache.persistMaxReadRetry
     * 本地缓存持久化最大读重试次数, 默认1次
     *
     * @return int
     */
    int getPersistMaxReadRetry();

    /**
     * services.consumer.localCache.persistRetryInterval
     * 本地缓存更新重试间隔
     *
     * @return long
     */
    long getPersistRetryInterval();

    /**
     * services.consumer.localCache.serviceExpireEnable
     * 是否启用服务缓存淘汰
     *
     * @return boolean
     */
    boolean isServiceExpireEnable();

    /**
     * services.comsumer.localCache.servicePushEmptyProtectEnable 是否启用服务推空保护
     * @return boolean
     */
    boolean isServicePushEmptyProtectEnable();
}
