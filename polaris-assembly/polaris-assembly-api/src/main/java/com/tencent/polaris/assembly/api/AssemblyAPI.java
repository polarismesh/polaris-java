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

package com.tencent.polaris.assembly.api;

import java.io.Closeable;
import java.util.List;

import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import com.tencent.polaris.assembly.api.pojo.GetOneInstanceRequest;
import com.tencent.polaris.assembly.api.pojo.GetReachableInstancesRequest;
import com.tencent.polaris.assembly.api.pojo.TraceAttributes;

public interface AssemblyAPI extends AutoCloseable, Closeable {

    /**
     * 初始化服务实例列表，避免在服务调用的时候进行拉取，可有效减少调用时延。
     * @param serviceKey 服务名和命名空间
     */
    void initService(ServiceKey serviceKey);

    /**
     * 获取路由后的服务实例列表，贯穿服务发现、服务路由的逻辑
     * @param request 多个符合路由条件的服务实例
     * @return List<Instance>
     */
    List<Instance> getReachableInstances(GetReachableInstancesRequest request);

    /**
     * 获取单个服务实例，贯穿服务发现、服务路由、负载均衡的逻辑，最终返回单个服务实例
     * @param request request
     * @return Instance
     */
    Instance getOneInstance(GetOneInstanceRequest request);

    /**
     * 上报服务调用结果，服务调用结果可用于熔断统计和监控数据
     * @param result 调用结果（包括成功失败，返回码，以及时延）
     */
    void updateServiceCallResult(ServiceCallResult result);

    /**
     * 上报调用链属性数据
     */
    void updateTraceAttributes(TraceAttributes traceAttributes);

    /**
     * 清理并释放资源
     */
    default void destroy() {

    }

    @Override
    default void close() {
        destroy();
    }

}
