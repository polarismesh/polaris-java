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

package com.tencent.polaris.plugins.router.lane;

import com.google.protobuf.StringValue;
import com.tencent.polaris.api.config.consumer.ServiceRouterConfig;
import com.tencent.polaris.api.plugin.route.RouteInfo;
import com.tencent.polaris.api.pojo.RouteArgument;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.metadata.core.MessageMetadataContainer;
import com.tencent.polaris.metadata.core.MetadataContainer;
import com.tencent.polaris.metadata.core.MetadataType;
import com.tencent.polaris.metadata.core.manager.MetadataContext;
import com.tencent.polaris.specification.api.v1.model.ModelProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.LaneProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.RoutingProto;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Test for matching traffic against ordered lane rules.
 *
 * @author fuyuwei01
 */
public class LaneTrafficMatchingTest {

    private static final String GROUP = "tsf";

    private final MetadataContext manager = Mockito.mock(MetadataContext.class);

    private final MessageMetadataContainer message = Mockito.mock(MessageMetadataContainer.class);

    private final RouteInfo routeInfo = Mockito.mock(RouteInfo.class);

    @Before
    public void setUp() {
        MetadataContainer custom = Mockito.mock(MetadataContainer.class);
        Mockito.when(manager.getMetadataContainer(MetadataType.MESSAGE, false)).thenReturn(message);
        Mockito.when(manager.getMetadataContainer(MetadataType.MESSAGE, true)).thenReturn(message);
        Mockito.when(manager.getMetadataContainer(MetadataType.CUSTOM, false)).thenReturn(custom);
        Mockito.when(message.getHeader("region")).thenReturn("east");
        Mockito.when(message.getHeader("tenant")).thenReturn("enterprise");
        Mockito.when(routeInfo.getRouterMetadata(ServiceRouterConfig.DEFAULT_ROUTER_LANE))
                .thenReturn(Collections.emptyMap());
        Mockito.when(routeInfo.getExternalParameterSupplier()).thenReturn(key -> Optional.of("east"));
    }

    /**
     * 验证 AND 条件全部满足时选择最新规则，任一条件失败时继续匹配旧规则。
     */
    @Test
    public void testAndRuleFallsBackToOlderMatchingRule() {
        LaneProto.LaneRule newest = rule("new", "2024-06-01 00:00:00", LaneProto.TrafficMatchRule.TrafficMatchMode.AND,
                argument("region", "east", false), argument("tenant", "premium", false));
        LaneProto.LaneRule older = rule("old", "2024-01-01 00:00:00", LaneProto.TrafficMatchRule.TrafficMatchMode.AND,
                argument("region", "east", false), argument("tenant", "enterprise", false));
        LaneRuleContainer container = container(newest, older);

        Assertions.assertThat(container.matchRule(manager).get().getName()).isEqualTo("old");
        Assertions.assertThat(container.matchRule(routeInfo, manager).get().getName()).isEqualTo("old");
        Mockito.when(message.getHeader("tenant")).thenReturn("premium");
        Assertions.assertThat(container.matchRule(routeInfo, manager).get().getName()).isEqualTo("new");
    }

    /**
     * 验证 OR 条件部分满足即可匹配，全部不满足时不选择泳道。
     */
    @Test
    public void testOrRuleRequiresAtLeastOneMatch() {
        LaneRuleContainer container = container(rule("canary", "2024-06-01 00:00:00",
                LaneProto.TrafficMatchRule.TrafficMatchMode.OR, argument("region", "west", false),
                argument("tenant", "enterprise", false)));

        Assertions.assertThat(container.matchRule(manager)).isPresent();
        Assertions.assertThat(container.matchRule(routeInfo, manager)).isPresent();
        Mockito.when(message.getHeader("tenant")).thenReturn("standard");
        Assertions.assertThat(container.matchRule(manager)).isEmpty();
        Assertions.assertThat(container.matchRule(routeInfo, manager)).isEmpty();
    }

    /**
     * 验证外部变量由请求供应器解析，供应器返回的匹配值可命中规则。
     */
    @Test
    public void testVariableUsesExternalParameter() {
        LaneRuleContainer container = container(rule("variable", "2024-06-01 00:00:00",
                LaneProto.TrafficMatchRule.TrafficMatchMode.AND, argument("region", "EXPECTED_REGION", true)));

        Assertions.assertThat(container.matchRule(routeInfo, manager)).isPresent();
        Mockito.verify(routeInfo).getExternalParameterSupplier();
    }

    /**
     * 验证请求显式参数优先于线程上下文，染色标签只匹配已启用规则。
     */
    @Test
    public void testRequestOverrideAndStainLookup() {
        LaneProto.LaneRule enabled = rule("active", "2024-06-01 00:00:00",
                LaneProto.TrafficMatchRule.TrafficMatchMode.AND, argument("region", "west", false));
        LaneProto.LaneRule disabled = enabled.toBuilder().setName("disabled").setEnable(false).build();
        LaneRuleContainer container = container(disabled, enabled);
        Mockito.when(routeInfo.getRouterMetadata(ServiceRouterConfig.DEFAULT_ROUTER_LANE))
                .thenReturn(Collections.singletonMap(RouteArgument.ArgumentType.HEADER.key("region"), "west"));

        Assertions.assertThat(container.matchRule(routeInfo, manager).get().getName()).isEqualTo("active");
        Assertions.assertThat(container.matchRule(manager)).isEmpty();
        Assertions.assertThat(container.matchRule("tsf/active")).contains(enabled);
        Assertions.assertThat(container.matchRule("tsf/disabled")).isEmpty();
    }

    private LaneRuleContainer container(LaneProto.LaneRule... rules) {
        LaneProto.LaneGroup group = LaneProto.LaneGroup.newBuilder().setName(GROUP)
                .addAllRules(Arrays.asList(rules)).build();
        return new LaneRuleContainer(manager, new ServiceKey("production", "checkout"),
                Collections.singletonList(group));
    }

    private LaneProto.LaneRule rule(String name, String time, LaneProto.TrafficMatchRule.TrafficMatchMode mode,
            RoutingProto.SourceMatch... arguments) {
        LaneProto.TrafficMatchRule match = LaneProto.TrafficMatchRule.newBuilder().setMatchMode(mode)
                .addAllArguments(Arrays.asList(arguments)).build();
        return LaneProto.LaneRule.newBuilder().setName(name).setGroupName(GROUP).setEnable(true).setCtime(time)
                .setTrafficMatchRule(match).build();
    }

    private RoutingProto.SourceMatch argument(String key, String value, boolean variable) {
        ModelProto.MatchString match = ModelProto.MatchString.newBuilder()
                .setType(ModelProto.MatchString.MatchStringType.EXACT)
                .setValueType(variable ? ModelProto.MatchString.ValueType.VARIABLE
                        : ModelProto.MatchString.ValueType.TEXT)
                .setValue(StringValue.of(value)).build();
        return RoutingProto.SourceMatch.newBuilder().setType(RoutingProto.SourceMatch.Type.HEADER)
                .setKey(key).setValue(match).build();
    }
}
