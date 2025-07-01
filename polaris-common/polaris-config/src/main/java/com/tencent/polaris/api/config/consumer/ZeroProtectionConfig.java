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

package com.tencent.polaris.api.config.consumer;

import com.tencent.polaris.api.config.verify.Verifier;

/**
 * 零实例保护的配置项
 *
 * @author Haotian Zhang
 */
public interface ZeroProtectionConfig extends Verifier {

    /**
     * 是否启用零实例保护
     *
     * @return boolean
     */
    boolean isEnable();

    /**
     * 是否检测网络连通性
     *
     * @return boolean
     */
    boolean isNeedTestConnectivity();

    /**
     * 获取探测结果的过期时间
     *
     * @return timeout
     */
    int getTestConnectivityExpiration();

    /**
     * 获取探测请求的超时时间
     *
     * @return timeout
     */
    int getTestConnectivityTimeout();

    /**
     * 获取探测请求的并行数
     *
     * @return parallel
     */
    int getTestConnectivityParallel();
}
