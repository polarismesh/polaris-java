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

package com.tencent.polaris.api.plugin.route;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.Plugin;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceMetadata;

/**
 * 【扩展点接口】服务路由
 *
 * @author andrewshan
 * @date 2019/8/21
 */
public interface ServiceRouter extends Plugin {

    enum Aspect {
        BEFORE, MIDDLE, AFTER
    }

    /**
     * 获取运行切面
     *
     * @return 运行切面
     */
    Aspect getAspect();

    /**
     * 是否启动该路由插件
     *
     * @param routeInfo 路由参数
     * @param dstSvcInfo 被调服务
     * @return 是否启用
     */
    boolean enable(RouteInfo routeInfo, ServiceMetadata dstSvcInfo);

    /**
     * 获取通过规则过滤后的服务集群信息以及服务实例列表
     *
     * @param routeInfo 路由请求信息
     * @param instances 服务实例列表
     * @return 经过过滤后的服务实例列表
     */
    RouteResult getFilteredInstances(RouteInfo routeInfo, ServiceInstances instances) throws PolarisException;
}
