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

package com.tencent.polaris.assembly.api.pojo;

import java.util.HashMap;
import java.util.Map;

import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.api.pojo.SourceService;
import com.tencent.polaris.api.rpc.MetadataFailoverType;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link GetReachableInstancesRequest}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class GetReachableInstancesRequestTest {

    /**
     * 测试 setServiceInfo 传入 SourceService 直接保留
     * 测试目的：验证传入 SourceService 实例时不做转换直接保存
     * 测试场景：构造 SourceService 设置 namespace/service 后传入
     * 验证内容：getServiceInfo 返回同一实例
     */
    @Test
    public void testSetServiceInfoWithSourceService() {
        // Arrange
        GetReachableInstancesRequest request = new GetReachableInstancesRequest();
        SourceService sourceService = new SourceService();
        sourceService.setNamespace("Prod");
        sourceService.setService("caller");

        // Act
        request.setServiceInfo(sourceService);

        // Assert
        assertThat(request.getServiceInfo()).isSameAs(sourceService);
    }

    /**
     * 测试 setServiceInfo 传入普通 ServiceInfo 转换为 SourceService
     * 测试目的：验证传入非 SourceService 时转换为 SourceService 并把 metadata 转为 arguments
     * 测试场景：构造 ServiceInfo 设置 namespace/service/metadata 后传入
     * 验证内容：返回 SourceService，namespace/service 一致，arguments 非空
     */
    @Test
    public void testSetServiceInfoWithPlainServiceInfo() {
        // Arrange
        GetReachableInstancesRequest request = new GetReachableInstancesRequest();
        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.setNamespace("Prod");
        serviceInfo.setService("caller");
        Map<String, String> metadata = new HashMap<>();
        metadata.put("env", "prod");
        serviceInfo.setMetadata(metadata);

        // Act
        request.setServiceInfo(serviceInfo);

        // Assert
        SourceService result = request.getServiceInfo();
        assertThat(result).isInstanceOf(SourceService.class);
        assertThat(result.getNamespace()).isEqualTo("Prod");
        assertThat(result.getService()).isEqualTo("caller");
        assertThat(result.getArguments()).isNotEmpty();
    }

    /**
     * 测试 setServiceInfo 传入 metadata 为 null 的 ServiceInfo
     * 测试目的：验证 metadata 为 null 时不抛 NPE
     * 测试场景：ServiceInfo 不设置 metadata
     * 验证内容：转换成功，arguments 为空
     */
    @Test
    public void testSetServiceInfoWithNullMetadata() {
        // Arrange
        GetReachableInstancesRequest request = new GetReachableInstancesRequest();
        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.setNamespace("Prod");
        serviceInfo.setService("caller");

        // Act
        request.setServiceInfo(serviceInfo);

        // Assert
        SourceService result = request.getServiceInfo();
        assertThat(result.getNamespace()).isEqualTo("Prod");
        assertThat(result.getArguments()).isEmpty();
    }

    /**
     * 测试基本属性 setter/getter
     * 测试目的：验证 metadata/canary/method/includeCircuitBreak/includeUnhealthy 读写
     * 测试场景：设置各字段后读取
     * 验证内容：读取值与设置值一致
     */
    @Test
    public void testBasicSettersAndGetters() {
        // Arrange
        GetReachableInstancesRequest request = new GetReachableInstancesRequest();
        Map<String, String> metadata = new HashMap<>();
        metadata.put("k", "v");

        // Act
        request.setMetadata(metadata);
        request.setCanary("gray");
        request.setMethod("GET");
        request.setIncludeCircuitBreak(true);
        request.setIncludeUnhealthy(true);
        request.setMetadataFailoverType(MetadataFailoverType.METADATAFAILOVERNONE);

        // Assert
        assertThat(request.getMetadata()).isEqualTo(metadata);
        assertThat(request.getCanary()).isEqualTo("gray");
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.isIncludeCircuitBreak()).isTrue();
        assertThat(request.isIncludeUnhealthy()).isTrue();
        assertThat(request.getMetadataFailoverType()).isEqualTo(MetadataFailoverType.METADATAFAILOVERNONE);
    }

    /**
     * 测试 includeCircuitBreak/includeUnhealthy 默认为 false
     * 测试目的：验证布尔字段默认值
     * 测试场景：new 构造后直接读取
     * 验证内容：两个布尔字段均为 false
     */
    @Test
    public void testDefaultBooleanFlags() {
        // Act
        GetReachableInstancesRequest request = new GetReachableInstancesRequest();

        // Assert
        assertThat(request.isIncludeCircuitBreak()).isFalse();
        assertThat(request.isIncludeUnhealthy()).isFalse();
    }

    /**
     * 测试 MetadataFailoverType.getByName 匹配已知名称
     * 测试目的：验证 getByName 能按 name 解析出对应枚举
     * 测试场景：传入 metadataFailoverAll
     * 验证内容：返回 METADATAFAILOVERALL
     */
    @Test
    public void testMetadataFailoverTypeGetByName() {
        // Act & Assert
        assertThat(MetadataFailoverType.getByName("metadataFailoverAll"))
                .isEqualTo(MetadataFailoverType.METADATAFAILOVERALL);
    }

    /**
     * 测试 MetadataFailoverType.getByName 未知名称兜底
     * 测试目的：验证 getByName 对未知名称返回默认值 METADATAFAILOVERNONE
     * 测试场景：传入不存在的名称
     * 验证内容：返回 METADATAFAILOVERNONE
     */
    @Test
    public void testMetadataFailoverTypeGetByNameUnknown() {
        // Act & Assert
        assertThat(MetadataFailoverType.getByName("not-exist"))
                .isEqualTo(MetadataFailoverType.METADATAFAILOVERNONE);
    }

    /**
     * 测试 timeoutMs 继承自 RequestBaseEntity
     * 测试目的：验证继承的超时属性读写
     * 测试场景：setTimeoutMs 后读取
     * 验证内容：读取值与设置值一致
     */
    @Test
    public void testTimeoutMs() {
        // Arrange
        GetReachableInstancesRequest request = new GetReachableInstancesRequest();

        // Act
        request.setTimeoutMs(2000L);

        // Assert
        assertThat(request.getTimeoutMs()).isEqualTo(2000L);
    }

    /**
     * 测试 toString 包含关键字段
     * 测试目的：验证 toString 输出包含 canary/method
     * 测试场景：设置字段后调用 toString
     * 验证内容：toString 包含 canary 和 method 值
     */
    @Test
    public void testToString() {
        // Arrange
        GetReachableInstancesRequest request = new GetReachableInstancesRequest();
        request.setCanary("gray");
        request.setMethod("POST");

        // Act
        String str = request.toString();

        // Assert
        assertThat(str).contains("gray");
        assertThat(str).contains("POST");
    }
}
