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

package com.tencent.polaris.ai.api.flow;

import com.tencent.polaris.api.plugin.skill.SkillDownloadRequest;
import com.tencent.polaris.api.plugin.skill.SkillGetRequest;
import com.tencent.polaris.api.plugin.skill.SkillListRequest;
import com.tencent.polaris.client.api.SDKContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link SkillFlow} default methods.
 *
 * @author Fishtail Fu
 */
@RunWith(MockitoJUnitRunner.class)
public class SkillFlowDefaultTest {

    /**
     * 测试目的：接口默认实现返回 null
     * 测试场景：匿名空实现
     * 验证内容：三个方法都是 null
     */
    @Test
    public void testDefaultMethodsReturnNull() {
        // Arrange
        SkillFlow skillFlow = new SkillFlow() {
            @Override
            public String getName() {
                return "default";
            }

            @Override
            public void setSDKContext(SDKContext sdkContext) {
            }
        };

        // Act & Assert
        assertThat(skillFlow.getSkill(new SkillGetRequest())).isNull();
        assertThat(skillFlow.listSkills(new SkillListRequest())).isNull();
        assertThat(skillFlow.downloadSkill(new SkillDownloadRequest())).isNull();
    }
}
