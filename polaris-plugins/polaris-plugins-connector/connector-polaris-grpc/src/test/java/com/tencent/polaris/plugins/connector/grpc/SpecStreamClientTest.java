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

package com.tencent.polaris.plugins.connector.grpc;

import com.google.protobuf.StringValue;
import com.tencent.polaris.api.config.global.ClusterType;
import com.tencent.polaris.api.plugin.server.EventHandler;
import com.tencent.polaris.api.plugin.server.ServerEvent;
import com.tencent.polaris.api.plugin.server.ServiceEventHandler;
import com.tencent.polaris.api.pojo.RegistryCacheValue;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.plugins.connector.common.ServiceUpdateTask;
import com.tencent.polaris.plugins.connector.common.constant.ServiceUpdateTaskConstant;
import com.tencent.polaris.specification.api.v1.service.manage.ResponseProto;
import com.tencent.polaris.specification.api.v1.service.manage.ServiceProto;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

@RunWith(MockitoJUnitRunner.class)
public class SpecStreamClientTest extends TestCase {

    @Test
    public void testNormalPendingTask() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        // 每秒处理
        SpecStreamClient mockClient = new SpecStreamClient(1000);
        mockClient.putPendingTask(MockServiceUpdateTask.build(new ServiceKey("mock_namespace", "mock_service"), (task, label) -> {
            System.out.println(task.getTaskStatus() + " " + label);
            Assert.assertEquals(ServiceUpdateTaskConstant.Status.READY, task.getTaskStatus());
            Assert.assertEquals("normal", label);
            latch.countDown();
        }));

        mockClient.onNext(ResponseProto.DiscoverResponse.newBuilder()
                .setService(ServiceProto.Service.newBuilder()
                        .setName(StringValue.newBuilder().setValue("mock_service").build())
                        .setNamespace(StringValue.newBuilder().setValue("mock_namespace").build())
                        .build())
                .setType(ResponseProto.DiscoverResponse.DiscoverResponseType.INSTANCE)
                .build());

        latch.await();
    }

    @Test
    public void testAbNormalPendingTask() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        // 每秒处理
        SpecStreamClient mockClient = new SpecStreamClient(1000);
        mockClient.putPendingTask(MockServiceUpdateTask.build(new ServiceKey("mock_namespace", "mock_service"), (task, label) -> {
            System.out.println(task.getTaskStatus() + " " + label);
            Assert.assertEquals(ServiceUpdateTaskConstant.Status.RUNNING, task.getTaskStatus());
            Assert.assertEquals("retry", label);
            latch.countDown();
        }));

        // 等待 10s 中
        TimeUnit.SECONDS.sleep(10);
        mockClient.onNext(ResponseProto.DiscoverResponse.newBuilder()
                .setService(ServiceProto.Service.newBuilder()
                        .setName(StringValue.newBuilder().setValue("mock_service").build())
                        .setNamespace(StringValue.newBuilder().setValue("mock_namespace").build())
                        .build())
                .setType(ResponseProto.DiscoverResponse.DiscoverResponseType.INSTANCE)
                .build());

        latch.await();
    }

    private static final class MockServiceUpdateTask extends ServiceUpdateTask {

        private final BiConsumer<MockServiceUpdateTask, String> consumer;

        public static MockServiceUpdateTask build(ServiceKey serviceKey, BiConsumer<MockServiceUpdateTask, String> consumer) {
            ServiceEventHandler handler = new ServiceEventHandler(new ServiceEventKey(serviceKey, ServiceEventKey.EventType.INSTANCE), new EventHandler() {
                @Override
                public boolean onEventUpdate(ServerEvent event) {
                    return false;
                }

                @Override
                public String getRevision() {
                    return "" + System.currentTimeMillis();
                }

                @Override
                public RegistryCacheValue getValue() {
                    return null;
                }
            });
            handler.setTargetCluster(ClusterType.BUILTIN_CLUSTER);
            handler.setRefreshInterval(2000);
            handler.setLastUpdateTimeMs(System.currentTimeMillis());
            MockServiceUpdateTask task = new MockServiceUpdateTask(handler, consumer);
            task.setStatus(ServiceUpdateTaskConstant.Status.READY, ServiceUpdateTaskConstant.Status.RUNNING);
            return task;
        }

        public MockServiceUpdateTask(ServiceEventHandler handler, BiConsumer<MockServiceUpdateTask, String> consumer) {
            super(handler, null);
            this.consumer = consumer;
        }

        @Override
        public void execute() {

        }

        @Override
        protected void handle(Throwable throwable) {

        }

        @Override
        public void addUpdateTaskSet() {
            consumer.accept(this, "normal");
        }

        @Override
        public boolean notifyServerEvent(ServerEvent serverEvent) {
            setStatus(ServiceUpdateTaskConstant.Status.RUNNING, ServiceUpdateTaskConstant.Status.READY);
            return false;
        }

        @Override
        public void retry() {
            consumer.accept(this, "retry");
        }
    }
}