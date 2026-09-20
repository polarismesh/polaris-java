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
import com.tencent.polaris.api.plugin.skill.SkillDownloadRequest;
import com.tencent.polaris.api.plugin.skill.SkillDownloadResponse;
import com.tencent.polaris.api.plugin.skill.SkillGetRequest;
import com.tencent.polaris.api.plugin.skill.SkillGetResponse;
import com.tencent.polaris.api.plugin.skill.SkillListRequest;
import com.tencent.polaris.api.plugin.skill.SkillListResponse;
import com.tencent.polaris.api.plugin.skill.SkillResourceVersion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Test for {@link DefaultSkillFlow}.
 *
 * @author Fishtail Fu
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultSkillFlowTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private SkillConnector skillConnector;

    /**
     * 测试关闭落盘时不走内存回退
     * 测试目的：Skill 只降级文件缓存，不在内存里留一份
     * 测试场景：persistEnable=false，connector 先成功再抛 NETWORK_ERROR
     * 验证内容：第二次抛出 NETWORK_ERROR
     */
    @Test
    public void testGetSkillDoesNotFallbackWhenPersistDisabled() throws IOException {
        // Arrange
        SkillPersistentHandler handler = new SkillPersistentHandler(
                temporaryFolder.newFolder().getAbsolutePath(), false, 1, 0, 10L);
        DefaultSkillFlow flow = new DefaultSkillFlow(skillConnector, handler, true);
        SkillGetRequest request = new SkillGetRequest();
        request.setNamespace("default");
        request.setName("sql-analysis");
        request.setVersion("1.0.0");
        SkillGetResponse remote = successResponse("skill-v1", "1.0.0");
        when(skillConnector.getSkill(request)).thenReturn(remote)
                .thenThrow(new PolarisException(ErrorCode.NETWORK_ERROR, "down"));

        // Act
        SkillGetResponse first = flow.getSkill(request);

        // Assert
        assertThat(first.getContent()).isEqualTo("skill-v1");
        assertThatThrownBy(() -> flow.getSkill(request))
                .isInstanceOf(PolarisException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NETWORK_ERROR);
    }

    /**
     * 测试空 version 回退 active 指针
     * 测试目的：请求 version 为空时用服务端 resolved version 作为 active
     * 测试场景：第一次空 version 成功，第二次网络失败仍空 version
     * 验证内容：回退到 1.1.0 的 content
     */
    @Test
    public void testGetSkillEmptyVersionFallsBackViaActivePointer() throws IOException {
        // Arrange
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

        // Act
        flow.getSkill(request);
        waitUntilPersisted(handler, "default", "sql-analysis", "1.1.0");
        waitUntilActiveVersionPersisted(handler, "default", "sql-analysis", "1.1.0");
        SkillGetResponse fallback = flow.getSkill(request);

        // Assert
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
        // Arrange
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

        // Act
        SkillGetResponse result = flow.getSkill(request);

        // Assert
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
        // Arrange
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

        // Act
        SkillGetResponse result = flow.getSkill(request);

        // Assert
        assertThat(result.getCode()).isEqualTo(ServerCodes.NOT_FOUND_RESOURCE);
    }

    /**
     * 测试目的：listSkills 直接走远端，无 fallback
     * 测试场景：connector 抛 NETWORK_ERROR
     * 验证内容：异常向上抛出
     */
    @Test
    public void testListSkillsDoesNotFallback() throws IOException {
        // Arrange
        SkillPersistentHandler handler = new SkillPersistentHandler(
                temporaryFolder.newFolder().getAbsolutePath(), true, 1, 0, 10L);
        DefaultSkillFlow flow = new DefaultSkillFlow(skillConnector, handler, true);
        SkillListRequest request = new SkillListRequest();
        request.setNamespace("default");
        when(skillConnector.listSkills(request)).thenThrow(new PolarisException(ErrorCode.NETWORK_ERROR, "down"));

        // Act & Assert
        assertThatThrownBy(() -> flow.listSkills(request))
                .isInstanceOf(PolarisException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NETWORK_ERROR);
    }

    /**
     * 测试目的：listSkills 成功返回远端结果
     * 测试场景：connector 返回列表
     * 验证内容：total 与远端一致
     */
    @Test
    public void testListSkillsReturnsRemote() throws IOException {
        // Arrange
        DefaultSkillFlow flow = new DefaultSkillFlow(skillConnector, null, true);
        SkillListRequest request = new SkillListRequest();
        SkillListResponse remote = new SkillListResponse();
        remote.setTotal(3);
        when(skillConnector.listSkills(request)).thenReturn(remote);

        // Act
        SkillListResponse result = flow.listSkills(request);

        // Assert
        assertThat(result.getTotal()).isEqualTo(3);
        assertThat(flow.getName()).isEqualTo("default");
    }

    /**
     * 测试目的：zip 下载网络失败回退磁盘
     * 测试场景：盘上已有 zip，connector 抛 NETWORK_ERROR
     * 验证内容：返回缓存 zip
     */
    @Test
    public void testDownloadSkillFallsBackToLocalZip() throws IOException {
        // Arrange
        SkillPersistentHandler handler = new SkillPersistentHandler(
                temporaryFolder.newFolder().getAbsolutePath(), true, 1, 0, 10L);
        DefaultSkillFlow flow = new DefaultSkillFlow(skillConnector, handler, true);
        SkillDownloadResponse cached = new SkillDownloadResponse();
        cached.setVersion("1.0.0");
        cached.setFilename("sql-analysis.zip");
        cached.setZipContent("zip-bytes".getBytes(StandardCharsets.UTF_8));
        handler.saveDownload("default", "sql-analysis", "1.0.0", "zip", cached);
        SkillDownloadRequest request = new SkillDownloadRequest();
        request.setNamespace("default");
        request.setName("sql-analysis");
        request.setVersion("1.0.0");
        request.setFormat("zip");
        when(skillConnector.downloadSkill(request)).thenThrow(new PolarisException(ErrorCode.NETWORK_ERROR, "down"));

        // Act
        SkillDownloadResponse result = flow.downloadSkill(request);

        // Assert
        assertThat(result.getFilename()).isEqualTo("sql-analysis.zip");
        assertThat(result.getZipContent()).isEqualTo("zip-bytes".getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 测试目的：关闭 fallback 时网络错误不读盘
     * 测试场景：盘上有缓存，fallbackToLocalCache=false
     * 验证内容：抛出 NETWORK_ERROR
     */
    @Test
    public void testDownloadSkillDoesNotFallbackWhenDisabled() throws IOException {
        // Arrange
        SkillPersistentHandler handler = new SkillPersistentHandler(
                temporaryFolder.newFolder().getAbsolutePath(), true, 1, 0, 10L);
        DefaultSkillFlow flow = new DefaultSkillFlow(skillConnector, handler, false);
        SkillDownloadResponse cached = new SkillDownloadResponse();
        cached.setVersion("1.0.0");
        cached.setContent("# md");
        handler.saveDownload("default", "sql-analysis", "1.0.0", "markdown", cached);
        SkillDownloadRequest request = new SkillDownloadRequest();
        request.setNamespace("default");
        request.setName("sql-analysis");
        request.setVersion("1.0.0");
        when(skillConnector.downloadSkill(request)).thenThrow(new PolarisException(ErrorCode.NETWORK_ERROR, "down"));

        // Act & Assert
        assertThatThrownBy(() -> flow.downloadSkill(request))
                .isInstanceOf(PolarisException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NETWORK_ERROR);
    }

    /**
     * 测试目的：成功下载会异步落盘，空 format 视为 markdown
     * 测试场景：远端成功且 version 来自响应
     * 验证内容：磁盘可读回 content
     */
    @Test
    public void testDownloadSkillPersistsMarkdownWhenFormatBlank() throws IOException {
        // Arrange
        SkillPersistentHandler handler = new SkillPersistentHandler(
                temporaryFolder.newFolder().getAbsolutePath(), true, 1, 0, 10L);
        DefaultSkillFlow flow = new DefaultSkillFlow(skillConnector, handler, true);
        SkillDownloadRequest request = new SkillDownloadRequest();
        request.setNamespace("default");
        request.setName("sql-analysis");
        SkillDownloadResponse remote = new SkillDownloadResponse();
        remote.setCode(ServerCodes.EXECUTE_SUCCESS);
        remote.setVersion("2.0.0");
        remote.setContent("# markdown");
        when(skillConnector.downloadSkill(request)).thenReturn(remote);

        // Act
        flow.downloadSkill(request);
        waitUntilDownloadPersisted(handler, "default", "sql-analysis", "2.0.0", "markdown");
        SkillDownloadResponse loaded = handler.loadDownload("default", "sql-analysis", "2.0.0", "markdown");

        // Assert
        assertThat(loaded.getContent()).isEqualTo("# markdown");
    }

    /**
     * 测试目的：code=0 视为成功并落盘
     * 测试场景：远端 code 为 0
     * 验证内容：磁盘有 content
     */
    @Test
    public void testGetSkillPersistsWhenCodeZero() throws IOException {
        // Arrange
        SkillPersistentHandler handler = new SkillPersistentHandler(
                temporaryFolder.newFolder().getAbsolutePath(), true, 1, 0, 10L);
        DefaultSkillFlow flow = new DefaultSkillFlow(skillConnector, handler, true);
        SkillGetRequest request = new SkillGetRequest();
        request.setNamespace("default");
        request.setName("sql-analysis");
        request.setVersion("1.0.0");
        SkillGetResponse remote = successResponse("zero-ok", "1.0.0");
        remote.setCode(0);
        when(skillConnector.getSkill(request)).thenReturn(remote);

        // Act
        flow.getSkill(request);
        waitUntilPersisted(handler, "default", "sql-analysis", "1.0.0");

        // Assert
        assertThat(handler.loadGetSkill("default", "sql-analysis", "1.0.0").getContent()).isEqualTo("zero-ok");
    }

    /**
     * 测试目的：NOT_FOUND 不覆盖本地缓存
     * 测试场景：盘上已有 1.0.0，远端返回 NOT_FOUND
     * 验证内容：磁盘 content 仍是旧值
     */
    @Test
    public void testGetSkillNotFoundDoesNotPersist() throws IOException {
        // Arrange
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
        notFound.setContent("gone");
        when(skillConnector.getSkill(request)).thenReturn(notFound);

        // Act
        flow.getSkill(request);

        // Assert
        assertThat(handler.loadGetSkill("default", "sql-analysis", "1.0.0").getContent()).isEqualTo("cached-skill");
    }

    /**
     * 测试目的：成功但无法解析 version 时不落盘
     * 测试场景：request version 空且 resourceVersion 为空
     * 验证内容：磁盘无文件
     */
    @Test
    public void testGetSkillDoesNotPersistWhenVersionBlank() throws IOException {
        // Arrange
        SkillPersistentHandler handler = new SkillPersistentHandler(
                temporaryFolder.newFolder().getAbsolutePath(), true, 1, 0, 10L);
        DefaultSkillFlow flow = new DefaultSkillFlow(skillConnector, handler, true);
        SkillGetRequest request = new SkillGetRequest();
        request.setNamespace("default");
        request.setName("sql-analysis");
        SkillGetResponse remote = new SkillGetResponse();
        remote.setCode(ServerCodes.EXECUTE_SUCCESS);
        remote.setContent("no-version");
        when(skillConnector.getSkill(request)).thenReturn(remote);

        // Act
        flow.getSkill(request);

        // Assert
        assertThat(handler.loadGetSkill("default", "sql-analysis", "")).isNull();
        assertThat(handler.loadActiveVersion("default", "sql-analysis")).isNull();
    }

    /**
     * 测试目的：fallback 时无 version 指针则继续抛异常
     * 测试场景：request version 空，无 active 文件
     * 验证内容：NETWORK_ERROR
     */
    @Test
    public void testGetSkillFallbackWithoutActiveVersionRethrows() throws IOException {
        // Arrange
        SkillPersistentHandler handler = new SkillPersistentHandler(
                temporaryFolder.newFolder().getAbsolutePath(), true, 1, 0, 10L);
        DefaultSkillFlow flow = new DefaultSkillFlow(skillConnector, handler, true);
        SkillGetRequest request = new SkillGetRequest();
        request.setNamespace("default");
        request.setName("sql-analysis");
        when(skillConnector.getSkill(request)).thenThrow(new PolarisException(ErrorCode.NETWORK_ERROR, "down"));

        // Act & Assert
        assertThatThrownBy(() -> flow.getSkill(request))
                .isInstanceOf(PolarisException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NETWORK_ERROR);
    }

    /**
     * 测试目的：下载成功但 version 为空不落盘
     * 测试场景：request/response version 都空
     * 验证内容：磁盘无 markdown
     */
    @Test
    public void testDownloadSkillDoesNotPersistWhenVersionBlank() throws IOException {
        // Arrange
        SkillPersistentHandler handler = new SkillPersistentHandler(
                temporaryFolder.newFolder().getAbsolutePath(), true, 1, 0, 10L);
        DefaultSkillFlow flow = new DefaultSkillFlow(skillConnector, handler, true);
        SkillDownloadRequest request = new SkillDownloadRequest();
        request.setNamespace("default");
        request.setName("sql-analysis");
        SkillDownloadResponse remote = new SkillDownloadResponse();
        remote.setCode(ServerCodes.EXECUTE_SUCCESS);
        remote.setContent("# md");
        when(skillConnector.downloadSkill(request)).thenReturn(remote);

        // Act
        flow.downloadSkill(request);

        // Assert
        assertThat(handler.loadDownload("default", "sql-analysis", "", "markdown")).isNull();
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

    private void waitUntilPersisted(SkillPersistentHandler handler, String namespace, String name, String version) {
        long deadline = System.currentTimeMillis() + 2000L;
        SkillGetResponse persisted = handler.loadGetSkill(namespace, name, version);
        while (persisted == null && System.currentTimeMillis() < deadline) {
            persisted = handler.loadGetSkill(namespace, name, version);
        }
        assertThat(persisted).isNotNull();
    }

    private void waitUntilActiveVersionPersisted(SkillPersistentHandler handler, String namespace, String name,
            String expectedVersion) {
        long deadline = System.currentTimeMillis() + 2000L;
        String activeVersion = handler.loadActiveVersion(namespace, name);
        while (!expectedVersion.equals(activeVersion) && System.currentTimeMillis() < deadline) {
            activeVersion = handler.loadActiveVersion(namespace, name);
        }
        assertThat(activeVersion).isEqualTo(expectedVersion);
    }

    private void waitUntilDownloadPersisted(SkillPersistentHandler handler, String namespace, String name,
            String version, String format) {
        long deadline = System.currentTimeMillis() + 2000L;
        SkillDownloadResponse persisted = handler.loadDownload(namespace, name, version, format);
        while (persisted == null && System.currentTimeMillis() < deadline) {
            persisted = handler.loadDownload(namespace, name, version, format);
        }
        assertThat(persisted).isNotNull();
    }
}
