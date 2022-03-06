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

package com.tencent.polaris.plugins.connector.composite;

import com.tencent.polaris.api.plugin.server.ServerEvent;
import com.tencent.polaris.api.plugin.server.ServiceEventHandler;
import com.tencent.polaris.plugins.connector.common.DestroyableServerConnector;
import com.tencent.polaris.plugins.connector.common.ServiceUpdateTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scheduled task for updating service information.
 *
 * @author Haotian Zhang
 */
public class CompositeServiceUpdateTask extends ServiceUpdateTask {

    private static final Logger LOG = LoggerFactory.getLogger(
            CompositeServiceUpdateTask.class);

    public CompositeServiceUpdateTask(ServiceEventHandler handler, DestroyableServerConnector connector) {
        super(handler, connector);
    }

    @Override
    protected void execute() throws Throwable {

    }

    @Override
    protected void handle(Throwable throwable) {

    }

    @Override
    public boolean notifyServerEvent(ServerEvent serverEvent) {
        return false;
    }
}
