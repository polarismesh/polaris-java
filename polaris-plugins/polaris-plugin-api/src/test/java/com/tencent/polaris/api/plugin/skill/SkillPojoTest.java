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

package com.tencent.polaris.api.plugin.skill;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for Skill plugin-api POJOs.
 *
 * @author Fishtail Fu
 */
@RunWith(MockitoJUnitRunner.class)
public class SkillPojoTest {

    /**
     * 测试目的：GetSkill 请求字段
     * 测试场景：填满 name/namespace/version
     * 验证内容：读回一致
     */
    @Test
    public void testGetRequestPojo() {
        // Arrange
        SkillGetRequest getRequest = new SkillGetRequest();
        getRequest.setName("sql-analysis");
        getRequest.setNamespace("default");
        getRequest.setVersion("1.0.0");

        // Act & Assert
        assertThat(getRequest.getName()).isEqualTo("sql-analysis");
        assertThat(getRequest.getNamespace()).isEqualTo("default");
        assertThat(getRequest.getVersion()).isEqualTo("1.0.0");
    }

    /**
     * 测试目的：ListSkills 请求字段
     * 测试场景：填充分页与过滤
     * 验证内容：读回一致
     */
    @Test
    public void testListRequestPojo() {
        // Arrange
        SkillListRequest listRequest = new SkillListRequest();
        listRequest.setNamespace("default");
        listRequest.setName("sql");
        listRequest.setTag("db");
        listRequest.setOwner("alice");
        listRequest.setScope("public");
        listRequest.setKeyword("sql");
        listRequest.setOrderBy("create_time");
        listRequest.setOrderType("desc");
        listRequest.setOffset(10);
        listRequest.setLimit(20);

        // Act & Assert
        assertThat(listRequest.getNamespace()).isEqualTo("default");
        assertThat(listRequest.getName()).isEqualTo("sql");
        assertThat(listRequest.getTag()).isEqualTo("db");
        assertThat(listRequest.getOwner()).isEqualTo("alice");
        assertThat(listRequest.getScope()).isEqualTo("public");
        assertThat(listRequest.getKeyword()).isEqualTo("sql");
        assertThat(listRequest.getOrderBy()).isEqualTo("create_time");
        assertThat(listRequest.getOrderType()).isEqualTo("desc");
        assertThat(listRequest.getOffset()).isEqualTo(10);
        assertThat(listRequest.getLimit()).isEqualTo(20);
    }

    /**
     * 测试目的：DownloadSkill 请求字段
     * 测试场景：zip 格式
     * 验证内容：读回一致
     */
    @Test
    public void testDownloadRequestPojo() {
        // Arrange
        SkillDownloadRequest downloadRequest = new SkillDownloadRequest();
        downloadRequest.setName("sql-analysis");
        downloadRequest.setNamespace("default");
        downloadRequest.setVersion("1.0.0");
        downloadRequest.setTag("db");
        downloadRequest.setFormat("zip");

        // Act & Assert
        assertThat(downloadRequest.getName()).isEqualTo("sql-analysis");
        assertThat(downloadRequest.getNamespace()).isEqualTo("default");
        assertThat(downloadRequest.getVersion()).isEqualTo("1.0.0");
        assertThat(downloadRequest.getTag()).isEqualTo("db");
        assertThat(downloadRequest.getFormat()).isEqualTo("zip");
    }

