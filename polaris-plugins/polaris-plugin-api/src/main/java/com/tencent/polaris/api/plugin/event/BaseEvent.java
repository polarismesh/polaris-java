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

package com.tencent.polaris.api.plugin.event;

import java.time.LocalDateTime;

/**
 * 事件接口类。
 */
public interface BaseEvent {

    EventConstants.EventType getEventType();

    EventConstants.EventName getEventName();

    LocalDateTime getTimestamp();

    String getClientId();

    String getClientIp();

    String getNamespace();

    EventConstants.Status getCurrentStatus();

    EventConstants.Status getPreviousStatus();

    EventConstants.ResourceType getResourceType();

    String getReason();

    String convertMessage();
}