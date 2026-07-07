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

package com.tencent.polaris.plugins.router.isolated;

import java.util.Arrays;
import java.util.List;

import com.tencent.polaris.api.config.consumer.ServiceRouterConfig;
import com.tencent.polaris.api.plugin.route.RouteInfo;
import com.tencent.polaris.api.plugin.route.RouteResult;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.plugins.router.common.AbstractServiceRouter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link IsolatedRouter}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class IsolatedRouterTest {

    private Instance buildInstance(int weight, boolean isolated) {
        Instance instance = mock(Instance.class);
        when(instance.getWeight()).thenReturn(weight);
        when(instance.isIsolated()).thenReturn(isolated);
        return instance;
    }

    private ServiceInstances buildInstances(Instance... instances) {
        ServiceInstances svcInstances = mock(ServiceInstances.class);
        when(svcInstances.getInstances()).thenReturn(Arrays.asList(instances));
        return svcInstances;
    }

    /**
     * 测试 router 过滤掉 weight=0 和 isolated 的实例
     * 测试目的：验证 router 剔除不可用实例，保留健康实例
     * 测试场景：构造 4 个实例：正常、weight=0、isolated、正常
     * 验证内容：结果保留 2 个正常实例
     */
    @Test
    public void testRouterFilterUnhealthy() {
        // Arrange
        IsolatedRouter router = new IsolatedRouter();
        RouteInfo routeInfo = mock(RouteInfo.class);
        Instance healthy1 = buildInstance(100, false);
        Instance zeroWeight = buildInstance(0, false);
        Instance isolated = buildInstance(100, true);
        Instance healthy2 = buildInstance(50, false);
        ServiceInstances svcInstances = buildInstances(healthy1, zeroWeight, isolated, healthy2);

        // Act
        RouteResult result = router.router(routeInfo, svcInstances);

        // Assert
        assertThat(result.getInstances()).containsExactlyInAnyOrder(healthy1, healthy2);
    }

    /**
     * 测试 router 全部实例不可用时返回空列表
     * 测试目的：验证所有实例都被过滤时返回空结果
     * 测试场景：构造 2 个实例：weight=0、isolated
     * 验证内容：结果为空列表
     */
    @Test
    public void testRouterAllUnhealthy() {
        // Arrange
        IsolatedRouter router = new IsolatedRouter();
        RouteInfo routeInfo = mock(RouteInfo.class);
        Instance zeroWeight = buildInstance(0, false);
        Instance isolated = buildInstance(100, true);
        ServiceInstances svcInstances = buildInstances(zeroWeight, isolated);

        // Act
        RouteResult result = router.router(routeInfo, svcInstances);

        // Assert
        assertThat(result.getInstances()).isEmpty();
    }

    /**
     * 测试 router 全部实例健康时全部保留
     * 测试目的：验证所有实例健康时全部保留
     * 测试场景：构造 2 个健康实例
     * 验证内容：结果包含 2 个实例
     */
    @Test
    public void testRouterAllHealthy() {
        // Arrange
        IsolatedRouter router = new IsolatedRouter();
        RouteInfo routeInfo = mock(RouteInfo.class);
        Instance healthy1 = buildInstance(100, false);
        Instance healthy2 = buildInstance(80, false);
        ServiceInstances svcInstances = buildInstances(healthy1, healthy2);

        // Act
        RouteResult result = router.router(routeInfo, svcInstances);

        // Assert
        assertThat(result.getInstances()).containsExactlyInAnyOrder(healthy1, healthy2);
    }

    /**
     * 测试 getAspect 返回 BEFORE
     * 测试目的：验证 IsolatedRouter 在路由链中的执行时机
     * 测试场景：调用 getAspect
     * 验证内容：返回 Aspect.BEFORE
     */
    @Test
    public void testGetAspect() {
        // Arrange
        IsolatedRouter router = new IsolatedRouter();

        // Act & Assert
        assertThat(router.getAspect()).isEqualTo(AbstractServiceRouter.Aspect.BEFORE);
    }

    /**
     * 测试 enable 始终返回 true
     * 测试目的：验证 IsolatedRouter 默认启用
     * 测试场景：调用 enable
     * 验证内容：返回 true
     */
    @Test
    public void testEnableAlwaysTrue() {
        // Arrange
        IsolatedRouter router = new IsolatedRouter();
        RouteInfo routeInfo = mock(RouteInfo.class);

        // Act & Assert
        assertThat(router.enable(routeInfo, null)).isTrue();
    }

    /**
     * 测试 getName 返回默认 isolated 路由名
     * 测试目的：验证 getName 返回 ServiceRouterConfig.DEFAULT_ROUTER_ISOLATED
     * 测试场景：调用 getName
     * 验证内容：返回 DEFAULT_ROUTER_ISOLATED
     */
    @Test
    public void testGetName() {
        // Arrange
        IsolatedRouter router = new IsolatedRouter();

        // Act & Assert
        assertThat(router.getName()).isEqualTo(ServiceRouterConfig.DEFAULT_ROUTER_ISOLATED);
    }

    /**
     * 测试 init 不抛异常
     * 测试目的：验证 init 空实现调用安全
     * 测试场景：调用 init(null)
     * 验证内容：不抛异常
     */
    @Test
    public void testInitDoesNotThrow() {
        // Arrange
        IsolatedRouter router = new IsolatedRouter();

        // Act
        router.init(null);

        // 验证 init 无异常即通过
        assertThat(router).isNotNull();
    }

    /**
     * 测试 router 对空实例列表的处理
     * 测试目的：验证无实例时返回空结果
     * 测试场景：ServiceInstances 返回空列表
     * 验证内容：结果为空
     */
    @Test
    public void testRouterEmptyInstances() {
        // Arrange
        IsolatedRouter router = new IsolatedRouter();
        RouteInfo routeInfo = mock(RouteInfo.class);
        ServiceInstances svcInstances = mock(ServiceInstances.class);
        when(svcInstances.getInstances()).thenReturn(new java.util.ArrayList<Instance>());

        // Act
        RouteResult result = router.router(routeInfo, svcInstances);

        // Assert
        assertThat(result.getInstances()).isEmpty();
    }

    /**
     * 测试 RouteResult 状态为 Next
     * 测试目的：验证 router 返回的结果状态为 Next
     * 测试场景：任意输入调用 router
     * 验证内容：getNextRouterInfo 的 state 为 Next
     */
    @Test
    public void testRouterResultStateNext() {
        // Arrange
        IsolatedRouter router = new IsolatedRouter();
        RouteInfo routeInfo = mock(RouteInfo.class);
        Instance healthy = buildInstance(100, false);
        ServiceInstances svcInstances = buildInstances(healthy);

        // Act
        RouteResult result = router.router(routeInfo, svcInstances);

        // Assert
        assertThat(result.getNextRouterInfo().getState()).isEqualTo(RouteResult.State.Next);
    }
}
