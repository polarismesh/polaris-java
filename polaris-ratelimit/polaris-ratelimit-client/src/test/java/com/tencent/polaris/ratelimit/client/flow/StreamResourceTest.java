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

import com.tencent.polaris.api.plugin.ratelimiter.QuotaBucket;
import com.tencent.polaris.api.plugin.ratelimiter.RemoteQuotaInfo;
import com.tencent.polaris.client.pojo.Node;
import com.tencent.polaris.specification.api.v1.traffic.manage.ratelimiter.RateLimiterProto.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test for {@link StreamResource}.
 * 复现限流规则更新后 counters 中旧 DurationBaseCallback 未清理的 bug。
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class StreamResourceTest {

    private static final String TEST_SERVICE = "testService";
    private static final String TEST_NAMESPACE = "testNamespace";
    private static final String TEST_LABELS = "|";
    private static final int COUNTER_KEY = 100;
    private static final int DURATION_SECONDS = 30;

    @Mock
    private RateLimitWindow oldWindow;

    @Mock
    private RateLimitWindow newWindow;

    @Mock
    private QuotaBucket oldBucket;

    @Mock
    private QuotaBucket newBucket;

    private Map<ServiceIdentifier, InitializeRecord> initRecordMap;
    private Map<Integer, DurationBaseCallback> countersMap;
    private StreamResource streamResource;

    @Before
    public void setUp() {
        when(oldWindow.getUniqueKey()).thenReturn("oldRevision#testService#testNamespace#|");
        when(newWindow.getUniqueKey()).thenReturn("newRevision#testService#testNamespace#|");
        when(oldWindow.getAllocatingBucket()).thenReturn(oldBucket);
        when(newWindow.getAllocatingBucket()).thenReturn(newBucket);

        // 用 @JustForTest 测试构造器创建，跳过 gRPC channel 建立
        streamResource = new StreamResource(new Node("test-host", 8081), null, null, null);
        initRecordMap = streamResource.getInitRecord();
        countersMap = streamResource.getCounters();
    }

    /**
     * 复现规则更新后 counters.putIfAbsent 导致旧回调未清理的 bug
     * 测试目的：验证规则 revision 变更后，服务端返回相同 counterKey 时，
     * 旧窗口的 DurationBaseCallback 是否被新窗口覆盖
     * 测试场景：
     * 1. 旧 revision 初始化，counterKey=100 映射到 oldWindow
     * 2. 模拟规则更新：删除旧 initRecord，添加新 initRecord 指向 newWindow
     * 3. 新 revision 初始化，服务端返回相同 counterKey=100
     * 4. 服务端 ACQUIRE 响应到达，counterKey=100，检查更新的是哪个窗口
     * 验证内容：ACQUIRE 响应应该更新 newWindow，而不是 oldWindow
     */
    @Test
    public void testRuleRevisionUpdate_CounterKeyReuse_OldCallbackNotReplaced() throws Exception {
        ServiceIdentifier serviceId = new ServiceIdentifier(TEST_SERVICE, TEST_NAMESPACE, TEST_LABELS);

        // 步骤 1：模拟旧 revision 的 INIT 响应
        InitializeRecord oldRecord = new InitializeRecord(oldWindow);
        initRecordMap.put(serviceId, oldRecord);

        RateLimitInitResponse oldInitResp = buildInitResponse(COUNTER_KEY, DURATION_SECONDS, 150000);
        invokeHandleInitResponse(streamResource, oldInitResp);

        // 验证：counters 中 counterKey=100 指向 oldWindow
        DurationBaseCallback callbackAfterOldInit = countersMap.get(COUNTER_KEY);
        assertThat(callbackAfterOldInit).isNotNull();
        assertThat(callbackAfterOldInit.getRateLimitWindow()).isSameAs(oldWindow);

        // 步骤 2：模拟规则更新 — 旧 initRecord 被替换为指向 newWindow
        // （对应 StreamResource.getInitRecord 中检测到 window 变化时的替换逻辑）
        InitializeRecord newRecord = new InitializeRecord(newWindow);
        initRecordMap.put(serviceId, newRecord);

        // 步骤 3：新 revision 的 INIT 响应到达，服务端复用了相同的 counterKey
        RateLimitInitResponse newInitResp = buildInitResponse(COUNTER_KEY, DURATION_SECONDS, 150000);
        invokeHandleInitResponse(streamResource, newInitResp);

        // 关键验证：counters 中 counterKey=100 应该指向 newWindow
        // BUG: 由于 putIfAbsent，实际仍指向 oldWindow
        DurationBaseCallback callbackAfterNewInit = countersMap.get(COUNTER_KEY);
        assertThat(callbackAfterNewInit.getRateLimitWindow())
                .as("规则更新后，counterKey=%d 的回调应指向新窗口 newWindow，但实际指向了旧窗口 oldWindow。"
                        + "这导致服务端的配额响应被路由到已删除的旧窗口，新窗口收不到远程配额更新，"
                        + "每 30 秒降级到 REMOTE_TO_LOCAL 本地限流。", COUNTER_KEY)
                .isSameAs(newWindow);
    }

    /**
     * 验证 ACQUIRE 响应路由到错误窗口
     * 测试目的：确认 counterKey 映射到旧窗口后，ACQUIRE 响应的 onRemoteUpdate 调用了旧窗口
     * 测试场景：在 counterKey 映射错误的状态下，模拟服务端 ACQUIRE 响应
     * 验证内容：onRemoteUpdate 应该调用 newBucket，但实际调用了 oldBucket
     */
    @Test
    public void testAcquireResponse_RoutedToOldWindow_AfterRuleUpdate() throws Exception {
        ServiceIdentifier serviceId = new ServiceIdentifier(TEST_SERVICE, TEST_NAMESPACE, TEST_LABELS);

        // 步骤 1：旧 revision 初始化
        InitializeRecord oldRecord = new InitializeRecord(oldWindow);
        initRecordMap.put(serviceId, oldRecord);
        invokeHandleInitResponse(streamResource, buildInitResponse(COUNTER_KEY, DURATION_SECONDS, 150000));

        // 步骤 2：规则更新，新 revision 初始化
        InitializeRecord newRecord = new InitializeRecord(newWindow);
        initRecordMap.put(serviceId, newRecord);
        invokeHandleInitResponse(streamResource, buildInitResponse(COUNTER_KEY, DURATION_SECONDS, 150000));

        // 步骤 3：模拟服务端 ACQUIRE 响应
        RateLimitReportResponse reportResp = RateLimitReportResponse.newBuilder()
                .setCode(200000)
                .setTimestamp(System.currentTimeMillis())
                .addQuotaLefts(QuotaLeft.newBuilder()
                        .setCounterKey(COUNTER_KEY)
                        .setLeft(140000)
                        .setClientCount(4)
                        .build())
                .build();
        invokeHandleReportResponse(streamResource, reportResp);

        // 验证：onRemoteUpdate 应该调用 newBucket（新窗口），而不是 oldBucket（旧窗口）
        // BUG: 实际调用了 oldBucket
        // 修复后：newBucket 被调用 2 次（INIT 1 次 + ACQUIRE 1 次），oldBucket 只在旧 INIT 中被调用 1 次
        verify(newBucket, times(2))
                .onRemoteUpdate(any(RemoteQuotaInfo.class));
        verify(oldBucket, times(1))
                .onRemoteUpdate(any(RemoteQuotaInfo.class));
    }

    /**
     * 构建 INIT 响应
     */
    private RateLimitInitResponse buildInitResponse(int counterKey, int durationSeconds, int quotaLeft) {
        return RateLimitInitResponse.newBuilder()
                .setCode(200000)
                .setClientKey(1)
                .setTimestamp(System.currentTimeMillis())
                .setTarget(LimitTarget.newBuilder()
                        .setService(TEST_SERVICE)
                        .setNamespace(TEST_NAMESPACE)
                        .setLabels(TEST_LABELS)
                        .build())
                .addCounters(QuotaCounter.newBuilder()
                        .setCounterKey(counterKey)
                        .setDuration(durationSeconds)
                        .setLeft(quotaLeft)
                        .setClientCount(4)
                        .build())
                .build();
    }

    /**
     * handleRateLimitInitResponse 是 private，为保留对它的细粒度测试用反射调用。
     */
    private void invokeHandleInitResponse(StreamResource resource, RateLimitInitResponse response) throws Exception {
        Method method = StreamResource.class.getDeclaredMethod("handleRateLimitInitResponse", RateLimitInitResponse.class);
        method.setAccessible(true);
        method.invoke(resource, response);
    }

    private void invokeHandleReportResponse(StreamResource resource, RateLimitReportResponse response) throws Exception {
        Method method = StreamResource.class.getDeclaredMethod("handleRateLimitReportResponse", RateLimitReportResponse.class);
        method.setAccessible(true);
        method.invoke(resource, response);
    }
}
