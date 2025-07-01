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

package com.tencent.polaris.plugins.registry.memory;

import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceKey;
import org.junit.Assert;
import org.junit.Test;

public class MessagePersistHandlerTest {

    @Test
    public void testMessagePersistHandler_shouldLoadFromStore() {
        MessagePersistHandler messagePersistHandler = new MessagePersistHandler(
                "/root", 1, 1, 1000);
        ServiceEventKey serviceEventKey = new ServiceEventKey(
                new ServiceKey("Test", "testSvc"), ServiceEventKey.EventType.SERVICE);
        boolean result1 = messagePersistHandler.shouldLoadFromStore(serviceEventKey);
        Assert.assertTrue(result1);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        boolean result2 = messagePersistHandler.shouldLoadFromStore(serviceEventKey);
        Assert.assertFalse(result2);
        ServiceEventKey serviceEventKey1 = new ServiceEventKey(
                new ServiceKey("Test", "testSvc1"), ServiceEventKey.EventType.SERVICE);
        boolean result11 = messagePersistHandler.shouldLoadFromStore(serviceEventKey1);
        Assert.assertTrue(result11);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        boolean result12 = messagePersistHandler.shouldLoadFromStore(serviceEventKey1);
        Assert.assertFalse(result12);
        try {
            Thread.sleep(25000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        boolean result3 = messagePersistHandler.shouldLoadFromStore(serviceEventKey);
        Assert.assertTrue(result3);
        boolean result13 = messagePersistHandler.shouldLoadFromStore(serviceEventKey1);
        Assert.assertTrue(result13);
    }
}
