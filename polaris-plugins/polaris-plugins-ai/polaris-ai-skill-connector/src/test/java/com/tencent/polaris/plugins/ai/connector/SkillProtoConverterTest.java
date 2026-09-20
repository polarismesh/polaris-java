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

package com.tencent.polaris.plugins.ai.connector;

import com.google.protobuf.ByteString;
import com.tencent.polaris.api.exception.ServerCodes;
import com.tencent.polaris.api.plugin.skill.SkillDownloadRequest;
import com.tencent.polaris.api.plugin.skill.SkillDownloadResponse;
import com.tencent.polaris.api.plugin.skill.SkillGetRequest;
import com.tencent.polaris.api.plugin.skill.SkillGetResponse;
import com.tencent.polaris.api.plugin.skill.SkillListRequest;
import com.tencent.polaris.api.plugin.skill.SkillListResponse;
import com.tencent.polaris.api.plugin.skill.SkillResource;
import com.tencent.polaris.specification.api.v1.skill.manage.PolarisSkillGRPCService;
import com.tencent.polaris.specification.api.v1.skill.manage.SkillProto;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link SkillProtoConverter}.
 *
 * @author Fishtail Fu
 */
@RunWith(MockitoJUnitRunner.class)
public class SkillProtoConverterTest {

    /**
     * 测试 GetSkill 请求转换
     * 测试目的：POJO 字段写入 protobuf
     * 测试场景：name/namespace/version 均有值
     * 验证内容：proto 字段一致
     */
    @Test
    public void testToGetRequest() {
        // Arrange
        SkillGetRequest request = new SkillGetRequest();
        request.setName("sql-analysis");
        request.setNamespace("default");
        request.setVersion("1.0.0");

        // Act
        PolarisSkillGRPCService.GetSkillRequest proto = SkillProtoConverter.toGetRequest(request);

        // Assert
        assertThat(proto.getName()).isEqualTo("sql-analysis");
        assertThat(proto.getNamespace()).isEqualTo("default");
        assertThat(proto.getVersion()).isEqualTo("1.0.0");
    }

    /**
     * 测试 GetSkill 响应转换
     * 测试目的：protobuf 转回 POJO
     * 测试场景：包含 content 与 version
     * 验证内容：content 与 version 一致
     */
    @Test
    public void testToGetResponse() {
        // Arrange
        PolarisSkillGRPCService.GetSkillResponse proto = PolarisSkillGRPCService.GetSkillResponse.newBuilder()
                .setCode(ServerCodes.EXECUTE_SUCCESS)
                .setContent("# Skill")
                .setVersion(SkillProto.SkillResourceVersion.newBuilder().setVersion("1.0.0").build())
                .build();

        // Act
        SkillGetResponse response = SkillProtoConverter.toGetResponse(proto);

        // Assert
        assertThat(response.getCode()).isEqualTo(ServerCodes.EXECUTE_SUCCESS);
        assertThat(response.getContent()).isEqualTo("# Skill");
        assertThat(response.getResourceVersion().getVersion()).isEqualTo("1.0.0");
    }