    /**
     * 测试目的：storage 与 versionInfo
     * 测试场景：嵌套对象
     * 验证内容：字段读回一致
     */
    @Test
    public void testStorageAndVersionInfoPojo() {
        // Arrange
        SkillStorageInfo storage = new SkillStorageInfo();
        storage.setProvider("cos");
        storage.setFiles(Collections.singletonList("SKILL.md"));
        storage.setScope("ns");
        storage.setContentDigest("abc");
        SkillVersionInfo versionInfo = new SkillVersionInfo();
        versionInfo.setActiveVersion("1.0.0");
        versionInfo.setLatestVersion("1.1.0");
        versionInfo.setTotalVersions(2);

        // Act & Assert
        assertThat(storage.getProvider()).isEqualTo("cos");
        assertThat(storage.getFiles()).containsExactly("SKILL.md");
        assertThat(storage.getScope()).isEqualTo("ns");
        assertThat(storage.getContentDigest()).isEqualTo("abc");
        assertThat(versionInfo.getActiveVersion()).isEqualTo("1.0.0");
        assertThat(versionInfo.getLatestVersion()).isEqualTo("1.1.0");
        assertThat(versionInfo.getTotalVersions()).isEqualTo(2);
    }

    /**
     * 测试目的：SkillResourceVersion 字段
     * 测试场景：含 storage
     * 验证内容：读回一致
     */
    @Test
    public void testResourceVersionPojo() {
        // Arrange
        SkillStorageInfo storage = new SkillStorageInfo();
        storage.setProvider("cos");
        SkillResourceVersion resourceVersion = new SkillResourceVersion();
        resourceVersion.setId(9L);
        resourceVersion.setName("sql-analysis");
        resourceVersion.setNamespace("default");
        resourceVersion.setVersion("1.0.0");
        resourceVersion.setAuthor("alice");
        resourceVersion.setDescription("desc");
        resourceVersion.setStatus("published");
        resourceVersion.setStorage(storage);
        resourceVersion.setDownloadCount(3L);
        resourceVersion.setCreateTime("t1");
        resourceVersion.setModifyTime("t2");

        // Act & Assert
        assertThat(resourceVersion.getId()).isEqualTo(9L);
        assertThat(resourceVersion.getName()).isEqualTo("sql-analysis");
        assertThat(resourceVersion.getNamespace()).isEqualTo("default");
        assertThat(resourceVersion.getVersion()).isEqualTo("1.0.0");
        assertThat(resourceVersion.getAuthor()).isEqualTo("alice");
        assertThat(resourceVersion.getDescription()).isEqualTo("desc");
        assertThat(resourceVersion.getStatus()).isEqualTo("published");
        assertThat(resourceVersion.getStorage()).isSameAs(storage);
        assertThat(resourceVersion.getDownloadCount()).isEqualTo(3L);
        assertThat(resourceVersion.getCreateTime()).isEqualTo("t1");
        assertThat(resourceVersion.getModifyTime()).isEqualTo("t2");
    }

    /**
     * 测试目的：SkillResource 身份字段
     * 测试场景：id/name/namespace/status
     * 验证内容：读回一致
     */
    @Test
    public void testResourceIdentityPojo() {
        // Arrange
        SkillResource resource = new SkillResource();
        resource.setId(1L);
        resource.setName("sql-analysis");
        resource.setNamespace("default");
        resource.setDescription("desc");
        resource.setStatus("published");
        resource.setTags(Collections.singletonList("db"));
        resource.setExamples(Collections.singletonList("ex"));
        resource.setExt(Collections.singletonMap("k", "v"));

        // Act & Assert
        assertThat(resource.getId()).isEqualTo(1L);
        assertThat(resource.getName()).isEqualTo("sql-analysis");
        assertThat(resource.getNamespace()).isEqualTo("default");
        assertThat(resource.getDescription()).isEqualTo("desc");
        assertThat(resource.getStatus()).isEqualTo("published");
        assertThat(resource.getTags()).containsExactly("db");
        assertThat(resource.getExamples()).containsExactly("ex");
        assertThat(resource.getExt()).containsEntry("k", "v");
    }

