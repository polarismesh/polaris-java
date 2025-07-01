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
 * 基本路由链
 */
public class DefaultRouterChainGroup implements RouterChainGroup {

    private final List<ServiceRouter> beforeRouters;

    private final List<ServiceRouter> coreRouters;

    private final List<ServiceRouter> afterRouters;

    public DefaultRouterChainGroup(List<ServiceRouter> beforeRouters,
            List<ServiceRouter> coreRouters,
            List<ServiceRouter> afterRouters) {
        this.beforeRouters = beforeRouters;
        this.coreRouters = coreRouters;
        this.afterRouters = afterRouters;
    }

    @Override
    public List<ServiceRouter> getBeforeRouters() {
        return beforeRouters;
    }

    @Override
    public List<ServiceRouter> getCoreRouters() {
        return coreRouters;
    }

    @Override
    public List<ServiceRouter> getAfterRouters() {
        return afterRouters;
    }
}
