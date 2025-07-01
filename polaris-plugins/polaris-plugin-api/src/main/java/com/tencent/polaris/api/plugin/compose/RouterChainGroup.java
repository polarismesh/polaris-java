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

package com.tencent.polaris.api.plugin.compose;

import com.tencent.polaris.api.plugin.route.ServiceRouter;
import java.util.List;

/**
 * 路由链分组
 */
public interface RouterChainGroup {

    /**
     * 获取前置路由链
     *
     * @return 前置路由链
     */
    List<ServiceRouter> getBeforeRouters();

    /**
     * 获取核心路由链
     *
     * @return 核心路由链
     */
    List<ServiceRouter> getCoreRouters();

    /**
     * 获取后置路由链
     *
     * @return 后置路由链
     */
    List<ServiceRouter> getAfterRouters();
}
