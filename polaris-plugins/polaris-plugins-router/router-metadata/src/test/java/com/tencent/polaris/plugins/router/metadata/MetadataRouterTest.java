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

package com.tencent.polaris.plugins.router.metadata;

import com.tencent.polaris.api.config.consumer.ServiceRouterConfig;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.route.RouteInfo;
import com.tencent.polaris.api.plugin.route.RouteResult;
import com.tencent.polaris.api.plugin.route.ServiceRouter;
import com.tencent.polaris.api.pojo.*;
import com.tencent.polaris.api.rpc.MetadataFailoverType;
import com.tencent.polaris.metadata.core.MetadataContainer;
import com.tencent.polaris.metadata.core.TransitiveType;
import com.tencent.polaris.metadata.core.manager.CalleeMetadataContainerGroup;
import com.tencent.polaris.metadata.core.manager.MetadataContainerGroup;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static com.tencent.polaris.metadata.core.manager.MetadataContext.DEFAULT_TRANSITIVE_PREFIX;
import static com.tencent.polaris.plugins.router.metadata.MetadataRouter.KEY_METADATA_KEYS;
import static com.tencent.polaris.plugins.router.metadata.MetadataRouter.ROUTER_TYPE_METADATA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link MetadataRouter}.
 *
 * @author Haotian Zhang
 */
public class MetadataRouterTest {

    private MetadataRouter metadataRouter;

    private MetadataRouterConfig config;

    @Before
    public void setUp() {
        metadataRouter = new MetadataRouter();
        config = new MetadataRouterConfig();
        config.setMetadataFailOverType(FailOverType.none);
        metadataRouter.setConfig(config);
    }

    /**
     * 创建带有 metadata 的实例
     */
    private DefaultInstance createInstance(String id, String host, int port, Map<String, String> metadata) {
        DefaultInstance instance = new DefaultInstance();
        instance.setId(id);
        instance.setHost(host);
        instance.setPort(port);
        instance.setHealthy(true);
        instance.setMetadata(metadata);
        return instance;
    }

    /**
     * 创建RouteInfo，并设置路由元数据
     */
    private RouteInfo createRouteInfo(Map<String, String> routerMetadata) {
        SourceService sourceService = new SourceService();
        sourceService.setNamespace("testNamespace");
        sourceService.setService("testService");
        RouteInfo routeInfo = new RouteInfo(sourceService, null, null, null);

        // set router metadata
        MetadataContainerGroup calleeMetadataContainerGroup = new CalleeMetadataContainerGroup(DEFAULT_TRANSITIVE_PREFIX);
        MetadataContainer calleeCustomMetadataContainer = calleeMetadataContainerGroup.getCustomMetadataContainer();
        String metadataKeys = String.join(",", routerMetadata.keySet());
        calleeCustomMetadataContainer.putMetadataMapValue(ROUTER_TYPE_METADATA, KEY_METADATA_KEYS, metadataKeys, TransitiveType.NONE);
        for (Map.Entry<String, String> entry : routerMetadata.entrySet()) {
            calleeCustomMetadataContainer.putMetadataStringValue(entry.getKey(), entry.getValue(), TransitiveType.NONE);
        }
        routeInfo.setMetadataContainerGroup(calleeMetadataContainerGroup);

        return routeInfo;
    }

    /**
     * 测试场景1：请求metadata完全匹配实例metadata，返回匹配的实例
     */
    @Test
    public void testRouterWithMatchingMetadata() {
        // 准备测试数据
        Map<String, String> reqMetadata = new HashMap<>();
        reqMetadata.put("env", "test");
        reqMetadata.put("version", "v1");

        Map<String, String> instanceMetadata1 = new HashMap<>();
        instanceMetadata1.put("env", "test");
        instanceMetadata1.put("version", "v1");

        Map<String, String> instanceMetadata2 = new HashMap<>();
        instanceMetadata2.put("env", "prod");
        instanceMetadata2.put("version", "v2");

        Map<String, String> instanceMetadata3 = new HashMap<>();
        instanceMetadata3.put("env", "test");
        instanceMetadata3.put("version", "v1");
        instanceMetadata3.put("region", "cn-north");

        List<Instance> instances = new ArrayList<>();
        instances.add(createInstance("1", "127.0.0.1", 8080, instanceMetadata1));
        instances.add(createInstance("2", "127.0.0.2", 8080, instanceMetadata2));
        instances.add(createInstance("3", "127.0.0.3", 8080, instanceMetadata3));

        ServiceInstances serviceInstances = new DefaultServiceInstances(
                new ServiceKey("testNamespace", "testService"), instances);

        RouteInfo routeInfo = createRouteInfo(reqMetadata);

        // 执行测试
        RouteResult result = metadataRouter.router(routeInfo, serviceInstances);

        // 验证结果：应该返回实例1和实例3（都包含env=test和version=v1）
        assertThat(result.getInstances()).hasSize(2);
        assertThat(result.getInstances().stream().map(Instance::getId))
                .containsExactlyInAnyOrder("1", "3");
        assertThat(result.getNextRouterInfo().getState()).isEqualTo(RouteResult.State.Next);
    }