    /**
     * 测试目的：SkillResource 统计字段
     * 测试场景：source/owner/时间戳
     * 验证内容：读回一致
     */
    @Test
    public void testResourceMetaPojo() {
        // Arrange
        SkillVersionInfo versionInfo = new SkillVersionInfo();
        SkillResource resource = new SkillResource();
        resource.setSource("polaris");
        resource.setVersionInfo(versionInfo);
        resource.setMetaVersion(8L);
        resource.setScope("public");
        resource.setOwner("alice");
        resource.setDownloadCount(4L);
        resource.setCreateTime("t1");
        resource.setModifyTime("t2");

        // Act & Assert
        assertThat(resource.getSource()).isEqualTo("polaris");
        assertThat(resource.getVersionInfo()).isSameAs(versionInfo);
        assertThat(resource.getMetaVersion()).isEqualTo(8L);
        assertThat(resource.getScope()).isEqualTo("public");
        assertThat(resource.getOwner()).isEqualTo("alice");
        assertThat(resource.getDownloadCount()).isEqualTo(4L);
        assertThat(resource.getCreateTime()).isEqualTo("t1");
        assertThat(resource.getModifyTime()).isEqualTo("t2");
    }

    /**
     * 测试目的：GetSkill 响应字段
     * 测试场景：code/info/嵌套对象
     * 验证内容：读回一致
     */
    @Test
    public void testGetResponsePojo() {
        // Arrange
        SkillResource resource = new SkillResource();
        SkillResourceVersion resourceVersion = new SkillResourceVersion();
        SkillGetResponse getResponse = new SkillGetResponse();
        getResponse.setCode(200000);
        getResponse.setInfo("ok");
        getResponse.setResource(resource);
        getResponse.setResourceVersion(resourceVersion);
        getResponse.setContent("# skill");

        // Act & Assert
        assertThat(getResponse.getCode()).isEqualTo(200000);
        assertThat(getResponse.getInfo()).isEqualTo("ok");
        assertThat(getResponse.getResource()).isSameAs(resource);
        assertThat(getResponse.getResourceVersion()).isSameAs(resourceVersion);
        assertThat(getResponse.getContent()).isEqualTo("# skill");
    }

    /**
     * 测试目的：ListSkills 响应字段
     * 测试场景：列表与 total
     * 验证内容：读回一致
     */
    @Test
    public void testListResponsePojo() {
        // Arrange
        SkillResource resource = new SkillResource();
        SkillListResponse listResponse = new SkillListResponse();
        listResponse.setCode(200000);
        listResponse.setInfo("ok");
        listResponse.setTotal(1);
        listResponse.setResources(Collections.singletonList(resource));

        // Act & Assert
        assertThat(listResponse.getCode()).isEqualTo(200000);
        assertThat(listResponse.getInfo()).isEqualTo("ok");
        assertThat(listResponse.getTotal()).isEqualTo(1);
        assertThat(listResponse.getResources()).hasSize(1);
    }

    /**
     * 测试目的：DownloadSkill 响应字段
     * 测试场景：zip 字节
     * 验证内容：读回一致
     */
    @Test
    public void testDownloadResponsePojo() {
        // Arrange
        SkillDownloadResponse downloadResponse = new SkillDownloadResponse();
        downloadResponse.setCode(200000);
        downloadResponse.setInfo("ok");
        downloadResponse.setVersion("1.0.0");
        downloadResponse.setContentType("application/zip");
        downloadResponse.setContent("# md");
        downloadResponse.setZipContent("zip".getBytes(StandardCharsets.UTF_8));
        downloadResponse.setTotalSize(3L);
        downloadResponse.setFilename("a.zip");

        // Act & Assert
        assertThat(downloadResponse.getCode()).isEqualTo(200000);
        assertThat(downloadResponse.getInfo()).isEqualTo("ok");
        assertThat(downloadResponse.getVersion()).isEqualTo("1.0.0");
        assertThat(downloadResponse.getContentType()).isEqualTo("application/zip");
        assertThat(downloadResponse.getContent()).isEqualTo("# md");
        assertThat(downloadResponse.getZipContent()).isEqualTo("zip".getBytes(StandardCharsets.UTF_8));
        assertThat(downloadResponse.getTotalSize()).isEqualTo(3L);
        assertThat(downloadResponse.getFilename()).isEqualTo("a.zip");
    }
}
