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

package com.tencent.polaris.ai.client.internal;

import com.tencent.polaris.api.plugin.skill.SkillDownloadResponse;
import com.tencent.polaris.api.plugin.skill.SkillGetResponse;
import com.tencent.polaris.api.plugin.skill.SkillResourceVersion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link SkillPersistentHandler}.
 *
 * @author polaris
 */
@RunWith(MockitoJUnitRunner.class)
public class SkillPersistentHandlerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    /**
     * 测试不同 version 落盘互不覆盖
     * 测试目的：同一 skill 的多个 version 各自写文件
     * 测试场景：先后保存 1.0.0 与 1.1.0
     * 验证内容：分别 load 得到对应 content
     */
    @Test
    public void testSaveGetSkillKeepsMultipleVersions() throws IOException {
        // Arrange
        SkillPersistentHandler handler = newHandler(true);

        SkillGetResponse versionOne = new SkillGetResponse();
        versionOne.setContent("skill-v1");
        SkillResourceVersion resourceVersionOne = new SkillResourceVersion();
        resourceVersionOne.setVersion("1.0.0");
        versionOne.setResourceVersion(resourceVersionOne);

        SkillGetResponse versionTwo = new SkillGetResponse();
        versionTwo.setContent("skill-v2");
        SkillResourceVersion resourceVersionTwo = new SkillResourceVersion();
        resourceVersionTwo.setVersion("1.1.0");
        versionTwo.setResourceVersion(resourceVersionTwo);

        // Act
        handler.saveGetSkill("default", "sql-analysis", "1.0.0", versionOne);
        handler.saveGetSkill("default", "sql-analysis", "1.1.0", versionTwo);

        // Assert
        assertThat(handler.loadGetSkill("default", "sql-analysis", "1.0.0").getContent()).isEqualTo("skill-v1");
        assertThat(handler.loadGetSkill("default", "sql-analysis", "1.1.0").getContent()).isEqualTo("skill-v2");
    }

    /**
     * 测试 active 指针
     * 测试目的：空 version 回退时能找到最近一次 activeVersion
     * 测试场景：保存 1.1.0 并写入 active 指针
     * 验证内容：loadActiveVersion 返回 1.1.0
     */
    @Test
    public void testSaveActiveVersionPointer() throws IOException {
        // Arrange
        SkillPersistentHandler handler = newHandler(true);

        // Act
        handler.saveActiveVersion("default", "sql-analysis", "1.1.0");

        // Assert
        assertThat(handler.loadActiveVersion("default", "sql-analysis")).isEqualTo("1.1.0");
    }

    /**
     * 测试 zip 字节与元数据分开落盘
     * 测试目的：zip 内容可完整读回
     * 测试场景：保存 download zip
     * 验证内容：zipContent 与 filename 一致
     */
    @Test
    public void testSaveDownloadZipRoundTrip() throws IOException {
        // Arrange
        SkillPersistentHandler handler = newHandler(true);
        SkillDownloadResponse response = new SkillDownloadResponse();
        response.setVersion("1.0.0");
        response.setFilename("sql-analysis-1.0.0.zip");
        response.setZipContent("zip-bytes".getBytes(StandardCharsets.UTF_8));

        // Act
        handler.saveDownload("default", "sql-analysis", "1.0.0", "zip", response);
        SkillDownloadResponse loaded = handler.loadDownload("default", "sql-analysis", "1.0.0", "zip");

        // Assert
        assertThat(loaded.getFilename()).isEqualTo("sql-analysis-1.0.0.zip");
        assertThat(loaded.getZipContent()).isEqualTo("zip-bytes".getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 测试关闭 persist 后不落盘
     * 测试目的：persistEnable=false 时不写文件
     * 测试场景：保存后 load
     * 验证内容：load 返回 null
     */
    @Test
    public void testPersistDisableSkipsDisk() throws IOException {
        // Arrange
        SkillPersistentHandler handler = newHandler(false);
        SkillGetResponse response = new SkillGetResponse();
        response.setContent("skill-v1");

        // Act
        handler.saveGetSkill("default", "sql-analysis", "1.0.0", response);

        // Assert
        assertThat(handler.loadGetSkill("default", "sql-analysis", "1.0.0")).isNull();
    }

    private SkillPersistentHandler newHandler(boolean persistEnable) throws IOException {
        return new SkillPersistentHandler(temporaryFolder.newFolder().getAbsolutePath(), persistEnable, 1, 0, 10L);
    }
}
