/*
 * Tencent is pleased to support the open source community by making Polaris available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.tencent.polaris.api.core;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.rpc.GetAllInstancesRequest;
import com.tencent.polaris.api.rpc.GetInstancesRequest;
import com.tencent.polaris.api.rpc.GetOneInstanceRequest;
import com.tencent.polaris.api.rpc.GetServiceRuleRequest;
import com.tencent.polaris.api.rpc.GetServicesRequest;
import com.tencent.polaris.api.rpc.InstancesFuture;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import com.tencent.polaris.api.rpc.ServiceRuleResponse;
import com.tencent.polaris.api.rpc.ServicesResponse;

import java.io.Closeable;

/**
 * 主调端相关的接口API
 *
 * @author andrewshan
 * @date 2019/8/21
 */
public interface ConsumerAPI extends AutoCloseable, Closeable {

    /**
     * 同步获取服务下全量服务列表
     *
     * @param req 请求
     * @return 服务实例列表
     * @throws PolarisException 错误码及错误信息
     */
    InstancesResponse getAllInstance(GetAllInstancesRequest req) throws PolarisException;

    /**
     * 同步获取服务下单个服务实例
     *
     * @param req 请求
     * @return 单个服务实例
     * @throws PolarisException 错误码及错误信息
     */
    InstancesResponse getOneInstance(GetOneInstanceRequest req) throws PolarisException;

    /**
     * 同步获取服务下进过路由过滤后的服务列表
     *
     * @param req 请求
     * @return 过滤后的服务列表
     * @throws PolarisException 错误码及错误信息
     */
    InstancesResponse getInstances(GetInstancesRequest req) throws PolarisException;

    /**
     * 异步获取服务下全量服务列表
     *
     * @param req 请求
     * @return 服务实例列表
     * @throws PolarisException 错误码及错误信息
     */
    InstancesFuture asyncGetAllInstances(GetAllInstancesRequest req) throws PolarisException;

    /**
     * 异步获取服务下单个服务实例
     *
     * @param req 请求
     * @return 单个服务实例
     * @throws PolarisException 错误码及错误信息
     */
    InstancesFuture asyncGetOneInstance(GetOneInstanceRequest req) throws PolarisException;

    /**
     * 异步获取服务下进过路由过滤后的服务列表
     *
     * @param req 请求
     * @return 过滤后的服务列表
     * @throws PolarisException 错误码及错误信息
     */
    InstancesFuture asyncGetInstances(GetInstancesRequest req) throws PolarisException;

    /**
     * 上报调用结果信息
     *
     * @param req 调用结果（包括成功失败，返回码，以及时延）
     * @throws PolarisException 错误码及错误信息
     */
    void updateServiceCallResult(ServiceCallResult req) throws PolarisException;

    /**
     * 获取服务规则
     *
     * @param req 请求参数
     * @return 服务规则信息
     * @throws PolarisException 错误码及错误信息
     */
    ServiceRuleResponse getServiceRule(GetServiceRuleRequest req) throws PolarisException;

    /**
     * 获取服务列表
     *
     * @param req 请求参数
     * @return 服务列表
     * @throws PolarisException 错误码及错误信息
     */
    ServicesResponse getServices(GetServicesRequest req) throws  PolarisException;

    /**
     * 监听服务下实例变化
     *
     * @param request 监听请求
     * @return 发起监听时查询的服务下的实例列表
     * @throws PolarisException
     */
    WatchServiceResponse watchService(WatchServiceRequest request) throws PolarisException;

    /**
     * 取消服务监听
     *
     * @param request 取消监听请求
     * @return 取消成功标识
     */
    boolean unWatchService(WatchServiceRequest request);

    /**
     * 清理并释放资源
     */
    void destroy();

    @Override
    default void close() {
        destroy();
    }
}