    /**
     * 测试场景2：请求metadata部分匹配，只返回完全匹配的实例
     */
    @Test
    public void testRouterWithPartialMatchingMetadata() {
        Map<String, String> reqMetadata = new HashMap<>();
        reqMetadata.put("env", "test");
        reqMetadata.put("version", "v1");

        // 实例1只匹配env
        Map<String, String> instanceMetadata1 = new HashMap<>();
        instanceMetadata1.put("env", "test");

        // 实例2完全匹配
        Map<String, String> instanceMetadata2 = new HashMap<>();
        instanceMetadata2.put("env", "test");
        instanceMetadata2.put("version", "v1");

        List<Instance> instances = new ArrayList<>();
        instances.add(createInstance("1", "127.0.0.1", 8080, instanceMetadata1));
        instances.add(createInstance("2", "127.0.0.2", 8080, instanceMetadata2));

        ServiceInstances serviceInstances = new DefaultServiceInstances(
                new ServiceKey("testNamespace", "testService"), instances);

        RouteInfo routeInfo = createRouteInfo(reqMetadata);

        RouteResult result = metadataRouter.router(routeInfo, serviceInstances);

        // 只有实例2完全匹配
        assertThat(result.getInstances()).hasSize(1);
        assertThat(result.getInstances().get(0).getId()).isEqualTo("2");
    }

    /**
     * 测试场景3：无匹配实例时，failoverType为none，抛出异常
     */
    @Test
    public void testRouterNoMatchWithFailoverTypeNone() {
        Map<String, String> reqMetadata = new HashMap<>();
        reqMetadata.put("env", "test");

        Map<String, String> instanceMetadata = new HashMap<>();
        instanceMetadata.put("env", "prod");

        List<Instance> instances = new ArrayList<>();
        instances.add(createInstance("1", "127.0.0.1", 8080, instanceMetadata));

        ServiceInstances serviceInstances = new DefaultServiceInstances(
                new ServiceKey("testNamespace", "testService"), instances);

        RouteInfo routeInfo = createRouteInfo(reqMetadata);
        config.setMetadataFailOverType(FailOverType.none);

        assertThatThrownBy(() -> metadataRouter.router(routeInfo, serviceInstances))
                .isInstanceOf(PolarisException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.METADATA_MISMATCH);
    }

    /**
     * 测试场景4：无匹配实例时，failoverType为all，返回所有实例
     */
    @Test
    public void testRouterNoMatchWithFailoverTypeAll() {
        Map<String, String> reqMetadata = new HashMap<>();
        reqMetadata.put("env", "test");

        Map<String, String> instanceMetadata1 = new HashMap<>();
        instanceMetadata1.put("env", "prod");

        Map<String, String> instanceMetadata2 = new HashMap<>();
        instanceMetadata2.put("env", "staging");

        List<Instance> instances = new ArrayList<>();
        instances.add(createInstance("1", "127.0.0.1", 8080, instanceMetadata1));
        instances.add(createInstance("2", "127.0.0.2", 8080, instanceMetadata2));

        ServiceInstances serviceInstances = new DefaultServiceInstances(
                new ServiceKey("testNamespace", "testService"), instances);

        RouteInfo routeInfo = createRouteInfo(reqMetadata);
        config.setMetadataFailOverType(FailOverType.all);

        RouteResult result = metadataRouter.router(routeInfo, serviceInstances);

        // 应该返回所有实例
        assertThat(result.getInstances()).hasSize(2);
        assertThat(result.getNextRouterInfo().getState()).isEqualTo(RouteResult.State.Next);
    }