    /**
     * 测试 zip 分片组装
     * 测试目的：多帧 zip_chunk 拼成完整字节
     * 测试场景：两帧，第一帧带 filename
     * 验证内容：zipContent 为拼接结果
     */
    @Test
    public void testAssembleDownloadConcatenatesZipChunks() {
        // Arrange
        PolarisSkillGRPCService.DownloadSkillResponse first = PolarisSkillGRPCService.DownloadSkillResponse.newBuilder()
                .setCode(ServerCodes.EXECUTE_SUCCESS)
                .setVersion("1.0.0")
                .setFilename("sql-analysis-1.0.0.zip")
                .setZipChunk(ByteString.copyFrom("hel".getBytes(StandardCharsets.UTF_8)))
                .build();
        PolarisSkillGRPCService.DownloadSkillResponse second = PolarisSkillGRPCService.DownloadSkillResponse.newBuilder()
                .setZipChunk(ByteString.copyFrom("lo".getBytes(StandardCharsets.UTF_8)))
                .build();

        // Act
        SkillDownloadResponse response = SkillProtoConverter.assembleDownload(Arrays.asList(first, second).iterator());

        // Assert
        assertThat(response.getFilename()).isEqualTo("sql-analysis-1.0.0.zip");
        assertThat(response.getZipContent()).isEqualTo("hello".getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 测试 Download 请求 format
     * 测试目的：format 透传到 protobuf
     * 测试场景：format=markdown
     * 验证内容：proto.format 为 markdown
     */
    @Test
    public void testToDownloadRequestKeepsFormat() {
        // Arrange
        SkillDownloadRequest request = new SkillDownloadRequest();
        request.setName("sql-analysis");
        request.setNamespace("default");
        request.setFormat("markdown");

        // Act
        PolarisSkillGRPCService.DownloadSkillRequest proto = SkillProtoConverter.toDownloadRequest(request);

        // Assert
        assertThat(proto.getFormat()).isEqualTo("markdown");
    }

    /**
     * 测试后续帧错误码会中断拼接
     * 测试目的：任一流帧非成功时不再 append zip，并保留错误码
     * 测试场景：首帧成功写入 chunk，第二帧返回业务错误码并带脏 chunk
     * 验证内容：code/info 来自错误帧，zipContent 不含错误帧字节
     */
    @Test
    public void testAssembleDownloadAbortsOnLaterFrameError() {
        // Arrange
        PolarisSkillGRPCService.DownloadSkillResponse first = PolarisSkillGRPCService.DownloadSkillResponse.newBuilder()
                .setCode(ServerCodes.EXECUTE_SUCCESS)
                .setInfo("ok")
                .setVersion("1.0.0")
                .setZipChunk(ByteString.copyFrom("hel".getBytes(StandardCharsets.UTF_8)))
                .build();
        PolarisSkillGRPCService.DownloadSkillResponse second = PolarisSkillGRPCService.DownloadSkillResponse.newBuilder()
                .setCode(500000)
                .setInfo("stream broken")
                .setZipChunk(ByteString.copyFrom("lo".getBytes(StandardCharsets.UTF_8)))
                .build();
        PolarisSkillGRPCService.DownloadSkillResponse third = PolarisSkillGRPCService.DownloadSkillResponse.newBuilder()
                .setZipChunk(ByteString.copyFrom("xx".getBytes(StandardCharsets.UTF_8)))
                .build();

        // Act
        SkillDownloadResponse response = SkillProtoConverter.assembleDownload(
                Arrays.asList(first, second, third).iterator());

        // Assert
        assertThat(response.getCode()).isEqualTo(500000);
        assertThat(response.getInfo()).isEqualTo("stream broken");
        assertThat(response.getZipContent()).isEqualTo("hel".getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 测试目的：List 请求全字段转换
     * 测试场景：过滤与分页均有值
     * 验证内容：proto 字段一致
     */
    @Test
    public void testToListRequestFillsFilters() {
        // Arrange
        SkillListRequest request = new SkillListRequest();
        request.setNamespace("default");
        request.setName("sql");
        request.setTag("db");
        request.setOwner("alice");
        request.setScope("public");
        request.setKeyword("kw");
        request.setOrderBy("name");
        request.setOrderType("asc");
        request.setOffset(2);
        request.setLimit(8);

        // Act
        PolarisSkillGRPCService.ListSkillsRequest proto = SkillProtoConverter.toListRequest(request);

        // Assert
        assertThat(proto.getNamespace()).isEqualTo("default");
        assertThat(proto.getName()).isEqualTo("sql");
        assertThat(proto.getTag()).isEqualTo("db");
        assertThat(proto.getOwner()).isEqualTo("alice");
        assertThat(proto.getScope()).isEqualTo("public");
        assertThat(proto.getKeyword()).isEqualTo("kw");
        assertThat(proto.getOrderBy()).isEqualTo("name");
        assertThat(proto.getOrderType()).isEqualTo("asc");
        assertThat(proto.getOffset()).isEqualTo(2);
        assertThat(proto.getLimit()).isEqualTo(8);
    }

    /**
     * 测试目的：List 响应映射资源与 digest
     * 测试场景：一条 SkillResource 带 versionInfo 与 storage
     * 验证内容：name/digest/activeVersion 一致
     */
    @Test
    public void testToListResponseMapsResource() {
        // Arrange
        SkillProto.SkillResource protoResource = SkillProto.SkillResource.newBuilder()
                .setId(11L)
                .setName("sql-analysis")
                .setNamespace("default")
                .setDescription("desc")
                .setStatus("published")
                .addTags("db")
                .addExamples("ex")
                .putExt("k", "v")
                .setSource("polaris")
                .setMetaVersion(3L)
                .setScope("public")
                .setOwner("alice")
                .setDownloadCount(9L)
                .setCreateTime("c")
                .setModifyTime("m")
                .setVersionInfo(SkillProto.VersionInfo.newBuilder()
                        .setActiveVersion("1.0.0")
                        .setLatestVersion("1.1.0")
                        .setTotalVersions(2)
                        .build())
                .build();
        PolarisSkillGRPCService.ListSkillsResponse proto = PolarisSkillGRPCService.ListSkillsResponse.newBuilder()
                .setCode(ServerCodes.EXECUTE_SUCCESS)
                .setInfo("ok")
                .setTotal(1)
                .addResources(protoResource)
                .build();

        // Act
        SkillListResponse response = SkillProtoConverter.toListResponse(proto);
        SkillResource resource = response.getResources().get(0);

        // Assert
        assertThat(response.getTotal()).isEqualTo(1);
        assertThat(resource.getName()).isEqualTo("sql-analysis");
        assertThat(resource.getTags()).containsExactly("db");
        assertThat(resource.getVersionInfo().getActiveVersion()).isEqualTo("1.0.0");
    }

    /**
     * 测试目的：GetSkill 映射 storage digest
     * 测试场景：version.storage.contentDigest 有值
     * 验证内容：POJO storage digest 一致
     */
    @Test
    public void testToGetResponseMapsStorageDigest() {
        // Arrange
        PolarisSkillGRPCService.GetSkillResponse proto = PolarisSkillGRPCService.GetSkillResponse.newBuilder()
                .setCode(ServerCodes.EXECUTE_SUCCESS)
                .setInfo("ok")
                .setContent("# Skill")
                .setVersion(SkillProto.SkillResourceVersion.newBuilder()
                        .setId(1L)
                        .setName("sql-analysis")
                        .setNamespace("default")
                        .setVersion("1.0.0")
                        .setAuthor("alice")
                        .setDescription("d")
                        .setStatus("ok")
                        .setDownloadCount(1L)
                        .setCreateTime("c")
                        .setModifyTime("m")
                        .setStorage(SkillProto.StorageInfo.newBuilder()
                                .setProvider("cos")
                                .addFiles("SKILL.md")
                                .setScope("ns")
                                .setContentDigest("digest-1")
                                .build())
                        .build())
                .build();

        // Act
        SkillGetResponse response = SkillProtoConverter.toGetResponse(proto);

        // Assert
        assertThat(response.getResourceVersion().getStorage().getContentDigest()).isEqualTo("digest-1");
        assertThat(response.getResourceVersion().getAuthor()).isEqualTo("alice");
    }

    /**
     * 测试目的：Download 请求空白字段不写入 proto
     * 测试场景：仅 name
     * 验证内容：namespace/version 为空
     */
    @Test
    public void testToDownloadRequestSkipsBlankFields() {
        // Arrange
        SkillDownloadRequest request = new SkillDownloadRequest();
        request.setName("sql-analysis");

        // Act
        PolarisSkillGRPCService.DownloadSkillRequest proto = SkillProtoConverter.toDownloadRequest(request);

        // Assert
        assertThat(proto.getName()).isEqualTo("sql-analysis");
        assertThat(proto.getNamespace()).isEmpty();
        assertThat(proto.getVersion()).isEmpty();
        assertThat(proto.getTag()).isEmpty();
        assertThat(proto.getFormat()).isEmpty();
    }

    /**
     * 测试目的：markdown 内容写入 content
     * 测试场景：单帧 content 无 zip
     * 验证内容：content 有值，zipContent 为空
     */
    @Test
    public void testAssembleDownloadMarkdownContent() {
        // Arrange
        PolarisSkillGRPCService.DownloadSkillResponse frame = PolarisSkillGRPCService.DownloadSkillResponse.newBuilder()
                .setCode(ServerCodes.EXECUTE_SUCCESS)
                .setContent("# hello")
                .setContentType("text/markdown")
                .setTotalSize(7L)
                .build();

        // Act
        SkillDownloadResponse response = SkillProtoConverter.assembleDownload(Arrays.asList(frame).iterator());

        // Assert
        assertThat(response.getContent()).isEqualTo("# hello");
        assertThat(response.getContentType()).isEqualTo("text/markdown");
        assertThat(response.getTotalSize()).isEqualTo(7L);
        assertThat(response.getZipContent()).isNull();
    }

    /**
     * 测试目的：空请求不写 GetSkill proto 字段
     * 测试场景：空 POJO
     * 验证内容：name 为空字符串
     */
    @Test
    public void testToGetRequestSkipsBlankFields() {
        // Arrange
        SkillGetRequest request = new SkillGetRequest();

        // Act
        PolarisSkillGRPCService.GetSkillRequest proto = SkillProtoConverter.toGetRequest(request);

        // Assert
        assertThat(proto.getName()).isEmpty();
        assertThat(proto.getNamespace()).isEmpty();
        assertThat(proto.getVersion()).isEmpty();
    }
}
