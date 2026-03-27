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

package com.tencent.polaris.api.rpc;

import com.tencent.polaris.api.pojo.InstanceType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link ServiceCallResult}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class ServiceCallResultTest {

    @Test
    public void testInstanceType_DefaultValue() {
        ServiceCallResult result = new ServiceCallResult();
        assertThat(result.getInstanceType()).isEqualTo(InstanceType.MICROSERVICE);
    }

    @Test
    public void testInstanceType_SetAndGet() {
        ServiceCallResult result = new ServiceCallResult();
        result.setInstanceType(InstanceType.MCP);
        assertThat(result.getInstanceType()).isEqualTo(InstanceType.MCP);
        result.setInstanceType(InstanceType.A2A);
        assertThat(result.getInstanceType()).isEqualTo(InstanceType.A2A);
    }

    @Test
    public void testToString_ContainsInstanceType() {
        ServiceCallResult result = new ServiceCallResult();
        result.setInstanceType(InstanceType.MCP);
        String str = result.toString();
        assertThat(str).contains("instanceType=MCP");
    }
}
