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

package com.tencent.polaris.ratelimit.client.flow;

import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.client.pojo.Node;
import com.tencent.polaris.client.remote.ServiceAddressRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.ScheduledThreadPoolExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Test for {@link RateLimitExtension#stopSyncTask}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class RateLimitExtensionStopSyncTaskTest {

    private static final String UNIQUE_KEY = "rev#svc#ns#labels";

    @Mock
    private Extensions extensions;

    @Mock
    private RateLimitWindow window;

    @Mock
    private RateLimitWindowSet windowSet;

    @Mock
    private ServiceAddressRepository serviceAddressRepository;

    private ScheduledThreadPoolExecutor syncExecutor;
    private ScheduledThreadPoolExecutor expireExecutor;
    private RateLimitExtension rateLimitExtension;
    private AsyncRateLimitConnector connector;

    @Before
    public void setUp() {
        syncExecutor = new ScheduledThreadPoolExecutor(1);
        expireExecutor = new ScheduledThreadPoolExecutor(1);
        rateLimitExtension = new RateLimitExtension(extensions, syncExecutor, expireExecutor);
        connector = new AsyncRateLimitConnector();

        when(window.getWindowSet()).thenReturn(windowSet);
        when(window.getSvcKey()).thenReturn(new ServiceKey("ns", "svc"));
        when(window.getLabels()).thenReturn("labels");
        when(window.getUniqueKey()).thenReturn(UNIQUE_KEY);
        // stream 已回收时这些字段不应被读到
        lenient().when(window.getRemoteCluster()).thenReturn(new ServiceKey("ns", "limiter"));
        lenient().when(window.getServiceAddressRepository()).thenReturn(serviceAddressRepository);
        when(windowSet.getAsyncRateLimitConnector()).thenReturn(connector);
        lenient().when(windowSet.getRateLimitExtension()).thenReturn(rateLimitExtension);
    }

    @After
    public void tearDown() {
        if (rateLimitExtension != null) {
            rateLimitExtension.destroy();
        }
    }

    /**
     * 测试目的：stream 已被回收时，stopSyncTask 不应通过 getStreamCounterSet 触发新建连。
     * 测试场景：connector 为空，调用 stopSyncTask。
     * 验证内容：scheduledTasks 已清理；nodeToStream / uniqueKeyToStream 保持为空。
     */
    @Test
    public void stopSyncTask_NoExistingStream_ShouldNotCreateStream() throws Exception {
        lenient().when(serviceAddressRepository.getServiceAddressNode()).thenReturn(new Node("limiter-host", 8081));

        assertThat(connector.getNodeToStream()).isEmpty();
        assertThat(connector.getUniqueKeyToStream()).isEmpty();

        rateLimitExtension.stopSyncTask(UNIQUE_KEY, window);

        Thread.sleep(500);

        assertThat(rateLimitExtension.getScheduledTasks()).doesNotContainKey(UNIQUE_KEY);
        assertThat(connector.getNodeToStream()).isEmpty();
        assertThat(connector.getUniqueKeyToStream()).isEmpty();
    }
}
