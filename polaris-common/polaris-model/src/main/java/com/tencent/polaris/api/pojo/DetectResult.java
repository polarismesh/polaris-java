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

package com.tencent.polaris.api.pojo;

import java.util.Date;

/**
 * 健康探测结果
 *
 * @author andrewshan, Haotian Zhang
 */
public class DetectResult {

    /**
     * 探测持续时间
     */
    private final long elapseTime;
    /**
     * 上一次的探测时间
     */
    private final Date lastDetectTime;
    /**
     * 探测返回结果
     */
    private final RetStatus retStatus;
    /**
     * 下一次探测时间
     */
    private final Date nextDetectTime;
    /**
     * 探测类型，与插件名相同
     */
    private String detectType;

    public DetectResult(RetStatus retStatus, long elapseTime, Date lastDetectTime, Date nextDetectTime) {
        this.retStatus = retStatus;
        this.elapseTime = elapseTime;
        this.lastDetectTime = lastDetectTime;
        this.nextDetectTime = nextDetectTime;
    }

    public DetectResult(RetStatus retStatus) {
        this.retStatus = retStatus;
        this.elapseTime = 0;
        this.lastDetectTime = new Date();
        this.nextDetectTime = new Date();
    }

    public String getDetectType() {
        return detectType;
    }

    public void setDetectType(String detectType) {
        this.detectType = detectType;
    }

    public long getElapseTime() {
        return elapseTime;
    }

    public RetStatus getRetStatus() {
        return retStatus;
    }

    public Date getLastDetectTime() {
        return lastDetectTime;
    }

    public Date getNextDetectTime() {
        return nextDetectTime;
    }

    @Override
    public String toString() {
        return "DetectResult{" +
                "detectType='" + detectType + '\'' +
                ", elapseTime=" + elapseTime +
                ", lastDetectTime=" + lastDetectTime +
                ", retStatus=" + retStatus +
                ", nextDetectTime=" + nextDetectTime +
                '}';
    }
}
