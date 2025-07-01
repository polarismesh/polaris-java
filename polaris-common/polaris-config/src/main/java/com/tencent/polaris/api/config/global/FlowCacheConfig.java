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

/**
 * 流程缓存配置对象
 */
public interface FlowCacheConfig extends Verifier {

    String DEFAULT_FLOW_CACHE_NAME = "simpleCache";

    /**
     * 是否启用流程缓存
     *
     * @return boolean
     */
    boolean isEnable();

    /**
     * 获取缓存对象名
     *
     * @return 对象名
     */
    String getName();

    /**
     * 获取缓存对象过期淘汰时间
     *
     * @return long
     */
    long getExpireInterval();

}
