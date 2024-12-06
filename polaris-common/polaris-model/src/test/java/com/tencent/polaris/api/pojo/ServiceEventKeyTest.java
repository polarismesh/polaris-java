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

package com.tencent.polaris.api.pojo;

import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import org.junit.Assert;
import org.junit.Test;

public class ServiceEventKeyTest {

    @Test
    public void verifyCase1() {
        int receiveExceptionCnt = 0;
        int expectExceptionCnt = EventType.values().length - 1;
        for (EventType eventType : EventType.values()) {
            try {
                ServiceEventKey key = new ServiceEventKey(new ServiceKey("", ""), eventType);
                key.verify();
            } catch (IllegalArgumentException ignore) {
                receiveExceptionCnt++;
            }
        }
        Assert.assertEquals(expectExceptionCnt, receiveExceptionCnt);
    }

    @Test
    public void verifyCase2() {
        int receiveExceptionCnt = 0;
        int expectExceptionCnt = EventType.values().length - 1;
        for (EventType eventType : EventType.values()) {
            try {
                ServiceEventKey key = new ServiceEventKey(new ServiceKey("test_ns", ""), eventType);
                key.verify();
            } catch (IllegalArgumentException ignore) {
                receiveExceptionCnt++;
            }
        }
        Assert.assertEquals(expectExceptionCnt, receiveExceptionCnt);
    }

    @Test
    public void verifyCase3() {
        int receiveExceptionCnt = 0;
        int expectExceptionCnt = EventType.values().length - 1;
        for (EventType eventType : EventType.values()) {
            try {
                ServiceEventKey key = new ServiceEventKey(new ServiceKey("", "test_svc"), eventType);
                key.verify();
            } catch (IllegalArgumentException ignore) {
                receiveExceptionCnt++;
            }
        }
        Assert.assertEquals(expectExceptionCnt, receiveExceptionCnt);
    }

    @Test
    public void verifyCase4() {
        int receiveExceptionCnt = 0;
        int expectExceptionCnt = 0;
        for (EventType eventType : EventType.values()) {
            try {
                ServiceEventKey key = new ServiceEventKey(new ServiceKey("test_ns", "test_svc"), eventType);
                key.verify();
            } catch (IllegalArgumentException ignore) {
                receiveExceptionCnt++;
            }
        }
        Assert.assertEquals(expectExceptionCnt, receiveExceptionCnt);
    }
}
