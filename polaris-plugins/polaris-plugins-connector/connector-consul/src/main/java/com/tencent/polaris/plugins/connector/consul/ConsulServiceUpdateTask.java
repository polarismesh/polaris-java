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

package com.tencent.polaris.plugins.connector.consul;

import com.tencent.polaris.api.plugin.server.ServerEvent;
import com.tencent.polaris.api.plugin.server.ServiceEventHandler;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.plugins.connector.common.DestroyableServerConnector;
import com.tencent.polaris.plugins.connector.common.ServiceUpdateTask;
import com.tencent.polaris.plugins.connector.common.constant.ServiceUpdateTaskConstant;
import com.tencent.polaris.plugins.connector.consul.service.ConsulService;
import org.slf4j.Logger;

/**
 * Consul service update task.
 *
 * @author Haotian Zhang
 */
public class ConsulServiceUpdateTask extends ServiceUpdateTask {

    private static final Logger LOG = LoggerFactory.getLogger(ConsulServiceUpdateTask.class);

    public ConsulServiceUpdateTask(ServiceEventHandler handler,
                                   DestroyableServerConnector connector) {
        super(handler, connector);
    }

    @Override
    public void execute() {
        execute(this);
    }

    @Override
    public void execute(ServiceUpdateTask serviceUpdateTask) {
        if (serviceUpdateTask.getTaskType() == ServiceUpdateTaskConstant.Type.FIRST) {
            LOG.info("[ConsulAPIConnector]start to run first task {}", serviceUpdateTask);
        } else {
            LOG.debug("[ConsulAPIConnector]start to run task {}", serviceUpdateTask);
        }
        if (serverConnector instanceof ConsulAPIConnector) {
            ConsulAPIConnector consulAPIConnector = (ConsulAPIConnector) serverConnector;
            ConsulService consulService = consulAPIConnector.getConsulService(serviceUpdateTask.getServiceEventKey().getEventType());
            if (consulService != null) {
                consulService.asyncSendRequest(serviceUpdateTask);
            }
        }
    }

    @Override
    protected void handle(Throwable throwable) {
        LOG.error("Consul service task execute error.", throwable);
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
