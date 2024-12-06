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

package com.tencent.polaris.plugins.event.tsf.v1;

import java.util.List;

public class TsfEventData {
    private long occurTime;
    private String eventName;
    private byte status;
    private String instanceId;
    private List<TsfEventDataPair> dimensions;
    private List<TsfEventDataPair> additionalMsg;

    public TsfEventData() {
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public void setOccurTime(long occurTime) {
        this.occurTime = occurTime;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public void setStatus(byte status) {
        this.status = status;
    }

    public void setDimensions(List<TsfEventDataPair> dimensions) {
        this.dimensions = dimensions;
    }

    public void setAdditionalMsg(List<TsfEventDataPair> additionalMsg) {
        this.additionalMsg = additionalMsg;
    }

    @Override
    public String toString() {
        return "TsfEventData{" +
                "occurTime=" + occurTime +
                ", eventName='" + eventName + '\'' +
                ", status=" + status +
                ", instanceId='" + instanceId + '\'' +
                ", dimensions=" + dimensions +
                ", additionalMsg=" + additionalMsg +
                '}';
    }
}
