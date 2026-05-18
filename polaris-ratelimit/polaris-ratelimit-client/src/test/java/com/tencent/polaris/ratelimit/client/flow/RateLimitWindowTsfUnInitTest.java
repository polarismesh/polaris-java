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

import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.ratelimit.client.utils.RateLimitConstants;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link RateLimitWindow} unInit on TSF cluster.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class RateLimitWindowTsfUnInitTest {

    private static final String UNIQUE_KEY = "rev#svc#ns#tsf-labels";

    @Mock
    private RateLimitWindowSet windowSet;

    @Mock
    private RateLimitExtension rateLimitExtension;

    @Before
    public void setUp() {
        when(windowSet.getRateLimitExtension()).thenReturn(rateLimitExtension);
    }

    /**
     * TSF 集群限流窗口 unInit 时不应被 LOCAL_MODE early return 跳过 stopSyncTask。
     *
     * 测试目的：验证 isTsfCluster=true 的窗口 unInit 后，
     * RateLimitExtension.stopSyncTask 必须被调用以取消已在 init() 中提交的 TsfRemoteSyncTask。
     */
    @Test
    public void unInit_TsfClusterWindow_ShouldStopSyncTask() {
        RateLimitWindow window = new RateLimitWindow(
                windowSet,
                new ServiceKey("ns", "svc"),
                "tsf-labels",
                UNIQUE_KEY,
                RateLimitConstants.CONFIG_QUOTA_LOCAL_MODE,
                true);
        // 模拟 init() 已完成，TsfRemoteSyncTask 已提交
        window.setStatus(RateLimitWindow.WindowStatus.INITIALIZED.ordinal());

        window.unInit();

        verify(rateLimitExtension).stopSyncTask(UNIQUE_KEY, window);
    }
}
