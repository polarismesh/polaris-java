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
 * @author polaris
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
}