    /**
     * 测试场景5：无匹配实例时，failoverType为others，返回不包含请求metadata key的实例
     */
    @Test
    public void testRouterNoMatchWithFailoverTypeOthers() {
        Map<String, String> reqMetadata = new HashMap<>();
        reqMetadata.put("env", "test");

        // 实例1包含env key
        Map<String, String> instanceMetadata1 = new HashMap<>();
        instanceMetadata1.put("env", "prod");

        // 实例2不包含env key
        Map<String, String> instanceMetadata2 = new HashMap<>();
        instanceMetadata2.put("region", "cn-north");

        // 实例3也不包含env key
        Map<String, String> instanceMetadata3 = new HashMap<>();
        instanceMetadata3.put("version", "v1");

        List<Instance> instances = new ArrayList<>();
        instances.add(createInstance("1", "127.0.0.1", 8080, instanceMetadata1));
        instances.add(createInstance("2", "127.0.0.2", 8080, instanceMetadata2));
        instances.add(createInstance("3", "127.0.0.3", 8080, instanceMetadata3));

        ServiceInstances serviceInstances = new DefaultServiceInstances(
                new ServiceKey("testNamespace", "testService"), instances);

        RouteInfo routeInfo = createRouteInfo(reqMetadata);
        config.setMetadataFailOverType(FailOverType.others);

        RouteResult result = metadataRouter.router(routeInfo, serviceInstances);

        // 应该返回不包含env key的实例2和实例3
        assertThat(result.getInstances()).hasSize(2);
        assertThat(result.getInstances().stream().map(Instance::getId))
                .containsExactlyInAnyOrder("2", "3");
    }

    /**
     * 测试场景6：通过RouteInfo中的MetadataFailoverType覆盖配置的failoverType
     */
    @Test
    public void testRouterWithRouteInfoMetadataFailoverType() {
        Map<String, String> reqMetadata = new HashMap<>();
        reqMetadata.put("env", "test");

        Map<String, String> instanceMetadata = new HashMap<>();
        instanceMetadata.put("env", "prod");

        List<Instance> instances = new ArrayList<>();
        instances.add(createInstance("1", "127.0.0.1", 8080, instanceMetadata));

        ServiceInstances serviceInstances = new DefaultServiceInstances(
                new ServiceKey("testNamespace", "testService"), instances);

        RouteInfo routeInfo = createRouteInfo(reqMetadata);
        // 配置默认为none
        config.setMetadataFailOverType(FailOverType.none);
        // 但RouteInfo指定为all
        routeInfo.setMetadataFailoverType(MetadataFailoverType.METADATAFAILOVERALL);

        RouteResult result = metadataRouter.router(routeInfo, serviceInstances);

        // 应该按RouteInfo的设置返回所有实例
        assertThat(result.getInstances()).hasSize(1);
    }

    /**
     * 测试场景7：通过服务metadata设置failoverType
     */
    @Test
    public void testRouterWithServiceMetadataFailoverType() {
        Map<String, String> reqMetadata = new HashMap<>();
        reqMetadata.put("env", "test");

        Map<String, String> instanceMetadata = new HashMap<>();
        instanceMetadata.put("env", "prod");

        List<Instance> instances = new ArrayList<>();
        instances.add(createInstance("1", "127.0.0.1", 8080, instanceMetadata));

        // 服务metadata中设置failover type
        Map<String, String> svcMetadata = new HashMap<>();
        svcMetadata.put("internal-metadata-failover-type", "all");

        ServiceInstances serviceInstances = new DefaultServiceInstances(
                new ServiceKey("testNamespace", "testService"), instances, svcMetadata);

        RouteInfo routeInfo = createRouteInfo(reqMetadata);
        config.setMetadataFailOverType(FailOverType.none);

        RouteResult result = metadataRouter.router(routeInfo, serviceInstances);

        // 服务metadata设置为all，应返回所有实例
        assertThat(result.getInstances()).hasSize(1);
    }

    /**
     * 测试场景8：空的请求metadata，应该返回所有实例
     */
    @Test
    public void testRouterWithEmptyRequestMetadata() {
        Map<String, String> instanceMetadata1 = new HashMap<>();
        instanceMetadata1.put("env", "test");

        Map<String, String> instanceMetadata2 = new HashMap<>();
        instanceMetadata2.put("env", "prod");

        List<Instance> instances = new ArrayList<>();
        instances.add(createInstance("1", "127.0.0.1", 8080, instanceMetadata1));
        instances.add(createInstance("2", "127.0.0.2", 8080, instanceMetadata2));

        ServiceInstances serviceInstances = new DefaultServiceInstances(
                new ServiceKey("testNamespace", "testService"), instances);

        RouteInfo routeInfo = createRouteInfo(Collections.emptyMap());

        RouteResult result = metadataRouter.router(routeInfo, serviceInstances);

        // 空的请求metadata，所有实例都匹配
        assertThat(result.getInstances()).hasSize(2);
    }

