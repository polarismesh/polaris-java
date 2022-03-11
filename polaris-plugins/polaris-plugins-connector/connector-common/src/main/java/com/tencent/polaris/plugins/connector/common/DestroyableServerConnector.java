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

package com.tencent.polaris.plugins.connector.common;

import com.tencent.polaris.api.control.Destroyable;
import com.tencent.polaris.api.plugin.server.ServerConnector;
import com.tencent.polaris.api.pojo.DefaultInstance;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.Services;
import com.tencent.polaris.plugins.connector.common.constant.ServiceUpdateTaskConstant.Status;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Destroyable server connector.
 *
 * @author Haotian Zhang
 */
public abstract class DestroyableServerConnector extends Destroyable implements ServerConnector {

    protected static final int TASK_RETRY_INTERVAL_MS = 500;
    private static final Logger LOG = LoggerFactory.getLogger(DestroyableServerConnector.class);
    protected final Map<ServiceEventKey, ServiceUpdateTask> updateTaskSet = new ConcurrentHashMap<>();

    /**
     * Check if initialized.
     *
     * @return true if initialized.
     */
    public abstract boolean isInitialized();

    /**
     * Retry service update task.
     *
     * @param updateTask
     */
    public void retryServiceUpdateTask(ServiceUpdateTask updateTask) {
        LOG.info("[ServerConnector]retry schedule task for {}, retry delay {}", updateTask, TASK_RETRY_INTERVAL_MS);
        updateTask.setStatus(Status.RUNNING, Status.READY);
        if (isDestroyed()) {
            return;
        }
        submitServiceHandler(updateTask, TASK_RETRY_INTERVAL_MS);
    }

    /**
     * Submit service update task.
     *
     * @param updateTask
     * @param delayMs
     */
    protected abstract void submitServiceHandler(ServiceUpdateTask updateTask, long delayMs);

    /**
     * Add long-running task.
     *
     * @param serviceUpdateTask
     */
    public void addLongRunningTask(ServiceUpdateTask serviceUpdateTask) {
        updateTaskSet.put(serviceUpdateTask.getServiceEventKey(), serviceUpdateTask);
    }

    /**
     * Get service instance information synchronously.
     *
     * @param serviceUpdateTask
     * @return instance
     */
    public List<DefaultInstance> syncGetServiceInstances(ServiceUpdateTask serviceUpdateTask) {
        return null;
    }

    /**
     * Get service list information synchronously.
     *
     * @param serviceUpdateTask
     * @return services
     */
    public Services syncGetServices(ServiceUpdateTask serviceUpdateTask) {
        return null;
    }

    public class UpdateServiceTask implements Runnable {

        @Override
        public void run() {
            for (ServiceUpdateTask serviceUpdateTask : updateTaskSet.values()) {
                if (isDestroyed()) {
                    break;
                }
                if (!serviceUpdateTask.needUpdate()) {
                    continue;
                }
                submitServiceHandler(serviceUpdateTask, 0);
            }
        }
    }
}
