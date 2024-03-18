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

import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;
import com.tencent.polaris.api.pojo.RegistryCacheValue;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.pojo.ServiceRule;

import java.util.List;
import java.util.function.Consumer;

/**
 * 通过PB对象封装的服务信息
 *
 * @author andrewshan
 * @date 2019/8/22
 */
public class ServiceRuleByProto implements ServiceRule, RegistryCacheValue {

    public static final ServiceRuleByProto EMPTY_SERVICE_RULE = new ServiceRuleByProto();

    private final Object ruleValue;

    private final String revision;

    private final boolean initialized;

    private final boolean loadFromFile;

    private final EventType eventType;

    public ServiceRuleByProto(Object ruleValue, String revision, boolean loadFromFile, EventType eventType) {
        this.ruleValue = ruleValue;
        this.revision = revision;
        this.loadFromFile = loadFromFile;
        this.initialized = true;
        this.eventType = eventType;
    }

    public ServiceRuleByProto() {
        this.ruleValue = null;
        this.revision = "";
        this.loadFromFile = false;
        this.initialized = false;
        this.eventType = EventType.UNKNOWN;
    }

    @Override
    public Object getRule() {
        return ruleValue;
    }

    @Override
    public String getRevision() {
        return revision;
    }


    @Override
    public boolean isLoadedFromFile() {
        return loadFromFile;
    }

    @Override
    public EventType getEventType() {
        return eventType;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public String toString() {
        String val = "";
        if (ruleValue instanceof Message) {
            val = TextFormat.shortDebugString((Message) ruleValue);
        }
        if (ruleValue instanceof List) {
            List<Message> messages = (List<Message>) ruleValue;
            StringBuilder builder = new StringBuilder();
            messages.forEach(message -> builder.append(TextFormat.shortDebugString(message)));
            val = builder.toString();
        }
        return "ServiceRuleByProto [rule=" + (ruleValue == null ? null : val)
                + ", revision=" + revision + ", initialized="
                + initialized + ", eventType=" + eventType + "]";
    }
}
