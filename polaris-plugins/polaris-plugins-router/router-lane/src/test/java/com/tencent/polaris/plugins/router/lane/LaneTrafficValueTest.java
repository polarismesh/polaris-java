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

import com.tencent.polaris.api.config.consumer.ServiceRouterConfig;
import com.tencent.polaris.api.plugin.route.RouteInfo;
import com.tencent.polaris.api.pojo.RouteArgument;
import com.tencent.polaris.metadata.core.MessageMetadataContainer;
import com.tencent.polaris.metadata.core.MetadataContainer;
import com.tencent.polaris.metadata.core.MetadataType;
import com.tencent.polaris.metadata.core.manager.MetadataContext;
import com.tencent.polaris.specification.api.v1.traffic.manage.RoutingProto;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;

/**
 * Test for resolving traffic arguments from request and metadata.
 *
 * @author fuyuwei01
 */
@RunWith(Parameterized.class)
public class LaneTrafficValueTest {

    private final RoutingProto.SourceMatch.Type sourceType;

    private final RouteArgument.ArgumentType argumentType;

    public LaneTrafficValueTest(RoutingProto.SourceMatch.Type sourceType, RouteArgument.ArgumentType argumentType) {
        this.sourceType = sourceType;
        this.argumentType = argumentType;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][] {
                {RoutingProto.SourceMatch.Type.HEADER, RouteArgument.ArgumentType.HEADER},
                {RoutingProto.SourceMatch.Type.CUSTOM, RouteArgument.ArgumentType.CUSTOM},
                {RoutingProto.SourceMatch.Type.METHOD, RouteArgument.ArgumentType.METHOD},
                {RoutingProto.SourceMatch.Type.CALLER_IP, RouteArgument.ArgumentType.CALLER_IP},
                {RoutingProto.SourceMatch.Type.COOKIE, RouteArgument.ArgumentType.COOKIE},
                {RoutingProto.SourceMatch.Type.QUERY, RouteArgument.ArgumentType.QUERY},
                {RoutingProto.SourceMatch.Type.PATH, RouteArgument.ArgumentType.PATH},
        });
    }

    /**
     * 验证每种参数类型优先使用显式请求值，其次使用被调元数据，最后回退主调元数据。
     */
    @Test
    public void testRequestAndMetadataPrecedence() {
        MetadataContext manager = Mockito.mock(MetadataContext.class);
        MessageMetadataContainer callee = Mockito.mock(MessageMetadataContainer.class);
        MessageMetadataContainer caller = Mockito.mock(MessageMetadataContainer.class);
        MetadataContainer custom = Mockito.mock(MetadataContainer.class);
        Mockito.when(manager.getMetadataContainer(MetadataType.MESSAGE, false)).thenReturn(callee);
        Mockito.when(manager.getMetadataContainer(MetadataType.MESSAGE, true)).thenReturn(caller);
        Mockito.when(manager.getMetadataContainer(MetadataType.CUSTOM, false)).thenReturn(custom);
        RouteInfo routeInfo = Mockito.mock(RouteInfo.class);
        RoutingProto.SourceMatch source = RoutingProto.SourceMatch.newBuilder().setType(sourceType)
                .setKey("tenant").build();
        Mockito.when(routeInfo.getRouterMetadata(ServiceRouterConfig.DEFAULT_ROUTER_LANE))
                .thenReturn(Collections.singletonMap(argumentType.key("tenant"), "request-value"));
        Assertions.assertThat(LaneUtils.findTrafficValue(routeInfo, source, manager)).isEqualTo("request-value");

        Mockito.when(routeInfo.getRouterMetadata(ServiceRouterConfig.DEFAULT_ROUTER_LANE))
                .thenReturn(Collections.emptyMap());
        stubValue(callee, "callee-value");
        stubValue(caller, "caller-value");
        Mockito.when(custom.getRawMetadataStringValue("tenant")).thenReturn("callee-value");
        Assertions.assertThat(LaneUtils.findTrafficValue(routeInfo, source, manager)).isEqualTo("callee-value");
        Assertions.assertThat(LaneUtils.findTrafficValue(source, manager)).isEqualTo("callee-value");

        stubValue(callee, null);
        Mockito.when(custom.getRawMetadataStringValue("tenant")).thenReturn(null);
        String expected = sourceType == RoutingProto.SourceMatch.Type.CUSTOM ? "" : "caller-value";
        Assertions.assertThat(LaneUtils.findTrafficValue(routeInfo, source, manager)).isEqualTo(expected);
        Assertions.assertThat(LaneUtils.findTrafficValue(source, manager)).isEqualTo(expected);
    }

    private void stubValue(MessageMetadataContainer container, String value) {
        switch (sourceType) {
            case HEADER:
                Mockito.when(container.getHeader("tenant")).thenReturn(value);
                break;
            case METHOD:
                Mockito.when(container.getMethod()).thenReturn(value);
                break;
            case CALLER_IP:
                Mockito.when(container.getCallerIP()).thenReturn(value);
                break;
            case COOKIE:
                Mockito.when(container.getCookie("tenant")).thenReturn(value);
                break;
            case QUERY:
                Mockito.when(container.getQuery("tenant")).thenReturn(value);
                break;
            case PATH:
                Mockito.when(container.getPath()).thenReturn(value);
                break;
            default:
                break;
        }
    }
}
