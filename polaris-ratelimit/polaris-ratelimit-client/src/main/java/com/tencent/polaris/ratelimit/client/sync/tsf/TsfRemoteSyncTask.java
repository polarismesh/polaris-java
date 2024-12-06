/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.polaris.ratelimit.client.sync.tsf;

import com.tencent.polaris.api.plugin.ratelimiter.LocalQuotaInfo;
import com.tencent.polaris.api.plugin.ratelimiter.RemoteQuotaInfo;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.ratelimit.client.flow.RateLimitWindow;
import com.tencent.polaris.ratelimit.client.sync.RemoteSyncTask;

import java.util.Map;

/**
 * @author Haotian Zhang
 */
public class TsfRemoteSyncTask implements RemoteSyncTask {

    /**
     * 限流窗口
     */
    private final RateLimitWindow window;

    public TsfRemoteSyncTask(RateLimitWindow window) {
        this.window = window;
    }

    @Override
    public void run() {
        long curTimeMilli = System.currentTimeMillis();

        // 获取限流使用情况，这个场景下只取第一个
        Map<Integer, LocalQuotaInfo> localQuotaInfos = window.getAllocatingBucket().fetchLocalUsage(curTimeMilli);
        if (CollectionUtils.isNotEmpty(localQuotaInfos)) {
            for (Map.Entry<Integer, LocalQuotaInfo> entry : localQuotaInfos.entrySet()) {
                Map<String, Integer> ruleQuotaMap = TsfRateLimitMasterUtils.report(window.getRule().getId().getValue(),
                        entry.getValue().getQuotaUsed(), entry.getValue().getQuotaLimited());
                if (CollectionUtils.isNotEmpty(ruleQuotaMap)) {
                    RemoteQuotaInfo remoteQuotaInfo = new RemoteQuotaInfo(
                            ruleQuotaMap.get(window.getRule().getId().getValue()), 1, System.currentTimeMillis(), -1);
                    window.getAllocatingBucket().onRemoteUpdate(remoteQuotaInfo);
                }
                return;
            }
        }
    }

    @Override
    public RateLimitWindow getWindow() {
        return window;
    }
}
