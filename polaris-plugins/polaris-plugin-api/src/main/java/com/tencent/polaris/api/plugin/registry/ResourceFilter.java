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
 * 实例过滤器，用于获取的时候进行过滤操作
 *
 * @author andrewshan
 * @date 2019/9/3
 */
public class ResourceFilter {

    private final ServiceEventKey svcEventKey;

    //是否内部触发的请求
    private final boolean internalRequest;

    //使用缓存
    private final boolean includeCache;

    private final boolean fallback;

    public ResourceFilter(ServiceEventKey svcEventKey, boolean internalRequest, boolean includeCache) {
        this.svcEventKey = svcEventKey;
        this.internalRequest = internalRequest;
        this.includeCache = includeCache;
        this.fallback = false;
    }

    public ResourceFilter(ServiceEventKey svcEventKey, boolean internalRequest, boolean includeCache, boolean fallback) {
        this.svcEventKey = svcEventKey;
        this.internalRequest = internalRequest;
        this.includeCache = includeCache;
        this.fallback = fallback;
    }

    public ServiceEventKey getSvcEventKey() {
        return svcEventKey;
    }

    public boolean isInternalRequest() {
        return internalRequest;
    }

    public boolean isIncludeCache() {
        return includeCache;
    }

    public boolean isFallback() {
        return fallback;
    }
}