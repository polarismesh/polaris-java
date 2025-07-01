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

package com.tencent.polaris.api.plugin.registry;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.Plugin;
import com.tencent.polaris.api.plugin.server.ServerConnector;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.pojo.ServiceRule;
import com.tencent.polaris.api.pojo.Services;
import java.util.List;
import java.util.Set;

/**
 * 【扩展点接口】本地缓存扩展点
 *
 * @author andrewshan
 * @date 2019/8/21
 */
public interface LocalRegistry extends Plugin {

    /**
     * 获取服务列表
     *
     * @return Set
     */
    Set<ServiceKey> getServices();

    /**
     * 获取服务规则
     *
     * @param filter 规则参数
     * @return ServiceRule
     */
    ServiceRule getServiceRule(ResourceFilter filter);

    /**
     * 加载服务规则信息
     *
     * @param svcEventKey 服务信息
     * @param notifier 获取后的回调通知
     * @throws PolarisException 异常信息
     */
    void loadServiceRule(ServiceEventKey svcEventKey, EventCompleteNotifier notifier) throws PolarisException;

    /**
     * 获取服务列表
     *
     * @param filter 服务获取参数
     * @return 获取服务列表
     */
    Services getServices(ResourceFilter filter);

    /**
     * 加载服务列表信息
     *
     * @param svcEventKey 服务信息
     * @param notifier 获取后的回调通知
     * @throws PolarisException 异常信息
     */
    void loadServices(ServiceEventKey svcEventKey, EventCompleteNotifier notifier) throws PolarisException;

    /**
     * 获取实例列表
     *
     * @param filter 实例获取参数
     * @return 实例列表
     */
    ServiceInstances getInstances(ResourceFilter filter);

    /**
     * 非阻塞向{@link ServerConnector}发起一次缓存远程加载操作
     * 如果已经加载过了，那就直接进行notify
     * 否则，加载完毕后调用notify函数
     *
     * @param svcEventKey 服务标识
     * @param notifier represent an async request.
     * @throws PolarisException SDK被销毁则抛出异常
     */
    void loadInstances(ServiceEventKey svcEventKey, EventCompleteNotifier notifier) throws PolarisException;

    /**
     * 批量更新服务实例状态，properties存放的是状态值，当前支持2个key
     * 1. ReadyToServe: 故障熔断标识，true or false
     * 2. DynamicWeight：动态权重值
     *
     * @param request 服务实例批量更新请求
     */
    void updateInstances(ServiceUpdateRequest request);

    /**
     * 注册资源事件监听器
     *
     * @param listener 监听器
     */
    void registerResourceListener(ResourceEventListener listener);

    /**
     * 提示缓存某个服务被 Watch 监听了变化
     *
     * @param svcEventKey
     */
    void watchResource(ServiceEventKey svcEventKey);

    /**
     * 提示缓存某个服务取消 Watch 监听了变化
     *
     * @param svcEventKey
     */
    void unwatchResource(ServiceEventKey svcEventKey);
}
