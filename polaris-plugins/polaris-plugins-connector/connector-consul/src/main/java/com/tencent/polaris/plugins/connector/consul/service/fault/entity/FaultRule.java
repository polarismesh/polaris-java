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

package com.tencent.polaris.plugins.connector.consul.service.fault.entity;

import java.io.Serializable;

/**
 * TSF 故障注入规则项实体
 *
 * @author Haotian Zhang
 */
public class FaultRule implements Serializable {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -1886125299472426511L;

    /**
     * 是否开启故障注入
     */
    private Boolean enabled;

    /**
     * 延迟时长
     */
    private Integer fixedDelay;

    /**
     * 故障注入比例
     */
    private Integer delayPercentage;

    /**
     * 【中断注入】终止响应码
     */
    private Integer abortHttpStatusCode;

    /**
     * 中断注入比例
     */
    private Integer abortPercentage;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getFixedDelay() {
        return fixedDelay;
    }

    public void setFixedDelay(Integer fixedDelay) {
        this.fixedDelay = fixedDelay;
    }

    public Integer getDelayPercentage() {
        return delayPercentage;
    }

    public void setDelayPercentage(Integer delayPercentage) {
        this.delayPercentage = delayPercentage;
    }

    public Integer getAbortHttpStatusCode() {
        return abortHttpStatusCode;
    }

    public void setAbortHttpStatusCode(Integer abortHttpStatusCode) {
        this.abortHttpStatusCode = abortHttpStatusCode;
    }

    public Integer getAbortPercentage() {
        return abortPercentage;
    }

    public void setAbortPercentage(Integer abortPercentage) {
        this.abortPercentage = abortPercentage;
    }

    @Override
    public String toString() {
        return "FaultRule{" +
                "enabled=" + enabled +
                ", fixedDelay=" + fixedDelay +
                ", delayPercentage=" + delayPercentage +
                ", abortHttpStatusCode=" + abortHttpStatusCode +
                ", abortPercentage=" + abortPercentage +
                '}';
    }
}
