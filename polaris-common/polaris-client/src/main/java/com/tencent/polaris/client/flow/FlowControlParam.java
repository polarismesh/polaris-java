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

package com.tencent.polaris.client.flow;

/**
 * 流程控制参数
 */
public interface FlowControlParam {

    /**
     * 流程超时时间
     *
     * @return ms
     */
    long getTimeoutMs();

    /**
     * 设置流程超时时间
     *
     * @param timeoutMs 超时毫秒
     */
    void setTimeoutMs(long timeoutMs);

    /**
     * 重试间隔时间
     *
     * @return ms
     */
    long getRetryIntervalMs();

    /**
     * 设置重试间隔
     *
     * @param retryIntervalMs 重试间隔
     */
    void setRetryIntervalMs(long retryIntervalMs);

    /**
     * 流程最大重试次数
     *
     * @return int
     */
    int getMaxRetry();

    /**
     * 设置最大重试次数
     *
     * @param maxRetry 最大重试次数
     */
    void setMaxRetry(int maxRetry);
}
