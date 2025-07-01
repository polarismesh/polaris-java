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

package com.tencent.polaris.api.pojo;

/**
 * 健康探测结果
 *
 * @author andrewshan, Haotian Zhang
 * @date 2019/8/21
 */
public class DetectResult {

    /**
     * 探测类型，与插件名相同
     */
    private String detectType;

    /**
     * 探测返回状态码
     */
    private final int statusCode;

    /**
     * 探测的时延
     */
    private final long delay;

    /**
     * 探测返回结果
     */
    private final RetStatus retStatus;

    public DetectResult(int statusCode, long delay, RetStatus retStatus) {
        this.statusCode = statusCode;
        this.delay = delay;
        this.retStatus = retStatus;
    }

    public String getDetectType() {
        return detectType;
    }

    public void setDetectType(String detectType) {
        this.detectType = detectType;
    }

    public RetStatus getRetStatus() {
        return retStatus;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public long getDelay() {
        return delay;
    }

    @Override
    public String toString() {
        return "DetectResult{" +
                "detectType='" + detectType + '\'' +
                ", statusCode=" + statusCode +
                ", delay=" + delay +
                ", retStatus=" + retStatus +
                '}';
    }
}
