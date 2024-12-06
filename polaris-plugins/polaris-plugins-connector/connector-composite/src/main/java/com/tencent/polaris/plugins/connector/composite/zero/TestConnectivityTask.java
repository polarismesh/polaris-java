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

package com.tencent.polaris.plugins.connector.composite.zero;

import com.google.protobuf.BoolValue;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt32Value;
import com.tencent.polaris.api.config.consumer.ZeroProtectionConfig;
import com.tencent.polaris.api.exception.ServerCodes;
import com.tencent.polaris.api.plugin.server.ServerEvent;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.plugins.connector.composite.CompositeServiceUpdateTask;
import com.tencent.polaris.specification.api.v1.service.manage.ResponseProto;
import com.tencent.polaris.specification.api.v1.service.manage.ServiceProto;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Haotian Zhang
 */
public class TestConnectivityTask implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(TestConnectivityTask.class);

    private final CompositeServiceUpdateTask compositeServiceUpdateTask;

    private final ResponseProto.DiscoverResponse discoverResponse;

    private final ZeroProtectionConfig zeroProtectionConfig;

    private Set<ServiceKey> currentTestConnectivityTaskServiceKeys;

    public static final String REVISION_PREFIX = "zero-protect-";

    public TestConnectivityTask(CompositeServiceUpdateTask compositeServiceUpdateTask,
                                ResponseProto.DiscoverResponse discoverResponse,
                                ZeroProtectionConfig zeroProtectionConfig) {
        this.compositeServiceUpdateTask = compositeServiceUpdateTask;
        this.discoverResponse = discoverResponse;
        this.zeroProtectionConfig = zeroProtectionConfig;
    }

    @Override
    public void run() {
        try {
            List<ServiceProto.Instance> originalList = discoverResponse.getInstancesList();
            List<ServiceProto.Instance> zeroProtect = new ArrayList<>();
            ExecutorService executorService =
                    Executors.newFixedThreadPool(zeroProtectionConfig.getTestConnectivityParallel());
            CountDownLatch latch = new CountDownLatch(originalList.size());
            AtomicInteger passingCount = new AtomicInteger(0);
            AtomicInteger notPassingCount = new AtomicInteger(0);
            AtomicInteger isolateCount = new AtomicInteger(0);
            for (ServiceProto.Instance instance : originalList) {
                // passing 并且可连通
                if (isPassing(instance)) {
                    executorService.submit(() -> {
                        if (NetUtils.testConnectivity(instance.getHost().getValue(), instance.getPort().getValue(),
                                zeroProtectionConfig.getTestConnectivityTimeout())) {
                            zeroProtect.add(ServiceProto.Instance.newBuilder(instance).setHealthy(BoolValue.of(true)).build());
                            passingCount.incrementAndGet();
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Instance[{}:{}] of {} is tested passing.", instance.getHost().getValue(),
                                        instance.getPort().getValue(), instance.getService().getValue());
                            }
                        } else {
                            zeroProtect.add(ServiceProto.Instance.newBuilder(instance).setHealthy(BoolValue.of(false)).build());
                            notPassingCount.incrementAndGet();
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Instance[{}:{}] of {} is tested not passing.", instance.getHost().getValue(),
                                        instance.getPort().getValue(), instance.getService().getValue());
                            }
                        }
                        latch.countDown();
                    });
                } else {
                    zeroProtect.add(instance);
                    isolateCount.incrementAndGet();
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Instance[{}:{}] of {} is isolated, skip test.", instance.getHost().getValue(),
                                instance.getPort().getValue(), instance.getService().getValue());
                    }
                    latch.countDown();
                }
            }
            if (!latch.await(2L * zeroProtectionConfig.getTestConnectivityTimeout(), TimeUnit.MILLISECONDS)) {
                LOG.error("Test connectivity is interrupted. original size: {}, zero protect size: {}.",
                        originalList.size(), zeroProtect.size());
            } else {
                LOG.info("Test end. Passing count: {}, not passing count: {}, isolate count: {}.", passingCount.get(),
                        notPassingCount.get(), isolateCount.get());
            }

            ResponseProto.DiscoverResponse.Builder newDiscoverResponseBuilder =
                    ResponseProto.DiscoverResponse.newBuilder().mergeFrom(discoverResponse);
            String oldRevision = discoverResponse.getService().getRevision().getValue();
            // 需要标识为新数据
            newDiscoverResponseBuilder.setCode(UInt32Value.of(ServerCodes.EXECUTE_SUCCESS));
            newDiscoverResponseBuilder.clearInstances();
            newDiscoverResponseBuilder.addAllInstances(zeroProtect);
            String newRevision = REVISION_PREFIX + System.currentTimeMillis();
            ServiceProto.Service.Builder newServiceBuilder = ServiceProto.Service.newBuilder()
                    .mergeFrom(newDiscoverResponseBuilder.getService());
            newServiceBuilder.setRevision(StringValue.of(newRevision));
            newDiscoverResponseBuilder.setService(newServiceBuilder.build());
            compositeServiceUpdateTask.notifyServerEventWithRevisionChecking(
                    new ServerEvent(compositeServiceUpdateTask.getServiceEventKey(),
                            newDiscoverResponseBuilder.build(), null), oldRevision);
            currentTestConnectivityTaskServiceKeys.remove(compositeServiceUpdateTask.getServiceEventKey().getServiceKey());
        } catch (Exception e) {
            LOG.error("Test connectivity failed.", e);
        }
    }

    private boolean isPassing(ServiceProto.Instance instance) {
        return instance != null && !instance.getIsolate().getValue();
    }

    public CompositeServiceUpdateTask getCompositeServiceUpdateTask() {
        return compositeServiceUpdateTask;
    }

    public ResponseProto.DiscoverResponse getDiscoverResponse() {
        return discoverResponse;
    }

    public ZeroProtectionConfig getZeroProtectionConfig() {
        return zeroProtectionConfig;
    }

    public Set<ServiceKey> getCurrentTestConnectivityTaskServiceKeys() {
        return currentTestConnectivityTaskServiceKeys;
    }

    public void setCurrentTestConnectivityTaskServiceKeys(Set<ServiceKey> currentTestConnectivityTaskServiceKeys) {
        this.currentTestConnectivityTaskServiceKeys = currentTestConnectivityTaskServiceKeys;
    }
}
