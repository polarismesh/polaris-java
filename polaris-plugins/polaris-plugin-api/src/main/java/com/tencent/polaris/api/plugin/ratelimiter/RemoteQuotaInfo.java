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

package com.tencent.polaris.api.plugin.ratelimiter;

public class RemoteQuotaInfo {

    private final long remoteQuotaLeft;

    private final int clientCount;

    private final long curTimeMs;

    private final long durationMs;

    public RemoteQuotaInfo(long remoteQuotaLeft, int clientCount, long curTimeMs, long durationMs) {
        this.remoteQuotaLeft = remoteQuotaLeft;
        this.clientCount = clientCount;
        this.curTimeMs = curTimeMs;
        this.durationMs = durationMs;
    }

    /**
     * 远程剩余配额
     *
     * @return 剩余配额
     */
    public long getRemoteQuotaLeft() {
        return remoteQuotaLeft;
    }

    /**
     * 共享相同bucket的实例数
     *
     * @return 实例数
     */
    public int getClientCount() {
        return clientCount;
    }

    /**
     * 配额所属的时间点(客户端本地时间)
     *
     * @return clientTimeStamp
     */
    public long getCurTimeMs() {
        return curTimeMs;
    }

    /**
     * 限流时间段
     *
     * @return delay
     */
    public long getDurationMs() {
        return durationMs;
    }
}
