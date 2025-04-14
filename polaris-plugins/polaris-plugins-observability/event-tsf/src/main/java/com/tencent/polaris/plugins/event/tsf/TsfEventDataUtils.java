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

package com.tencent.polaris.plugins.event.tsf;

import com.tencent.polaris.api.plugin.event.BaseEvent;
import com.tencent.polaris.api.plugin.event.EventConstants;
import com.tencent.polaris.api.plugin.event.tsf.TsfEventDataConstants;
import com.tencent.polaris.api.pojo.ServiceEventKey;

import java.util.Objects;

/**
 * @author Haotian Zhang
 */
public class TsfEventDataUtils {

    public static String convertEventName(BaseEvent flowEvent) {
        if (Objects.equals(flowEvent.getEventType(), ServiceEventKey.EventType.CIRCUIT_BREAKING)) {
            return TsfEventDataConstants.CIRCUIT_BREAKER_EVENT_NAME;
        } else if (Objects.equals(flowEvent.getEventType(), ServiceEventKey.EventType.RATE_LIMITING)) {
            return TsfEventDataConstants.RATE_LIMIT_EVENT_NAME;
        }
        return "";
    }

    public static Byte convertStatus(BaseEvent flowEvent) {
        if (Objects.equals(flowEvent.getCurrentStatus(), EventConstants.Status.OPEN) &&
                Objects.equals(flowEvent.getPreviousStatus(), EventConstants.Status.CLOSE)) {
            return TsfEventDataConstants.STATUS_TRIGGER;
        } else if (Objects.equals(flowEvent.getCurrentStatus(), EventConstants.Status.CLOSE) ||
                (Objects.equals(flowEvent.getCurrentStatus(), EventConstants.Status.DESTROY) &&
                        !Objects.equals(flowEvent.getPreviousStatus(), EventConstants.Status.CLOSE))) {
            return TsfEventDataConstants.STATUS_RECOVER;
        } else if (Objects.equals(flowEvent.getCurrentStatus(), EventConstants.Status.LIMITED)) {
            return TsfEventDataConstants.STATUS_TRIGGER;
        } else if (Objects.equals(flowEvent.getCurrentStatus(), EventConstants.Status.UNLIMITED)) {
            return TsfEventDataConstants.STATUS_RECOVER;
        }
        return -1;
    }
}
