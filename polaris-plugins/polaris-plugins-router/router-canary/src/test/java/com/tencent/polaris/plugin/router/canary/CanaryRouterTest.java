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

package com.tencent.polaris.plugin.router.canary;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.tencent.polaris.api.config.consumer.ServiceRouterConfig;
import com.tencent.polaris.api.plugin.route.RouteInfo;
import com.tencent.polaris.api.plugin.route.RouteResult;
import com.tencent.polaris.api.plugin.route.RouterConstants;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceMetadata;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link CanaryRouter}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class CanaryRouterTest {

    private Instance buildInstance(Map<String, String> metadata) {
        Instance instance = mock(Instance.class);
        when(instance.getMetadata()).thenReturn(metadata);
        return instance;
    }

    private ServiceInstances buildInstances(Instance... instances) {
        ServiceInstances svcInstances = mock(ServiceInstances.class);
        when(svcInstances.getInstances()).thenReturn(Arrays.asList(instances));
        return svcInstances;
    }

    private RouteInfo buildRouteInfo(String canary) {
        RouteInfo routeInfo = mock(RouteInfo.class);
        when(routeInfo.getCanary()).thenReturn(canary);
        return routeInfo;
    }

    /**
     * 测试 router 在指定 canary 时过滤匹配的实例
     * 测试目的：验证 canaryValue 非空时只保留 metadata 中 canary 匹配的实例
     * 测试场景：canary=gray，2 个实例（canary=gray、canary=prod）
     * 验证内容：结果只含 canary=gray 的实例
     */
    @Test
    public void testRouterWithCanaryFilterMatched() {
        // Arrange
        CanaryRouter router = new CanaryRouter();
        Map<String, String> meta1 = new HashMap<>();
        meta1.put(RouterConstants.CANARY_KEY, "gray");
        Map<String, String> meta2 = new HashMap<>();
        meta2.put(RouterConstants.CANARY_KEY, "prod");
        Instance gray = buildInstance(meta1);
        Instance prod = buildInstance(meta2);
        ServiceInstances svcInstances = buildInstances(gray, prod);

        // Act
        RouteResult result = router.router(buildRouteInfo("gray"), svcInstances);

        // Assert
        assertThat(result.getInstances()).containsExactly(gray);
    }

    /**
     * 测试 router 在指定 canary 但无匹配时回退全部实例
     * 测试目的：验证过滤结果为空时返回所有实例
     * 测试场景：canary=gray，2 个实例（canary=prod、无 metadata）
     * 验证内容：结果返回全部实例
     */
    @Test
    public void testRouterWithCanaryNoMatchFallbackAll() {
        // Arrange
        CanaryRouter router = new CanaryRouter();
        Map<String, String> meta1 = new HashMap<>();
        meta1.put(RouterConstants.CANARY_KEY, "prod");
        Instance prod = buildInstance(meta1);
        Instance empty = buildInstance(null);
        ServiceInstances svcInstances = buildInstances(prod, empty);

        // Act
        RouteResult result = router.router(buildRouteInfo("gray"), svcInstances);

        // Assert
        assertThat(result.getInstances()).containsExactlyInAnyOrder(prod, empty);
    }

    /**
     * 测试 router 在 canary 为空时保留全部实例
     * 测试目的：验证 canaryValue 为空时走 else 分支，保留无 canary 标记及不匹配的实例
     * 测试场景：2 个实例（canary=gray、无 metadata），canary 传空串
     * 验证内容：结果包含全部实例（空串不等于 gray，tagged 也保留）
     */
    @Test
    public void testRouterWithoutCanaryKeepsAll() {
        // Arrange
        CanaryRouter router = new CanaryRouter();
        Map<String, String> meta1 = new HashMap<>();
        meta1.put(RouterConstants.CANARY_KEY, "gray");
        Instance tagged = buildInstance(meta1);
        Instance empty = buildInstance(new HashMap<>());
        ServiceInstances svcInstances = buildInstances(tagged, empty);

        // Act
        RouteResult result = router.router(buildRouteInfo(""), svcInstances);

        // Assert
        assertThat(result.getInstances()).containsExactlyInAnyOrder(tagged, empty);
    }

    /**
     * 测试 getName 返回默认 canary 路由名
     * 测试目的：验证 getName 返回 ServiceRouterConfig.DEFAULT_ROUTER_CANARY
     * 测试场景：调用 getName
     * 验证内容：返回 DEFAULT_ROUTER_CANARY
     */
    @Test
    public void testGetName() {
        // Arrange
        CanaryRouter router = new CanaryRouter();

        // Act & Assert
        assertThat(router.getName()).isEqualTo(ServiceRouterConfig.DEFAULT_ROUTER_CANARY);
    }

    /**
     * 测试 getAspect 返回 MIDDLE
     * 测试目的：验证 CanaryRouter 在路由链中的执行时机
     * 测试场景：调用 getAspect
     * 验证内容：返回 Aspect.MIDDLE
     */
    @Test
    public void testGetAspect() {
        // Arrange
        CanaryRouter router = new CanaryRouter();

        // Act & Assert
        assertThat(router.getAspect()).isEqualTo(com.tencent.polaris.plugins.router.common.AbstractServiceRouter.Aspect.MIDDLE);
    }

    /**
     * 测试 enable 在 dstSvcInfo metadata 含 internal-canary=true 时返回 true
     * 测试目的：验证 enable 受被调服务 internal-canary 元数据控制
     * 测试场景：dstSvcInfo metadata 含 internal-canary=true
     * 验证内容：enable 返回 true
     */
    @Test
    public void testEnableWithCanaryMetadataTrue() {
        // Arrange
        CanaryRouter router = new CanaryRouter();
        RouteInfo routeInfo = mock(RouteInfo.class);
        when(routeInfo.routerIsEnabled(ServiceRouterConfig.DEFAULT_ROUTER_CANARY)).thenReturn(null);
        ServiceMetadata dstSvcInfo = mock(ServiceMetadata.class);
        Map<String, String> meta = new HashMap<>();
        meta.put("internal-canary", "true");
        when(dstSvcInfo.getMetadata()).thenReturn(meta);

        // Act & Assert
        assertThat(router.enable(routeInfo, dstSvcInfo)).isTrue();
    }

    /**
     * 测试 enable 在 dstSvcInfo 无 internal-canary 时返回 false
     * 测试目的：验证被调服务未启用 canary 时 enable 返回 false
     * 测试场景：dstSvcInfo metadata 不含 internal-canary
     * 验证内容：enable 返回 false
     */
    @Test
    public void testEnableWithoutCanaryMetadata() {
        // Arrange
        CanaryRouter router = new CanaryRouter();
        RouteInfo routeInfo = mock(RouteInfo.class);
        when(routeInfo.routerIsEnabled(ServiceRouterConfig.DEFAULT_ROUTER_CANARY)).thenReturn(null);
        ServiceMetadata dstSvcInfo = mock(ServiceMetadata.class);
        when(dstSvcInfo.getMetadata()).thenReturn(new HashMap<String, String>());

        // Act & Assert
        assertThat(router.enable(routeInfo, dstSvcInfo)).isFalse();
    }
}
