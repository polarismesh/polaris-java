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

package com.tencent.polaris.plugins.connector.composite.zero;

import com.tencent.polaris.api.control.Destroyable;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.utils.ThreadPoolUtils;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.client.util.NamedThreadFactory;
import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Haotian Zhang
 */
public class TestConnectivityTaskManager extends Destroyable {
    private static final Logger LOG = LoggerFactory.getLogger(TestConnectivityTaskManager.class);

    private Future<?> currentTask;

    /**
     * 拉取任务的线程服务
     */
    private final ExecutorService pollingService;

    /**
     * 执行任务的线程服务
     */
    private final ExecutorService taskService;

    private final Set<ServiceKey> currentTestConnectivityTaskServiceKeys;

    private final BlockingQueue<TestConnectivityTask> taskBlockingQueue = new LinkedBlockingQueue<>();

    public TestConnectivityTaskManager(InitContext context) {
        this.pollingService = Executors.newSingleThreadExecutor(
                new NamedThreadFactory("composite-test-connectivity-polling"));
        this.taskService = Executors.newSingleThreadExecutor(
                new NamedThreadFactory("composite-test-connectivity-check"));
        this.currentTestConnectivityTaskServiceKeys = Collections.synchronizedSet(new HashSet<>());
        if (context instanceof SDKContext) {
            SDKContext sdkContext = (SDKContext) context;
            sdkContext.registerDestroyHook(this);
        }

        pollingService.submit(() -> {
            while (true) {
                TestConnectivityTask task = null;
                try {
                    task = taskBlockingQueue.take();
                    currentTask = taskService.submit(task);
                } catch (Exception e) {
                    LOG.warn("Test connectivity service {} ", task, e);
                }
            }
        });
    }

    public boolean submitTask(TestConnectivityTask newTask) {
        ServiceKey serviceKey = newTask.getCompositeServiceUpdateTask().getServiceEventKey().getServiceKey();
        boolean ifSubmitted = false;
        if (ifNeedTestConnectivity(newTask)) {
            currentTestConnectivityTaskServiceKeys.add(serviceKey);
            newTask.setCurrentTestConnectivityTaskServiceKeys(currentTestConnectivityTaskServiceKeys);
            ifSubmitted = taskBlockingQueue.offer(newTask);
        }
        return ifSubmitted;
    }

    @Override
    protected void doDestroy() {
        ThreadPoolUtils.waitAndStopThreadPools(new ExecutorService[]{pollingService, taskService});
    }

    private boolean ifNeedTestConnectivity(TestConnectivityTask newTask) {
        ServiceKey serviceKey = newTask.getCompositeServiceUpdateTask().getServiceEventKey().getServiceKey();
        if (!currentTestConnectivityTaskServiceKeys.contains(serviceKey)) {
            String revision = newTask.getDiscoverResponse().getService().getRevision().getValue();
            boolean ifLastZeroProtect = revision.startsWith(TestConnectivityTask.REVISION_PREFIX);
            boolean ifZeroProtectExpired = false;
            if (ifLastZeroProtect) {
                try {
                    long lastTimestamp =
                            Long.parseLong(revision.substring(TestConnectivityTask.REVISION_PREFIX.length()));
                    ifZeroProtectExpired =
                            (System.currentTimeMillis() - lastTimestamp) > newTask.getZeroProtectionConfig().getTestConnectivityExpiration();
                } catch (NumberFormatException ignored) {
                    return true;
                }
                return ifZeroProtectExpired;
            }
            return true;
        } else {
            return false;
        }
    }
}