    /**
     * 测试场景9：实例metadata为空但请求metadata不为空
     */
    @Test
    public void testRouterWithEmptyInstanceMetadata() {
        Map<String, String> reqMetadata = new HashMap<>();
        reqMetadata.put("env", "test");

        // 实例1没有metadata
        Map<String, String> instanceMetadata1 = new HashMap<>();

        // 实例2有匹配的metadata
        Map<String, String> instanceMetadata2 = new HashMap<>();
        instanceMetadata2.put("env", "test");

        List<Instance> instances = new ArrayList<>();
        instances.add(createInstance("1", "127.0.0.1", 8080, instanceMetadata1));
        instances.add(createInstance("2", "127.0.0.2", 8080, instanceMetadata2));

        ServiceInstances serviceInstances = new DefaultServiceInstances(
                new ServiceKey("testNamespace", "testService"), instances);

        RouteInfo routeInfo = createRouteInfo(reqMetadata);

        RouteResult result = metadataRouter.router(routeInfo, serviceInstances);

        // 只有实例2匹配
        assertThat(result.getInstances()).hasSize(1);
        assertThat(result.getInstances().get(0).getId()).isEqualTo("2");
    }

    /**
     * 测试场景10：MetadataFailoverType.METADATAFAILOVERNOTKEY
     */
    @Test
    public void testRouterWithMetadataFailoverNotKey() {
        Map<String, String> reqMetadata = new HashMap<>();
        reqMetadata.put("env", "test");
        reqMetadata.put("version", "v1");

        // 实例1包含所有请求的key
        Map<String, String> instanceMetadata1 = new HashMap<>();
        instanceMetadata1.put("env", "prod");
        instanceMetadata1.put("version", "v2");

        // 实例2只包含部分key
        Map<String, String> instanceMetadata2 = new HashMap<>();
        instanceMetadata2.put("env", "staging");

        // 实例3不包含任何请求的key
        Map<String, String> instanceMetadata3 = new HashMap<>();
        instanceMetadata3.put("region", "cn-north");

        List<Instance> instances = new ArrayList<>();
        instances.add(createInstance("1", "127.0.0.1", 8080, instanceMetadata1));
        instances.add(createInstance("2", "127.0.0.2", 8080, instanceMetadata2));
        instances.add(createInstance("3", "127.0.0.3", 8080, instanceMetadata3));

        ServiceInstances serviceInstances = new DefaultServiceInstances(
                new ServiceKey("testNamespace", "testService"), instances);

        RouteInfo routeInfo = createRouteInfo(reqMetadata);
        routeInfo.setMetadataFailoverType(MetadataFailoverType.METADATAFAILOVERNOTKEY);

        RouteResult result = metadataRouter.router(routeInfo, serviceInstances);

        // 实例2不包含version key，实例3不包含env和version key
        // 根据代码逻辑，只要有一个key不存在就会被选中
        assertThat(result.getInstances()).hasSize(2);
        assertThat(result.getInstances().stream().map(Instance::getId))
                .containsExactlyInAnyOrder("2", "3");
    }

    /**
     * 测试getAspect方法
     */
    @Test
    public void testGetAspect() {
        assertThat(metadataRouter.getAspect()).isEqualTo(ServiceRouter.Aspect.MIDDLE);
    }

    /**
     * 测试getName方法
     */
    @Test
    public void testGetName() {
        assertThat(metadataRouter.getName()).isEqualTo(ServiceRouterConfig.DEFAULT_ROUTER_METADATA);
    }

    /**
     * 测试getConfig方法
     */
    @Test
    public void testGetConfig() {
        assertThat(metadataRouter.getConfig()).isEqualTo(config);
    }

    /**
     * 测试enable方法 - 有metadata时应该启用
     */
    @Test
    public void testEnableWithMetadata() {
        Map<String, String> reqMetadata = new HashMap<>();
        reqMetadata.put("env", "test");

        RouteInfo routeInfo = createRouteInfo(reqMetadata);

        assertThat(metadataRouter.enable(routeInfo, null)).isTrue();
    }

    /**
     * 测试enable方法 - 无metadata时应该禁用
     */
    @Test
    public void testEnableWithoutMetadata() {
        RouteInfo routeInfo = createRouteInfo(Collections.emptyMap());

        assertThat(metadataRouter.enable(routeInfo, null)).isFalse();
    }
}