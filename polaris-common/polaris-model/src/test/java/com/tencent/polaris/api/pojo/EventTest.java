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

package com.tencent.polaris.api.pojo;

import com.tencent.polaris.client.pojo.Event;
import org.junit.Assert;
import org.junit.Test;

public class EventTest {

    @Test
    public void testEventString() {
        Event event = new Event();
        event.setClientId("client1");
        DefaultBaseInstance defaultBaseInstance = new DefaultBaseInstance();
        defaultBaseInstance.setNamespace("test");
        defaultBaseInstance.setService("TestSvc");
        defaultBaseInstance.setHost("127.0.0.1");
        defaultBaseInstance.setPort(8888);
        event.setBaseInstance(defaultBaseInstance);
        event.setEventName("HealthCheck");
        event.setDetails("{\"protocol\":\"http\"}");
        Assert.assertEquals(
                "client1|test|TestSvc|HealthCheck|127.0.0.1|8888|{\"protocol\":\"http\"}", event.toString());
    }
}
