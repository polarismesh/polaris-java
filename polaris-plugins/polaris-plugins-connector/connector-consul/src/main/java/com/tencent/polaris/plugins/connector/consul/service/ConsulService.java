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

package com.tencent.polaris.plugins.connector.consul.service;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.ConsulRawClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.polaris.api.control.Destroyable;
import com.tencent.polaris.api.utils.ThreadPoolUtils;
import com.tencent.polaris.client.util.NamedThreadFactory;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.plugins.connector.common.ServiceUpdateTask;
import com.tencent.polaris.plugins.connector.consul.ConsulContext;
import org.slf4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Haotian Zhang
 */
public abstract class ConsulService extends Destroyable {

    private static final Logger LOG = LoggerFactory.getLogger(ConsulService.class);

    protected final ConsulClient consulClient;

    protected final ConsulRawClient consulRawClient;

    protected final ConsulContext consulContext;

    protected final ObjectMapper mapper;

    protected final ExecutorService refreshExecutor;

    protected boolean enable = true;

    protected boolean isReset = false;

    public ConsulService(ConsulClient consulClient, ConsulRawClient consulRawClient, ConsulContext consulContext,
                         String threadName, ObjectMapper mapper) {
        this.consulClient = consulClient;
        this.consulRawClient = consulRawClient;
        this.consulContext = consulContext;
        this.mapper = mapper;
        NamedThreadFactory threadFactory = new NamedThreadFactory(threadName);
        this.refreshExecutor = Executors.newFixedThreadPool(8, threadFactory);
    }

    public void asyncSendRequest(ServiceUpdateTask serviceUpdateTask) {
        this.refreshExecutor.execute(() -> {
            try {
                sendRequest(serviceUpdateTask);
            } catch (Throwable throwable) {
                LOG.error("Send request with throwable.", throwable);
            }
        });
    }

    protected abstract void sendRequest(ServiceUpdateTask serviceUpdateTask);

    @Override
    protected void doDestroy() {
        ThreadPoolUtils.waitAndStopThreadPools(new ExecutorService[]{refreshExecutor});
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        if (!this.enable && enable) {
            this.isReset = true;
        }
        this.enable = enable;
    }
}
