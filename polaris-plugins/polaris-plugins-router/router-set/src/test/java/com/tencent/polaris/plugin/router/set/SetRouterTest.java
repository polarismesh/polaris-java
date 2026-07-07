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

package com.tencent.polaris.plugin.router.set;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.tencent.polaris.api.config.consumer.ServiceRouterConfig;
import com.tencent.polaris.api.plugin.route.RouteInfo;
import com.tencent.polaris.api.plugin.route.RouteResult;
import com.tencent.polaris.api.plugin.route.RouterConstants;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInstances;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link SetRouter}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class SetRouterTest {

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

    /**
     * 测试 router 在 destService 为 null 时返回全部实例
     * 测试目的：验证未指定被调服务元数据时直接返回所有实例
     * 测试场景：routeInfo.getDestService 返回 null，2 个实例
     * 验证内容：结果包含全部实例
     */
    @Test
    public void testRouterDestServiceNullReturnAll() {
        // Arrange
        SetRouter router = new SetRouter();
        RouteInfo routeInfo = mock(RouteInfo.class);
        when(routeInfo.getDestService()).thenReturn(null);
        Instance inst1 = buildInstance(new HashMap<>());
        Instance inst2 = buildInstance(new HashMap<>());
        ServiceInstances svcInstances = buildInstances(inst1, inst2);

        // Act
        RouteResult result = router.router(routeInfo, svcInstances);

        // Assert
        assertThat(result.getInstances()).containsExactlyInAnyOrder(inst1, inst2);
    }

    /**
     * 测试 router 在被调 set 启用且匹配时返回匹配实例
     * 测试目的：验证被调启用 set 且 setName 匹配时过滤出对应实例
     * 测试场景：destService metadata 含 internal-enable-set=Y、internal-set-name=a.b.c，实例一个匹配一个不匹配
     * 验证内容：结果只含匹配的实例
     */
    @Test
    public void testRouterDestSetEnabledMatch() {
        // Arrange
        SetRouter router = new SetRouter();
        Map<String, String> destMeta = new HashMap<>();
        destMeta.put(RouterConstants.SET_ENABLE_KEY, RouterConstants.SET_ENABLED);
        destMeta.put(RouterConstants.SET_NAME_KEY, "a.b.c");
        com.tencent.polaris.api.pojo.ServiceMetadata destService = mock(com.tencent.polaris.api.pojo.ServiceMetadata.class);
        when(destService.getMetadata()).thenReturn(destMeta);
        RouteInfo routeInfo = mock(RouteInfo.class);
        when(routeInfo.getDestService()).thenReturn(destService);

        Map<String, String> matchMeta = new HashMap<>();
        matchMeta.put(RouterConstants.SET_ENABLE_KEY, RouterConstants.SET_ENABLED);
        matchMeta.put(RouterConstants.SET_NAME_KEY, "a.b.c");
        Map<String, String> unmatchMeta = new HashMap<>();
        unmatchMeta.put(RouterConstants.SET_ENABLE_KEY, RouterConstants.SET_ENABLED);
        unmatchMeta.put(RouterConstants.SET_NAME_KEY, "x.y.z");
        Instance match = buildInstance(matchMeta);
        Instance unmatch = buildInstance(unmatchMeta);
        ServiceInstances svcInstances = buildInstances(match, unmatch);

        // Act
        RouteResult result = router.router(routeInfo, svcInstances);

        // Assert
        assertThat(result.getInstances()).containsExactly(match);
    }

    /**
     * 测试 groupIsLike 对三段带 * 的 setName 返回 true
     * 测试目的：验证 groupIsLike 识别 "地区.机房.*" 形式的模糊匹配
     * 测试场景：实例 metadata 的 internal-set-name = a.b.*
     * 验证内容：groupIsLike 返回 true
     */
    @Test
    public void testGroupIsLikeWithWildcard() {
        // Arrange
        SetRouter router = new SetRouter();
        Map<String, String> meta = new HashMap<>();
        meta.put(RouterConstants.SET_NAME_KEY, "a.b.*");
        Instance instance = buildInstance(meta);

        // Act & Assert
        assertThat(router.groupIsLike(instance)).isTrue();
    }

    /**
     * 测试 groupIsLike 对完整三段 setName 返回 false
     * 测试目的：验证 groupIsLike 对非模糊匹配（无 *）返回 false
     * 测试场景：实例 metadata 的 internal-set-name = a.b.c
     * 验证内容：groupIsLike 返回 false
     */
    @Test
    public void testGroupIsLikeWithoutWildcard() {
        // Arrange
        SetRouter router = new SetRouter();
        Map<String, String> meta = new HashMap<>();
        meta.put(RouterConstants.SET_NAME_KEY, "a.b.c");
        Instance instance = buildInstance(meta);

        // Act & Assert
        assertThat(router.groupIsLike(instance)).isFalse();
    }

    /**
     * 测试 groupIsLike 对 null metadata 返回 false
     * 测试目的：验证实例无 metadata 时 groupIsLike 返回 false
     * 测试场景：实例 metadata 为 null
     * 验证内容：groupIsLike 返回 false
     */
    @Test
    public void testGroupIsLikeWithNullMetadata() {
        // Arrange
        SetRouter router = new SetRouter();
        Instance instance = buildInstance(null);

        // Act & Assert
        assertThat(router.groupIsLike(instance)).isFalse();
    }

    /**
     * 测试 groupIsLike 对两段 setName 返回 false
     * 测试目的：验证 groupIsLike 仅对三段且末段为 * 返回 true
     * 测试场景：internal-set-name = a.b
     * 验证内容：groupIsLike 返回 false
     */
    @Test
    public void testGroupIsLikeWithTwoSegments() {
        // Arrange
        SetRouter router = new SetRouter();
        Map<String, String> meta = new HashMap<>();
        meta.put(RouterConstants.SET_NAME_KEY, "a.b");
        Instance instance = buildInstance(meta);

        // Act & Assert
        assertThat(router.groupIsLike(instance)).isFalse();
    }

    /**
     * 测试 getName 返回默认 set 路由名
     * 测试目的：验证 getName 返回 ServiceRouterConfig.DEFAULT_ROUTER_SET
     * 测试场景：调用 getName
     * 验证内容：返回 DEFAULT_ROUTER_SET
     */
    @Test
    public void testGetName() {
        // Arrange
        SetRouter router = new SetRouter();

        // Act & Assert
        assertThat(router.getName()).isEqualTo(ServiceRouterConfig.DEFAULT_ROUTER_SET);
    }

    /**
     * 测试 getAspect 返回 MIDDLE
     * 测试目的：验证 SetRouter 在路由链中的执行时机
     * 测试场景：调用 getAspect
     * 验证内容：返回 Aspect.MIDDLE
     */
    @Test
    public void testGetAspect() {
        // Arrange
        SetRouter router = new SetRouter();

        // Act & Assert
        assertThat(router.getAspect()).isEqualTo(com.tencent.polaris.plugins.router.common.AbstractServiceRouter.Aspect.MIDDLE);
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
        SetRouter router = new SetRouter();

        // Act
        router.init(null);

        // 验证 init 无异常即通过
        assertThat(router).isNotNull();
    }
}
