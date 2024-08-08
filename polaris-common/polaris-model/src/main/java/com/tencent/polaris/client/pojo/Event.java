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

package com.tencent.polaris.client.pojo;

import com.tencent.polaris.api.pojo.BaseInstance;
import com.tencent.polaris.api.utils.StringUtils;

import java.util.*;

/**
 * Event object for sdk, all event log will be encapsulated and output by this object
 */
public class Event {

    public Event() {
    }

    public Event(String clientId, BaseInstance baseInstance, String eventName) {
        this.clientId = clientId;
        this.baseInstance = baseInstance;
        this.eventName = eventName;
    }

    private String clientId;

    private BaseInstance baseInstance;

    private String eventName;

    private String details;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public BaseInstance getBaseInstance() {
        return baseInstance;
    }

    public void setBaseInstance(BaseInstance baseInstance) {
        this.baseInstance = baseInstance;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    @Override
    public String toString() {
        List<String> values = new ArrayList<>();
        values.add(StringUtils.defaultString(getClientId()));
        values.add(StringUtils.defaultString(getBaseInstance().getNamespace()));
        values.add(StringUtils.defaultString(getBaseInstance().getService()));
        values.add(StringUtils.defaultString(getEventName()));
        values.add(StringUtils.defaultString(getBaseInstance().getHost()));
        values.add(StringUtils.defaultString(Integer.toString(getBaseInstance().getPort())));
        values.add(StringUtils.defaultString(getDetails()));
        return String.join("|", values);
    }
}
