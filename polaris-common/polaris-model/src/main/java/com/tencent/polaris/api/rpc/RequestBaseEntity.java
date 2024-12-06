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

package com.tencent.polaris.api.rpc;

/**
 * 作为RPC请求的基类型
 *
 * @author andrewshan
 * @date 2019/8/21
 */
public class RequestBaseEntity extends BaseEntity {

    /**
     * 请求超时时间
     */
    private long timeoutMs;

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public String toString() {
        return "RequestBaseEntity{" +
                "timeoutMs=" + timeoutMs +
                "} " + super.toString();
    }
}
