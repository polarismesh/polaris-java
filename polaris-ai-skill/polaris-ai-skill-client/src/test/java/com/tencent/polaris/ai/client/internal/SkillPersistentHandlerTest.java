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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link SkillPersistentHandler}.
 *
 * @author Fishtail Fu
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

    /**
     * 测试目的：markdown 下载只写 yaml 不写 zip
     * 测试场景：format=markdown
     * 验证内容：content 可读回，zipContent 为空
     */
    @Test
    public void testSaveDownloadMarkdownRoundTrip() throws IOException {
        // Arrange
        SkillPersistentHandler handler = newHandler(true);
        SkillDownloadResponse response = new SkillDownloadResponse();
        response.setVersion("1.0.0");
        response.setContent("# skill");
        response.setFilename("SKILL.md");

        // Act
        handler.saveDownload("default", "sql-analysis", "1.0.0", "markdown", response);
        SkillDownloadResponse loaded = handler.loadDownload("default", "sql-analysis", "1.0.0", "markdown");

        // Assert
        assertThat(loaded.getContent()).isEqualTo("# skill");
        assertThat(loaded.getFilename()).isEqualTo("SKILL.md");
        assertThat(loaded.getZipContent()).isNull();
    }

    /**
     * 测试目的：异步写 GetSkill 最终落盘
     * 测试场景：asyncSaveGetSkill + asyncSaveActiveVersion
     * 验证内容：load 读回 content 与 active 指针
     */
    @Test
    public void testAsyncSaveGetSkillAndActiveVersion() throws IOException {
        // Arrange
        SkillPersistentHandler handler = newHandler(true);
        SkillGetResponse response = new SkillGetResponse();
        response.setContent("async-skill");

        // Act
        handler.asyncSaveGetSkill("default", "sql-analysis", "1.2.0", response);
        handler.asyncSaveActiveVersion("default", "sql-analysis", "1.2.0");
        long deadline = System.currentTimeMillis() + 2000L;
        SkillGetResponse loaded = handler.loadGetSkill("default", "sql-analysis", "1.2.0");
        String activeVersion = handler.loadActiveVersion("default", "sql-analysis");
        while (loaded == null && System.currentTimeMillis() < deadline) {
            loaded = handler.loadGetSkill("default", "sql-analysis", "1.2.0");
        }
        while (!"1.2.0".equals(activeVersion) && System.currentTimeMillis() < deadline) {
            activeVersion = handler.loadActiveVersion("default", "sql-analysis");
        }

        // Assert
        assertThat(loaded.getContent()).isEqualTo("async-skill");
        assertThat(activeVersion).isEqualTo("1.2.0");
        handler.doDestroy();
    }

    /**
     * 测试目的：缺失文件返回 null
     * 测试场景：从未写入
     * 验证内容：get/download/active 均为 null
     */
    @Test
    public void testLoadMissingFilesReturnNull() throws IOException {
        // Arrange
        SkillPersistentHandler handler = newHandler(true);

        // Act & Assert
        assertThat(handler.loadGetSkill("default", "missing", "1.0.0")).isNull();
        assertThat(handler.loadDownload("default", "missing", "1.0.0", "zip")).isNull();
        assertThat(handler.loadActiveVersion("default", "missing")).isNull();
    }

    /**
     * 测试目的：关闭 persist 后异步写也不落盘
     * 测试场景：persistEnable=false 调用 asyncSave
     * 验证内容：load 为 null
     */
    @Test
    public void testAsyncSaveSkippedWhenPersistDisabled() throws IOException {
        // Arrange
        SkillPersistentHandler handler = newHandler(false);
        SkillGetResponse response = new SkillGetResponse();
        response.setContent("no-disk");
        SkillDownloadResponse download = new SkillDownloadResponse();
        download.setContent("# md");

        // Act
        handler.asyncSaveGetSkill("default", "sql-analysis", "1.0.0", response);
        handler.asyncSaveDownload("default", "sql-analysis", "1.0.0", "markdown", download);
        handler.asyncSaveActiveVersion("default", "sql-analysis", "1.0.0");

        // Assert
        assertThat(handler.loadGetSkill("default", "sql-analysis", "1.0.0")).isNull();
        assertThat(handler.loadDownload("default", "sql-analysis", "1.0.0", "markdown")).isNull();
        assertThat(handler.loadActiveVersion("default", "sql-analysis")).isNull();
    }

    /**
     * 测试目的：空白 active 指针视为缺失
     * 测试场景：active 文件内容为空
     * 验证内容：loadActiveVersion 返回 null
     */
    @Test
    public void testLoadBlankActiveVersionReturnsNull() throws IOException {
        // Arrange
        SkillPersistentHandler handler = newHandler(true);

        // Act
        handler.saveActiveVersion("default", "sql-analysis", "   ");

        // Assert
        assertThat(handler.loadActiveVersion("default", "sql-analysis")).isNull();
    }

    /**
     * 测试目的：关闭 persist 后同步写也不落盘
     * 测试场景：persistEnable=false 调用 save*
     * 验证内容：load 为 null
     */
    @Test
    public void testSyncSaveSkippedWhenPersistDisabled() throws IOException {
        // Arrange
        SkillPersistentHandler handler = newHandler(false);
        SkillGetResponse response = new SkillGetResponse();
        response.setContent("no-disk");
        SkillDownloadResponse download = new SkillDownloadResponse();
        download.setContent("# md");

        // Act
        handler.saveGetSkill("default", "sql-analysis", "1.0.0", response);
        handler.saveDownload("default", "sql-analysis", "1.0.0", "markdown", download);
        handler.saveActiveVersion("default", "sql-analysis", "1.0.0");

        // Assert
        assertThat(handler.loadGetSkill("default", "sql-analysis", "1.0.0")).isNull();
        assertThat(handler.loadDownload("default", "sql-analysis", "1.0.0", "markdown")).isNull();
        assertThat(handler.loadActiveVersion("default", "sql-analysis")).isNull();
    }

    /**
     * 测试目的：损坏 yaml 读失败走重试后返回 null
     * 测试场景：get yaml 内容非法，maxReadRetry=1
     * 验证内容：loadGetSkill 为 null
     */
    @Test
    public void testLoadCorruptYamlReturnsNullAfterRetry() throws IOException {
        // Arrange
        File persistDir = temporaryFolder.newFolder();
        SkillPersistentHandler handler = new SkillPersistentHandler(
                persistDir.getAbsolutePath(), true, 1, 1, 1L);
        SkillGetResponse response = new SkillGetResponse();
        response.setContent("ok");
        handler.saveGetSkill("default", "sql-analysis", "1.0.0", response);
        File getDir = new File(persistDir, "get");
        File[] files = getDir.listFiles();
        assertThat(files).isNotEmpty();
        Files.write(files[0].toPath(), ":::not-yaml".getBytes(StandardCharsets.UTF_8));

        // Act
        SkillGetResponse loaded = handler.loadGetSkill("default", "sql-analysis", "1.0.0");

        // Assert
        assertThat(loaded).isNull();
    }

    /**
     * 测试目的：写 yaml 父路径冲突时吞异常并重试
     * 测试场景：get 目录被占成文件
     * 验证内容：不抛异常
     */
    @Test
    public void testWriteYamlFailureDoesNotThrow() throws IOException {
        // Arrange
        File persistDir = temporaryFolder.newFolder();
        File getAsFile = new File(persistDir, "get");
        assertThat(getAsFile.createNewFile()).isTrue();
        SkillPersistentHandler handler = new SkillPersistentHandler(
                persistDir.getAbsolutePath(), true, 1, 0, 1L);
        SkillGetResponse response = new SkillGetResponse();
        response.setContent("ok");

        // Act
        handler.saveGetSkill("default", "sql-analysis", "1.0.0", response);

        // Assert
        assertThat(handler.loadGetSkill("default", "sql-analysis", "1.0.0")).isNull();
    }

    /**
     * 测试目的：zip 字节写失败与读失败被吞掉
     * 测试场景：download 目录被占成文件；yaml 存在但 zip 路径是目录
     * 验证内容：不抛异常且 zipContent 为 null
     */
    @Test
    public void testZipWriteAndReadFailuresAreSwallowed() throws IOException {
        // Arrange
        File persistDir = temporaryFolder.newFolder();
        File downloadAsFile = new File(persistDir, "download");
        assertThat(downloadAsFile.createNewFile()).isTrue();
        SkillPersistentHandler writeHandler = new SkillPersistentHandler(
                persistDir.getAbsolutePath(), true, 1, 0, 1L);
        SkillDownloadResponse download = new SkillDownloadResponse();
        download.setFilename("a.zip");
        download.setZipContent("zip".getBytes(StandardCharsets.UTF_8));
        writeHandler.saveDownload("default", "sql-analysis", "1.0.0", "zip", download);

        File persistDir2 = temporaryFolder.newFolder();
        SkillPersistentHandler readHandler = new SkillPersistentHandler(
                persistDir2.getAbsolutePath(), true, 1, 0, 1L);
        SkillDownloadResponse meta = new SkillDownloadResponse();
        meta.setFilename("a.zip");
        meta.setZipContent("zip".getBytes(StandardCharsets.UTF_8));
        readHandler.saveDownload("default", "sql-analysis", "1.0.0", "zip", meta);
        File downloadDir = new File(persistDir2, "download");
        File[] zipFiles = downloadDir.listFiles((dir, name) -> name.endsWith(".zip"));
        assertThat(zipFiles).isNotEmpty();
        assertThat(zipFiles[0].delete()).isTrue();
        assertThat(zipFiles[0].mkdir()).isTrue();

        // Act
        SkillDownloadResponse loaded = readHandler.loadDownload("default", "sql-analysis", "1.0.0", "zip");

        // Assert
        assertThat(loaded).isNotNull();
        assertThat(loaded.getZipContent()).isNull();
    }

    /**
     * 测试目的：active 指针读失败被吞掉
     * 测试场景：active 文件路径实际是目录
     * 验证内容：loadActiveVersion 为 null
     */
    @Test
    public void testLoadActiveVersionWhenPathIsDirectory() throws IOException {
        // Arrange
        File persistDir = temporaryFolder.newFolder();
        SkillPersistentHandler handler = new SkillPersistentHandler(
                persistDir.getAbsolutePath(), true, 1, 0, 1L);
        handler.saveActiveVersion("default", "sql-analysis", "1.0.0");
        File activeDir = new File(persistDir, "active");
        File[] files = activeDir.listFiles();
        assertThat(files).isNotEmpty();
        assertThat(files[0].delete()).isTrue();
        assertThat(files[0].mkdir()).isTrue();

        // Act
        String version = handler.loadActiveVersion("default", "sql-analysis");

        // Assert
        assertThat(version).isNull();
    }

    /**
     * 测试目的：active 写失败被吞掉
     * 测试场景：active 目录被占成文件
     * 验证内容：load 为 null
     */
    @Test
    public void testWriteActiveFailureDoesNotThrow() throws IOException {
        // Arrange
        File persistDir = temporaryFolder.newFolder();
        File activeAsFile = new File(persistDir, "active");
        assertThat(activeAsFile.createNewFile()).isTrue();
        SkillPersistentHandler handler = new SkillPersistentHandler(
                persistDir.getAbsolutePath(), true, 1, 0, 1L);

        // Act
        handler.saveActiveVersion("default", "sql-analysis", "1.0.0");

        // Assert
        assertThat(handler.loadActiveVersion("default", "sql-analysis")).isNull();
    }

    /**
     * 测试目的：缺失文件走读重试
     * 测试场景：maxReadRetry=1
     * 验证内容：仍返回 null
     */
    @Test
    public void testLoadMissingFileRetriesThenReturnsNull() throws IOException {
        // Arrange
        File persistDir = temporaryFolder.newFolder();
        SkillPersistentHandler handler = new SkillPersistentHandler(
                persistDir.getAbsolutePath(), true, 1, 1, 1L);

        // Act
        SkillGetResponse loaded = handler.loadGetSkill("default", "missing", "1.0.0");

        // Assert
        assertThat(loaded).isNull();
    }

    private SkillPersistentHandler newHandler(boolean persistEnable) throws IOException {
        return new SkillPersistentHandler(temporaryFolder.newFolder().getAbsolutePath(), persistEnable, 1, 0, 10L);
    }
}
