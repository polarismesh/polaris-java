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

package com.tencent.polaris.discovery.client.flow;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.rpc.InstancesFuture;
import java.util.concurrent.CompletableFuture;

/**
 * AsyncFlow.java
 *
 * @author andrewshan
 * @date 2019/8/24
 */
public class AsyncFlow {

    private SyncFlow syncFlow;

    public void init(SyncFlow syncFlow) {
        this.syncFlow = syncFlow;
    }

    /**
     * 异步获取单个服务实例
     *
     * @param request 请求参数
     * @return future
     */
    public InstancesFuture commonAsyncGetOneInstance(CommonInstancesRequest request) {
        GetOneInstanceSupplier supplier = new GetOneInstanceSupplier(request, syncFlow);
        return new InstancesFuture(CompletableFuture.supplyAsync(supplier));
    }

    /**
     * 异步获取路由后的服务实例
     *
     * @param request 参数对象
     * @return 路由后的实例列表
     * @throws PolarisException 获取失败异常
     */
    public InstancesFuture commonAsyncGetInstances(CommonInstancesRequest request) throws PolarisException {
        GetInstancesSupplier supplier = new GetInstancesSupplier(request, syncFlow);
        return new InstancesFuture(CompletableFuture.supplyAsync(supplier));
    }

    /**
     * 异步获取全量服务实例
     *
     * @param request 请求对象
     * @return 全量服务实例
     * @throws PolarisException 获取失败
     */
    public InstancesFuture commonAsyncGetAllInstances(CommonInstancesRequest request) throws PolarisException {
        GetAllInstancesSupplier supplier = new GetAllInstancesSupplier(request, syncFlow);
        return new InstancesFuture(CompletableFuture.supplyAsync(supplier));
    }
}
