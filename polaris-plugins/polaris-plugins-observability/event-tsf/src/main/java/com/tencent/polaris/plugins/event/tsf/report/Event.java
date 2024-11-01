/*
 * Tencent is pleased to support the open source community by making Polaris available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
 *
 *  Licensed under the BSD 3-Clause License (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.tencent.polaris.plugins.event.tsf.report;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * @author jiangfan
 */
public abstract class Event implements Serializable {

    //* 事件类型
    @JsonProperty("type")
    private String event;

    // 事件对象
    @JsonProperty("source")
    private String object;

    // 事件发生时间
    private final long timestamp = System.currentTimeMillis();

    public Event(String source) {
        this.object = source;
    }

    public Event() {
        this.object = "";
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public long getTimestamp() {
        return timestamp;
    }

}
