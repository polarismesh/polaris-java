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

package com.tencent.polaris.plugins.router.common;

import com.tencent.polaris.api.control.Destroyable;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.IdAwarePlugin;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.route.RouteInfo;
import com.tencent.polaris.api.plugin.route.RouteResult;
import com.tencent.polaris.api.plugin.route.ServiceRouter;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceMetadata;
import com.tencent.polaris.api.utils.CollectionUtils;

public abstract class AbstractServiceRouter extends Destroyable implements ServiceRouter, IdAwarePlugin {

    private int id;

    private void validateParams(RouteInfo routeInfo, ServiceInstances instances)
            throws PolarisException {

        // 无实例, 返回参数错误
        if (instances == null || CollectionUtils.isEmpty(instances.getInstances())) {
            throw new PolarisException(ErrorCode.API_INVALID_ARGUMENT,
                    "GetFilteredInstances param invalid, empty instances");
        }

        // 规则实例为nil, 返回参数错误
        if (routeInfo == null) {
            throw new PolarisException(ErrorCode.API_INVALID_ARGUMENT,
                    "GetFilteredInstances param invalid, routeInfo can't be nil");
        }

        // 被调服务必须存在
        if (routeInfo.getDestService() == null) {
            throw new PolarisException(ErrorCode.API_INVALID_ARGUMENT,
                    "GetFilteredInstances param invalid, destService must exist");
        }
    }

    @Override
    public PluginType getType() {
        return PluginTypes.SERVICE_ROUTER.getBaseType();
    }

    @Override
    public RouteResult getFilteredInstances(RouteInfo routeInfo, ServiceInstances instances)
            throws PolarisException {
        validateParams(routeInfo, instances);
        return router(routeInfo, instances);
    }

    @Override
    public void postContextInit(Extensions extensions) throws PolarisException {

    }

    @Override
    public boolean enable(RouteInfo routeInfo, ServiceMetadata dstSvcInfo) {
        Boolean enableValue = routeInfo.routerIsEnabled(getName());
        return null == enableValue || enableValue;
    }

    public abstract RouteResult router(RouteInfo routeInfo, ServiceInstances instances)
            throws PolarisException;

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }
}
