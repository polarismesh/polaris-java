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

package com.tencent.polaris.plugins.connector.nacos;

import com.alibaba.nacos.api.naming.NamingService;
import com.tencent.polaris.api.plugin.server.ServerEvent;
import com.tencent.polaris.api.plugin.server.ServiceEventHandler;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.plugins.connector.common.DestroyableServerConnector;
import com.tencent.polaris.plugins.connector.common.ServiceUpdateTask;
import com.tencent.polaris.plugins.connector.common.constant.ServiceUpdateTaskConstant.Type;
import org.slf4j.Logger;

public class NacosServiceUpdateTask extends ServiceUpdateTask {


    private static final Logger LOG = LoggerFactory.getLogger(ServiceUpdateTask.class);

    public NacosServiceUpdateTask(ServiceEventHandler handler, DestroyableServerConnector connector) {
        super(handler, connector);
    }

    @Override
    public void execute() {
        execute(this);
    }

    /**
     * 事件驱动：
     * 根据service update task来加载远端服务实例列表
     * subscribe 一个新event listener
     * event listener 收到远端服务实例列表变化的事件后触发回调
     * 创建新事件
     * 回调serviceEventHandler
     */
    @Override
    public void execute(ServiceUpdateTask serviceUpdateTask) {
        if (serviceUpdateTask.getTaskType() == Type.FIRST) {
            LOG.info("[ServerConnector]start to run first task {}", serviceUpdateTask);
        } else {
            LOG.debug("[ServerConnector]start to run task {}", serviceUpdateTask);
        }
        if (serverConnector instanceof NacosConnector) {
            NacosConnector nacosConnector = (NacosConnector) serverConnector;
            NacosService nacosService = nacosConnector.getNacosService(
                    serviceUpdateTask.getServiceEventKey().getServiceKey().getNamespace());
            if (serviceUpdateTask.getServiceEventKey().getEventType() == EventType.SERVICE) {
                nacosService.sendServiceRequest(serviceUpdateTask);
            } else if (serviceUpdateTask.getServiceEventKey().getEventType() == EventType.INSTANCE) {
                nacosService.doInstanceSubscribe(serviceUpdateTask);
            }
        }
    }

    @Override
    protected void handle(Throwable throwable) {
        LOG.error("Nacos service task execute error.", throwable);
    }

    @Override
    public boolean notifyServerEvent(ServerEvent serverEvent) {
        return false;
    }

    @Override
    public boolean needUpdate() {
        if (serverConnector.isDiscoveryEnable()) {
            return super.needUpdate();
        } else {
            return false;
        }
    }
}
