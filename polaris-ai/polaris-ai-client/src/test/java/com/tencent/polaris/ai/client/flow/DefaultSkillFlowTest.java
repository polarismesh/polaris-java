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

package com.tencent.polaris.ai.client.flow;

import com.tencent.polaris.ai.client.internal.SkillPersistentHandler;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.exception.ServerCodes;
import com.tencent.polaris.api.plugin.skill.SkillConnector;
import com.tencent.polaris.api.plugin.skill.SkillGetRequest;
import com.tencent.polaris.api.plugin.skill.SkillGetResponse;
import com.tencent.polaris.api.plugin.skill.SkillResourceVersion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Test for {@link DefaultSkillFlow}.
 *
 * @author polaris
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultSkillFlowTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private SkillConnector skillConnector;

    /**
     * 测试远程成功后走内存缓存回退
     * 测试目的：成功响应写入内存，网络失败时不依赖落盘完成也能回退
     * 测试场景：connector 先成功再抛 NETWORK_ERROR
     * 验证内容：第二次返回第一次的 content
     */
    @Test
    public void testGetSkillFallsBackToMemoryCacheOnNetworkError() throws IOException {
        SkillPersistentHandler handler = new SkillPersistentHandler(
                temporaryFolder.newFolder().getAbsolutePath(), true, 1, 0, 10L);
        DefaultSkillFlow flow = new DefaultSkillFlow(skillConnector, handler, true);

        SkillGetRequest request = new SkillGetRequest();
        request.setNamespace("default");
        request.setName("sql-analysis");
        request.setVersion("1.0.0");

        SkillGetResponse remote = successResponse("skill-v1", "1.0.0");
        when(skillConnector.getSkill(request)).thenReturn(remote)
                .thenThrow(new PolarisException(ErrorCode.NETWORK_ERROR, "down"));

        SkillGetResponse first = flow.getSkill(request);
        SkillGetResponse fallback = flow.getSkill(request);
        assertThat(first.getContent()).isEqualTo("skill-v1");
        assertThat(fallback.getContent()).isEqualTo("skill-v1");
    }

    /**
     * 测试空 version 回退 active 指针
     * 测试目的：请求 version 为空时用服务端 resolved version 作为 active
     * 测试场景：第一次空 version 成功，第二次网络失败仍空 version
     * 验证内容：回退到 1.1.0 的 content
     */
    @Test
    public void testGetSkillEmptyVersionFallsBackViaActivePointer() throws IOException {
        SkillPersistentHandler handler = new SkillPersistentHandler(
                temporaryFolder.newFolder().getAbsolutePath(), true, 1, 0, 10L);
        DefaultSkillFlow flow = new DefaultSkillFlow(skillConnector, handler, true);

        SkillGetRequest request = new SkillGetRequest();
        request.setNamespace("default");
        request.setName("sql-analysis");
        request.setVersion("");

        SkillGetResponse remote = successResponse("skill-active", "1.1.0");
        when(skillConnector.getSkill(request)).thenReturn(remote)
                .thenThrow(new PolarisException(ErrorCode.NETWORK_ERROR, "down"));

        flow.getSkill(request);
        SkillGetResponse fallback = flow.getSkill(request);
        assertThat(fallback.getContent()).isEqualTo("skill-active");
    }

    /**
     * 测试网络失败回退本地
     * 测试目的：NETWORK_ERROR 时读盘
     * 测试场景：盘上已有 1.0.0，connector 抛网络错误
     * 验证内容：返回缓存 content
     */
    @Test
    public void testGetSkillFallsBackToLocalCacheOnNetworkError() throws IOException {
        SkillPersistentHandler handler = new SkillPersistentHandler(
                temporaryFolder.newFolder().getAbsolutePath(), true, 1, 0, 10L);
        DefaultSkillFlow flow = new DefaultSkillFlow(skillConnector, handler, true);

        SkillGetResponse cached = successResponse("cached-skill", "1.0.0");
        handler.saveGetSkill("default", "sql-analysis", "1.0.0", cached);

        SkillGetRequest request = new SkillGetRequest();
        request.setNamespace("default");
        request.setName("sql-analysis");
        request.setVersion("1.0.0");
        when(skillConnector.getSkill(request)).thenThrow(new PolarisException(ErrorCode.NETWORK_ERROR, "down"));

        SkillGetResponse result = flow.getSkill(request);
        assertThat(result.getContent()).isEqualTo("cached-skill");
    }

    /**
     * 测试 NOT_FOUND 不回退
     * 测试目的：业务失败不读本地
     * 测试场景：connector 返回 NOT_FOUND 响应
     * 验证内容：返回码为 NOT_FOUND
     */
    @Test
    public void testGetSkillNotFoundDoesNotFallback() throws IOException {
        SkillPersistentHandler handler = new SkillPersistentHandler(
                temporaryFolder.newFolder().getAbsolutePath(), true, 1, 0, 10L);
        DefaultSkillFlow flow = new DefaultSkillFlow(skillConnector, handler, true);

        SkillGetResponse cached = successResponse("cached-skill", "1.0.0");
        handler.saveGetSkill("default", "sql-analysis", "1.0.0", cached);

        SkillGetRequest request = new SkillGetRequest();
        request.setNamespace("default");
        request.setName("sql-analysis");
        request.setVersion("1.0.0");
        SkillGetResponse notFound = new SkillGetResponse();
        notFound.setCode(ServerCodes.NOT_FOUND_RESOURCE);
        when(skillConnector.getSkill(request)).thenReturn(notFound);

        SkillGetResponse result = flow.getSkill(request);
        assertThat(result.getCode()).isEqualTo(ServerCodes.NOT_FOUND_RESOURCE);
    }

    private SkillGetResponse successResponse(String content, String version) {
        SkillGetResponse response = new SkillGetResponse();
        response.setCode(ServerCodes.EXECUTE_SUCCESS);
        response.setContent(content);
        SkillResourceVersion resourceVersion = new SkillResourceVersion();
        resourceVersion.setVersion(version);
        response.setResourceVersion(resourceVersion);
        return response;
    }
}
