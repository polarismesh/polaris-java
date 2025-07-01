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

package com.tencent.polaris.api.config.global;

import com.tencent.polaris.api.config.verify.Verifier;
import java.util.List;

/**
 * 集群配置
 */
public interface ClusterConfig extends Verifier {

    /**
     * 获取命名空间
     *
     * @return namespace
     */
    String getNamespace();

    /**
     * 获取服务名
     *
     * @return service
     */
    String getService();

    /**
     * 获取服务刷新间隔
     *
     * @return refreshInterval
     */
    long getRefreshInterval();

    /**
     * 获取系统服务路由链
     *
     * @return routers
     */
    List<String> getRouters();

    /**
     * 获取系统服务负载均衡器
     *
     * @return loadBalancer
     */
    String getLbPolicy();

    /**
     * 是否与埋点地址一致，如果一致则无需填写服务地址信息
     *
     * @return boolean
     */
    boolean isSameAsBuiltin();
}