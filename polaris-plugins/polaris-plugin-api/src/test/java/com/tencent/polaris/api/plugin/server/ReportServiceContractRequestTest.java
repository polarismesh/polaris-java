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

package com.tencent.polaris.api.plugin.server;

import com.tencent.polaris.specification.api.v1.service.manage.ServiceContractProto;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link ReportServiceContractRequest}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class ReportServiceContractRequestTest {

    /**
     * 测试 serviceFeatures 字段的设置和获取
     * 测试目的：验证新增的 serviceFeatures 字段可以正确设置和读取
     * 测试场景：构造包含多个 ServiceFeature 的列表，设置后读取
     * 验证内容：列表大小和内容与设置时一致
     */
    @Test
    public void testServiceFeatures_NormalData() {
        // Arrange
        ReportServiceContractRequest request = new ReportServiceContractRequest();
        List<ServiceFeature> features = new ArrayList<>();

        ServiceFeature toolFeature = new ServiceFeature();
        toolFeature.setType(ServiceContractProto.ServiceFeatureType.Service_Feature_Type_MCP_Tool);
        toolFeature.setName("get_weather");
        toolFeature.setDescription("Get weather info");
        toolFeature.setContent("{\"type\":\"object\"}");
        toolFeature.setStatus(ServiceContractProto.ServiceFeatureStatus.Service_Feature_Status_Enabled);
        features.add(toolFeature);

        ServiceFeature resourceFeature = new ServiceFeature();
        resourceFeature.setType(ServiceContractProto.ServiceFeatureType.Service_Feature_Type_MCP_Resource);
        resourceFeature.setName("main.rs");
        resourceFeature.setDescription("Main source file");
        resourceFeature.setStatus(ServiceContractProto.ServiceFeatureStatus.Service_Feature_Status_Enabled);
        features.add(resourceFeature);

        // Act
        request.setServiceFeatures(features);

        // Assert
        assertThat(request.getServiceFeatures()).hasSize(2);
        assertThat(request.getServiceFeatures().get(0).getName()).isEqualTo("get_weather");
        assertThat(request.getServiceFeatures().get(1).getName()).isEqualTo("main.rs");
    }

    /**
     * 测试 serviceFeatures 默认值为空列表
     * 测试目的：验证未设置时 serviceFeatures 为空列表而非 null，避免 NPE
     * 测试场景：新建 request 不设置 serviceFeatures
     * 验证内容：getServiceFeatures 返回空列表
     */
    @Test
    public void testServiceFeatures_DefaultEmptyList() {
        // Arrange & Act
        ReportServiceContractRequest request = new ReportServiceContractRequest();

        // Assert
        assertThat(request.getServiceFeatures()).isNotNull();
        assertThat(request.getServiceFeatures()).isEmpty();
    }

    /**
     * 测试 interfaceDescriptors 默认值为空列表
     * 测试目的：验证未设置时 interfaceDescriptors 为空列表而非 null，避免 NPE
     * 测试场景：新建 request 不设置 interfaceDescriptors
     * 验证内容：getInterfaceDescriptors 返回空列表
     */
    @Test
    public void testInterfaceDescriptors_DefaultEmptyList() {
        // Arrange & Act
        ReportServiceContractRequest request = new ReportServiceContractRequest();

        // Assert
        assertThat(request.getInterfaceDescriptors()).isNotNull();
        assertThat(request.getInterfaceDescriptors()).isEmpty();
    }

    /**
     * 测试设置空列表
     * 测试目的：验证设置空列表后可以正确读取
     * 测试场景：设置 Collections.emptyList()
     * 验证内容：返回空列表而非 null
     */
    @Test
    public void testServiceFeatures_EmptyList() {
        // Arrange
        ReportServiceContractRequest request = new ReportServiceContractRequest();

        // Act
        request.setServiceFeatures(Collections.emptyList());

        // Assert
        assertThat(request.getServiceFeatures()).isEmpty();
    }

    /**
     * 测试 interfaceDescriptors 字段的设置和获取
     * 测试目的：验证 interfaceDescriptors 字段可以正确设置和读取
     * 测试场景：构造包含多个 InterfaceDescriptor 的列表，设置后读取
     * 验证内容：列表大小和内容与设置时一致
     */
    @Test
    public void testInterfaceDescriptors_NormalData() {
        // Arrange
        ReportServiceContractRequest request = new ReportServiceContractRequest();
        List<InterfaceDescriptor> descriptors = new ArrayList<>();

        InterfaceDescriptor descriptor1 = new InterfaceDescriptor();
        descriptor1.setName("listTools");
        descriptor1.setMethod("GET");
        descriptor1.setPath("/api/tools");
        descriptor1.setContent("{\"type\":\"list\"}");
        descriptors.add(descriptor1);

        InterfaceDescriptor descriptor2 = new InterfaceDescriptor();
        descriptor2.setName("callTool");
        descriptor2.setMethod("POST");
        descriptor2.setPath("/api/tools/call");
        descriptors.add(descriptor2);

        // Act
        request.setInterfaceDescriptors(descriptors);

        // Assert
        assertThat(request.getInterfaceDescriptors()).hasSize(2);
        assertThat(request.getInterfaceDescriptors().get(0).getName()).isEqualTo("listTools");
        assertThat(request.getInterfaceDescriptors().get(1).getName()).isEqualTo("callTool");
    }

    /**
     * 测试 interfaceDescriptors 设置空列表
     * 测试目的：验证设置空列表后可以正确读取
     * 测试场景：设置 Collections.emptyList()
     * 验证内容：返回空列表而非 null
     */
    @Test
    public void testInterfaceDescriptors_EmptyList() {
        // Arrange
        ReportServiceContractRequest request = new ReportServiceContractRequest();

        // Act
        request.setInterfaceDescriptors(Collections.emptyList());

        // Assert
        assertThat(request.getInterfaceDescriptors()).isEmpty();
    }
}
