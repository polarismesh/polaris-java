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

package com.tencent.polaris.plugin.lossless.common;

import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.event.FlowEvent;
import com.tencent.polaris.api.plugin.event.FlowEventConstants;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.client.flow.BaseFlow;

import java.time.LocalDateTime;

/**
 * @author Haotian Zhang
 */
public class LosslessEventUtils {

    public static void reportEvent(Extensions extensions, String namespace, String service, String host,
                                   int port, FlowEventConstants.EventName eventName) {
        if (extensions == null) {
            return;
        }

        FlowEvent.Builder flowEventBuilder = new FlowEvent.Builder()
                .withEventType(ServiceEventKey.EventType.LOSSLESS)
                .withEventName(eventName)
                .withTimestamp(LocalDateTime.now())
                .withClientId(extensions.getValueContext().getClientId())
                .withClientIp(extensions.getValueContext().getHost())
                .withNamespace(namespace)
                .withService(service)
                .withInstanceId(extensions.getValueContext().getInstanceId())
                .withHost(host)
                .withPort(port);

        FlowEvent flowEvent = flowEventBuilder.build();
        BaseFlow.reportFlowEvent(extensions, flowEvent);
    }
}
