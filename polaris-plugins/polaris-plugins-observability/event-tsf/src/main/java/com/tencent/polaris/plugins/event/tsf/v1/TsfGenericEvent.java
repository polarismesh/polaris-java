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

package com.tencent.polaris.plugins.event.tsf.v1;

import java.util.ArrayList;
import java.util.List;

public class TsfGenericEvent {
    private String region;
    private String appId;
    private List<TsfEventData> eventData = new ArrayList<>();

    public TsfGenericEvent() {
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public void setEventData(List<TsfEventData> eventData) {
        this.eventData = eventData;
    }

    public void addEventData(List<TsfEventData> eventData) {
        this.eventData.addAll(eventData);
    }

    public List<TsfEventData> getEventData() {
        return eventData;
    }
}
