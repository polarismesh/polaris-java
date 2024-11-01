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

package com.tencent.polaris.api.plugin.ratelimiter;

import com.tencent.polaris.api.exception.PolarisException;

import java.util.Map;

/**
 * bucket的令牌桶实现
 */
public interface QuotaBucket {

    /**
     * 在令牌桶/漏桶中进行单个配额的划扣，并返回本次分配的结果
     *
     * @param curTimeMs 当前时间点
     * @param count     需获取的配额数
     * @return 分配结果
     * @throws PolarisException 异常信息
     */
    QuotaResult allocateQuota(long curTimeMs, int count) throws PolarisException;

    /**
     * 归还配额
     *
     * @param allocateTimeMs 配额分配时间
     * @param count          需归还的配额数
     * @throws PolarisException 异常信息
     */
    void returnQuota(long allocateTimeMs, int count) throws PolarisException;

    /**
     * 释放配额（仅对于并发数限流有用）
     */
    void release();

    /**
     * 远程配额更新
     *
     * @param remoteQuotaInfo 远程同步结果
     */
    void onRemoteUpdate(RemoteQuotaInfo remoteQuotaInfo);

    /**
     * 拉取本地使用配额情况以供上报
     *
     * @param curTimeMs 当前时间点
     * @return localQuotaInfo，key为validDuration，单位为秒
     */
    Map<Integer, LocalQuotaInfo> fetchLocalUsage(long curTimeMs);

    /**
     * 获取规则的限流阈值信息
     *
     * @return 限流阈值信息，key为validDuration，单位为秒
     */
    Map<Integer, AmountInfo> getAmountInfo();
}
