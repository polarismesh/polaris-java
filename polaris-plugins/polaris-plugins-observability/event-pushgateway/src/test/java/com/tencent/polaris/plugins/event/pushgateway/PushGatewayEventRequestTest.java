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

package com.tencent.polaris.plugins.event.pushgateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tencent.polaris.api.plugin.event.EventConstants;
import com.tencent.polaris.api.plugin.event.FlowEvent;
import org.junit.Test;

import java.time.LocalDateTime;

import static com.tencent.polaris.api.plugin.event.EventConstants.EventName.LosslessOnlineStart;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PushGatewayEventRequest}.
 *
 * @author Haotian Zhang
 */
public class PushGatewayEventRequestTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void test() throws JsonProcessingException {
        FlowEvent flowEvent = new FlowEvent.Builder()
                .withEventType(EventConstants.EventType.LOSSLESS)
                .withEventName(LosslessOnlineStart)
                .withTimestamp(LocalDateTime.of(2025, 1, 13, 11, 58, 42))
                .withClientId("test-client")
                .withClientIp("1.2.3.4")
                .withNamespace("test-namespace")
                .withService("test-service")
                .withHost("1.2.3.4")
                .withPort(8080)
                .build();

        PushGatewayEventRequest request = new PushGatewayEventRequest();
        request.getBatch().add(flowEvent);

        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        assertThat(mapper.writeValueAsString(request)).isEqualTo("{\"batch\":[{\"currentStatus\":null,\"previousStatus\":null,\"resourceType\":null,\"ruleName\":\"\",\"additionalParams\":{},\"event_type\":\"LOSSLESS\",\"event_name\":\"LosslessOnlineStart\",\"event_time\":\"2025-01-13 11:58:42:0000\",\"client_id\":\"test-client\",\"client_ip\":\"1.2.3.4\",\"namespace\":\"test-namespace\",\"service\":\"test-service\",\"api_protocol\":\"\",\"api_path\":\"\",\"api_method\":\"\",\"host\":\"1.2.3.4\",\"port\":\"8080\",\"source_namespace\":\"\",\"source_service\":\"\",\"labels\":\"\",\"reason\":\"\"}]}");
    }
}