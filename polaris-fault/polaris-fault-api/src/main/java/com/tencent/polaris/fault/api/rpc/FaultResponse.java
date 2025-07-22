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

package com.tencent.polaris.fault.api.rpc;

/**
 * 故障注入响应
 *
 * @author Haotian Zhang
 */
public class FaultResponse {

    private boolean isFaultInjected;

    private AbortResult abortResult;

    private DelayResult delayResult;

    public FaultResponse(boolean isFaultInjected) {
        this(isFaultInjected, null, null);
    }

    public FaultResponse(boolean isFaultInjected, AbortResult abortResult) {
        this(isFaultInjected, abortResult, null);
    }

    public FaultResponse(boolean isFaultInjected, DelayResult delayResult) {
        this(isFaultInjected, null, delayResult);
    }

    public FaultResponse(boolean isFaultInjected, AbortResult abortResult, DelayResult delayResult) {
        this.isFaultInjected = isFaultInjected;
        this.abortResult = abortResult;
        this.delayResult = delayResult;
    }

    public boolean isFaultInjected() {
        return isFaultInjected;
    }

    public void setFaultInjected(boolean faultInjected) {
        isFaultInjected = faultInjected;
    }

    public AbortResult getAbortResult() {
        return abortResult;
    }

    public void setAbortResult(AbortResult abortResult) {
        this.abortResult = abortResult;
    }

    public DelayResult getDelayResult() {
        return delayResult;
    }

    public void setDelayResult(DelayResult delayResult) {
        this.delayResult = delayResult;
    }

    @Override
    public String toString() {
        return "FaultResponse{" +
                "isFaultInjected=" + isFaultInjected +
                "abortResult=" + abortResult +
                ", delayResult=" + delayResult +
                '}';
    }
}
