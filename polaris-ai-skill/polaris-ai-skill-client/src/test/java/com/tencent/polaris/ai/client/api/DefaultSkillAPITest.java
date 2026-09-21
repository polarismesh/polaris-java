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

package com.tencent.polaris.ai.client.api;

import com.tencent.polaris.ai.api.flow.SkillFlow;
import com.tencent.polaris.api.plugin.common.ValueContext;
import com.tencent.polaris.api.plugin.skill.SkillDownloadRequest;
import com.tencent.polaris.api.plugin.skill.SkillDownloadResponse;
import com.tencent.polaris.api.plugin.skill.SkillGetRequest;
import com.tencent.polaris.api.plugin.skill.SkillGetResponse;
import com.tencent.polaris.api.plugin.skill.SkillListRequest;
import com.tencent.polaris.api.plugin.skill.SkillListResponse;
import com.tencent.polaris.client.api.SDKContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test for {@link DefaultSkillAPI}.
 *
 * @author Fishtail Fu
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultSkillAPITest {

    @Mock
    private SDKContext sdkContext;

    @Mock
    private SkillFlow skillFlow;

    @Mock
    private ValueContext valueContext;

    private DefaultSkillAPI skillAPI;

    @Before
    public void setUp() {
        when(sdkContext.getOrInitFlow(SkillFlow.class)).thenReturn(skillFlow);
        when(sdkContext.getValueContext()).thenReturn(valueContext);
        skillAPI = new DefaultSkillAPI(sdkContext);
        skillAPI.init();
    }

    /**
     * 测试目的：getSkill 委托 Flow
     * 测试场景：合法请求
     * 验证内容：返回 Flow 结果
     */
    @Test
    public void testGetSkillDelegatesToFlow() {
        // Arrange
        SkillGetRequest request = new SkillGetRequest();
        request.setName("sql-analysis");
        request.setNamespace("default");
        SkillGetResponse response = new SkillGetResponse();
        response.setContent("# skill");
        when(skillFlow.getSkill(request)).thenReturn(response);

        // Act
        SkillGetResponse result = skillAPI.getSkill(request);

        // Assert
        assertThat(result.getContent()).isEqualTo("# skill");
    }

    /**
     * 测试目的：listSkills 委托 Flow
     * 测试场景：空过滤 list
     * 验证内容：返回 total
     */
    @Test
    public void testListSkillsDelegatesToFlow() {
        // Arrange
        SkillListRequest request = new SkillListRequest();
        SkillListResponse response = new SkillListResponse();
        response.setTotal(2);
        when(skillFlow.listSkills(any(SkillListRequest.class))).thenReturn(response);

        // Act
        SkillListResponse result = skillAPI.listSkills(request);

        // Assert
        assertThat(result.getTotal()).isEqualTo(2);
    }

    /**
     * 测试目的：downloadSkill 委托 Flow
     * 测试场景：zip 下载
     * 验证内容：返回 filename
     */
    @Test
    public void testDownloadSkillDelegatesToFlow() {
        // Arrange
        SkillDownloadRequest request = new SkillDownloadRequest();
        request.setName("sql-analysis");
        request.setNamespace("default");
        request.setFormat("zip");
        SkillDownloadResponse response = new SkillDownloadResponse();
        response.setFilename("sql-analysis.zip");
        when(skillFlow.downloadSkill(request)).thenReturn(response);

        // Act
        SkillDownloadResponse result = skillAPI.downloadSkill(request);

        // Assert
        assertThat(result.getFilename()).isEqualTo("sql-analysis.zip");
    }
}
