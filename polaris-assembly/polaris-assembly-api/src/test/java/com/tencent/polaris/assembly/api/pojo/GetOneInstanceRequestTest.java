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
import java.util.Optional;

import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.api.pojo.SourceService;
import com.tencent.polaris.api.rpc.Criteria;
import com.tencent.polaris.api.rpc.MetadataFailoverType;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link GetOneInstanceRequest}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class GetOneInstanceRequestTest {

    /**
     * 测试 setServiceInfo 传入 SourceService 直接保留
     * 测试目的：验证传入 SourceService 实例时不做转换直接保存
     * 测试场景：构造 SourceService 设置 namespace/service 后传入
     * 验证内容：getServiceInfo 返回同一实例
     */
    @Test
    public void testSetServiceInfoWithSourceService() {
        // Arrange
        GetOneInstanceRequest request = new GetOneInstanceRequest();
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
     * 测试目的：验证传入非 SourceService 时转换为 SourceService 并保留 namespace/service
     * 测试场景：构造 ServiceInfo 设置 namespace/service/metadata 后传入
     * 验证内容：返回 SourceService，namespace/service 一致，metadata 转为 arguments
     */
    @Test
    public void testSetServiceInfoWithPlainServiceInfo() {
        // Arrange
        GetOneInstanceRequest request = new GetOneInstanceRequest();
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
        GetOneInstanceRequest request = new GetOneInstanceRequest();
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
     * 测试目的：验证 metadata/canary/method/criteria 等字段读写
     * 测试场景：设置各字段后读取
     * 验证内容：读取值与设置值一致
     */
    @Test
    public void testBasicSettersAndGetters() {
        // Arrange
        GetOneInstanceRequest request = new GetOneInstanceRequest();
        Map<String, String> metadata = new HashMap<>();
        metadata.put("k", "v");
        Criteria criteria = new Criteria();

        // Act
        request.setMetadata(metadata);
        request.setCanary("gray");
        request.setMethod("GET");
        request.setCriteria(criteria);
        request.setMetadataFailoverType(MetadataFailoverType.METADATAFAILOVERNONE);

        // Assert
        assertThat(request.getMetadata()).isEqualTo(metadata);
        assertThat(request.getCanary()).isEqualTo("gray");
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getCriteria()).isEqualTo(criteria);
        assertThat(request.getMetadataFailoverType()).isEqualTo(MetadataFailoverType.METADATAFAILOVERNONE);
    }

    /**
     * 测试 externalParameterSupplier 默认返回 empty
     * 测试目的：验证默认 supplier 对任意输入返回 Optional.empty
     * 测试场景：new 构造后调用默认 supplier
     * 验证内容：返回 Optional.empty
     */
    @Test
    public void testDefaultExternalParameterSupplier() {
        // Arrange
        GetOneInstanceRequest request = new GetOneInstanceRequest();

        // Act
        Optional<String> result = request.getExternalParameterSupplier().apply("any");

        // Assert
        assertThat(result).isEmpty();
    }

    /**
     * 测试 setExternalParameterSupplier
     * 测试目的：验证可自定义 externalParameterSupplier
     * 测试场景：设置返回固定值的 supplier
     * 验证内容：apply 返回设置的值
     */
    @Test
    public void testSetExternalParameterSupplier() {
        // Arrange
        GetOneInstanceRequest request = new GetOneInstanceRequest();
        request.setExternalParameterSupplier(s -> Optional.of("val-" + s));

        // Act
        Optional<String> result = request.getExternalParameterSupplier().apply("k1");

        // Assert
        assertThat(result).contains("val-k1");
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
        GetOneInstanceRequest request = new GetOneInstanceRequest();

        // Act
        request.setTimeoutMs(1000L);

        // Assert
        assertThat(request.getTimeoutMs()).isEqualTo(1000L);
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
        GetOneInstanceRequest request = new GetOneInstanceRequest();
        request.setCanary("gray");
        request.setMethod("POST");

        // Act
        String str = request.toString();

        // Assert
        assertThat(str).contains("gray");
        assertThat(str).contains("POST");
    }
}
