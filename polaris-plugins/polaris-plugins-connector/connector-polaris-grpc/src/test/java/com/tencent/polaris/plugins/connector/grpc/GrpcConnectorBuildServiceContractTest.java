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

package com.tencent.polaris.plugins.connector.grpc;

import com.tencent.polaris.api.plugin.server.InterfaceDescriptor;
import com.tencent.polaris.api.plugin.server.ReportServiceContractRequest;
import com.tencent.polaris.api.plugin.server.ServiceFeature;
import com.tencent.polaris.specification.api.v1.service.manage.ServiceContractProto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link GrpcConnector} buildReportServiceContractRequest method.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class GrpcConnectorBuildServiceContractTest {

    private GrpcConnector grpcConnector;

    private Method buildMethod;

    @Before
    public void setUp() throws Exception {
        grpcConnector = new GrpcConnector();
        buildMethod = GrpcConnector.class.getDeclaredMethod(
                "buildReportServiceContractRequest", ReportServiceContractRequest.class);
        buildMethod.setAccessible(true);
    }

    /**
     * 测试正常的 serviceFeatures 序列化
     * 测试目的：验证 ServiceFeature 列表能正确序列化到 protobuf 消息
     * 测试场景：构造包含 Tool、Resource、Prompt 三种类型的 feature 列表
     * 验证内容：protobuf 消息中的 serviceFeatures 数量、类型、名称、描述、内容、状态均正确
     */
    @Test
    public void testBuildRequest_WithServiceFeatures() throws Exception {
        // Arrange
        ReportServiceContractRequest request = new ReportServiceContractRequest();
        request.setName("test-contract");
        request.setNamespace("default");
        request.setService("test-service");
        request.setProtocol("mcp-sse");
        request.setVersion("1.0.0");
        request.setInterfaceDescriptors(Collections.emptyList());

        List<ServiceFeature> features = new ArrayList<>();

        ServiceFeature toolFeature = new ServiceFeature();
        toolFeature.setType(ServiceContractProto.ServiceFeatureType.Service_Feature_Type_MCP_Tool);
        toolFeature.setName("get_weather");
        toolFeature.setDescription("Get weather information");
        toolFeature.setContent("{\"type\":\"object\"}");
        toolFeature.setStatus(ServiceContractProto.ServiceFeatureStatus.Service_Feature_Status_Enabled);
        features.add(toolFeature);

        ServiceFeature resourceFeature = new ServiceFeature();
        resourceFeature.setType(ServiceContractProto.ServiceFeatureType.Service_Feature_Type_MCP_Resource);
        resourceFeature.setName("main.rs");
        resourceFeature.setDescription("Main source file");
        resourceFeature.setContent("");
        resourceFeature.setStatus(ServiceContractProto.ServiceFeatureStatus.Service_Feature_Status_Enabled);
        features.add(resourceFeature);

        ServiceFeature promptFeature = new ServiceFeature();
        promptFeature.setType(ServiceContractProto.ServiceFeatureType.Service_Feature_Type_MCP_Prompt);
        promptFeature.setName("code_review");
        promptFeature.setDescription("Code review prompt");
        promptFeature.setContent("Review this code");
        promptFeature.setStatus(ServiceContractProto.ServiceFeatureStatus.Service_Feature_Status_Disabled);
        features.add(promptFeature);

        request.setServiceFeatures(features);

        // Act
        ServiceContractProto.ServiceContract result =
                (ServiceContractProto.ServiceContract) buildMethod.invoke(grpcConnector, request);

        // Assert
        assertThat(result.getServiceFeaturesList()).hasSize(3);

        ServiceContractProto.ServiceFeature protoTool = result.getServiceFeatures(0);
        assertThat(protoTool.getType()).isEqualTo(ServiceContractProto.ServiceFeatureType.Service_Feature_Type_MCP_Tool);
        assertThat(protoTool.getName()).isEqualTo("get_weather");
        assertThat(protoTool.getDescription()).isEqualTo("Get weather information");
        assertThat(protoTool.getContent()).isEqualTo("{\"type\":\"object\"}");
        assertThat(protoTool.getStatus()).isEqualTo(ServiceContractProto.ServiceFeatureStatus.Service_Feature_Status_Enabled);

        ServiceContractProto.ServiceFeature protoResource = result.getServiceFeatures(1);
        assertThat(protoResource.getType()).isEqualTo(ServiceContractProto.ServiceFeatureType.Service_Feature_Type_MCP_Resource);
        assertThat(protoResource.getName()).isEqualTo("main.rs");

        ServiceContractProto.ServiceFeature protoPrompt = result.getServiceFeatures(2);
        assertThat(protoPrompt.getType()).isEqualTo(ServiceContractProto.ServiceFeatureType.Service_Feature_Type_MCP_Prompt);
        assertThat(protoPrompt.getName()).isEqualTo("code_review");
        assertThat(protoPrompt.getStatus()).isEqualTo(ServiceContractProto.ServiceFeatureStatus.Service_Feature_Status_Disabled);
    }

    /**
     * 测试 serviceFeatures 为 null 时不报错
     * 测试目的：验证 null 防御逻辑正常工作
     * 测试场景：不设置 serviceFeatures（默认 null）
     * 验证内容：protobuf 消息中 serviceFeatures 为空列表
     */
    @Test
    public void testBuildRequest_WithNullServiceFeatures() throws Exception {
        // Arrange
        ReportServiceContractRequest request = new ReportServiceContractRequest();
        request.setName("test-contract");
        request.setNamespace("default");
        request.setService("test-service");
        request.setProtocol("mcp-sse");
        request.setInterfaceDescriptors(Collections.emptyList());

        // Act
        ServiceContractProto.ServiceContract result =
                (ServiceContractProto.ServiceContract) buildMethod.invoke(grpcConnector, request);

        // Assert
        assertThat(result.getServiceFeaturesList()).isEmpty();
    }

    /**
     * 测试 serviceFeatures 为空列表时
     * 测试目的：验证空列表正确处理
     * 测试场景：设置 Collections.emptyList()
     * 验证内容：protobuf 消息中 serviceFeatures 为空列表
     */
    @Test
    public void testBuildRequest_WithEmptyServiceFeatures() throws Exception {
        // Arrange
        ReportServiceContractRequest request = new ReportServiceContractRequest();
        request.setName("test-contract");
        request.setNamespace("default");
        request.setService("test-service");
        request.setProtocol("mcp-sse");
        request.setInterfaceDescriptors(Collections.emptyList());
        request.setServiceFeatures(Collections.emptyList());

        // Act
        ServiceContractProto.ServiceContract result =
                (ServiceContractProto.ServiceContract) buildMethod.invoke(grpcConnector, request);

        // Assert
        assertThat(result.getServiceFeaturesList()).isEmpty();
    }

    /**
     * 测试 ServiceFeature 中 String 字段为 null 时使用默认空字符串
     * 测试目的：验证 StringUtils.defaultString 防御逻辑
     * 测试场景：ServiceFeature 的 name/description/content 为 null
     * 验证内容：protobuf 消息中对应字段为空字符串
     */
    @Test
    public void testBuildRequest_WithNullStringFields() throws Exception {
        // Arrange
        ReportServiceContractRequest request = new ReportServiceContractRequest();
        request.setName("test-contract");
        request.setNamespace("default");
        request.setService("test-service");
        request.setProtocol("mcp-sse");
        request.setInterfaceDescriptors(Collections.emptyList());

        List<ServiceFeature> features = new ArrayList<>();
        ServiceFeature feature = new ServiceFeature();
        feature.setType(ServiceContractProto.ServiceFeatureType.Service_Feature_Type_MCP_Tool);
        feature.setStatus(ServiceContractProto.ServiceFeatureStatus.Service_Feature_Status_Enabled);
        features.add(feature);
        request.setServiceFeatures(features);

        // Act
        ServiceContractProto.ServiceContract result =
                (ServiceContractProto.ServiceContract) buildMethod.invoke(grpcConnector, request);

        // Assert
        ServiceContractProto.ServiceFeature protoFeature = result.getServiceFeatures(0);
        assertThat(protoFeature.getName()).isEmpty();
        assertThat(protoFeature.getDescription()).isEmpty();
        assertThat(protoFeature.getContent()).isEmpty();
    }
}
