/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.polaris.plugins.router.namespace;

import com.tencent.polaris.api.config.consumer.ServiceRouterConfig;
import com.tencent.polaris.api.plugin.route.RouteInfo;
import com.tencent.polaris.api.plugin.route.RouteResult;
import com.tencent.polaris.api.plugin.route.ServiceRouter;
import com.tencent.polaris.api.pojo.*;
import com.tencent.polaris.api.rpc.NamespaceRouterFailoverType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link NamespaceRouterTest}.
 *
 * @author Haotian Zhang
 */
public class NamespaceRouterTest {

    public static String testNamespace1 = "testNamespace1";
    public static String testNamespace2 = "testNamespace2";

    private final NamespaceRouter namespaceRouter = new NamespaceRouter();

    @Test
    public void test1() {
        SourceService sourceService = new SourceService();
        sourceService.setNamespace(testNamespace1);
        RouteInfo routeInfo = new RouteInfo(sourceService, null, null, null);

        List<Instance> instances = new ArrayList<>();
        DefaultBaseInstance instance0 = new DefaultInstance();
        instance0.setNamespace(testNamespace1);
        instance0.setHost("0.0.0.0");
        instances.add((Instance) instance0);
        DefaultBaseInstance instance1 = new DefaultInstance();
        instance1.setNamespace(testNamespace1);
        instance1.setHost("1.1.1.1");
        instances.add((Instance) instance1);
        DefaultBaseInstance instance2 = new DefaultInstance();
        instance2.setNamespace(testNamespace2);
        instance2.setHost("2.2.2.2");
        instances.add((Instance) instance2);

        ServiceInstances serviceInstances = new DefaultServiceInstances(new ServiceKey(), instances);
        RouteResult result = namespaceRouter.router(routeInfo, serviceInstances);
        assertThat(result.getInstances().size()).isEqualTo(2);
        for (Instance instance : result.getInstances()) {
            assertThat(instance.getNamespace()).isEqualTo(testNamespace1);
            assertThat(instance.getHost()).isNotEqualTo("2.2.2.2");
        }
    }

    @Test
    public void test2() {
        SourceService sourceService = new SourceService();
        sourceService.setNamespace(testNamespace1);
        RouteInfo routeInfo = new RouteInfo(sourceService, null, null, null);

        List<Instance> instances = new ArrayList<>();
        DefaultBaseInstance instance0 = new DefaultInstance();
        instance0.setNamespace(testNamespace2);
        instance0.setHost("0.0.0.0");
        instances.add((Instance) instance0);
        DefaultBaseInstance instance1 = new DefaultInstance();
        instance1.setNamespace(testNamespace2);
        instance1.setHost("1.1.1.1");
        instances.add((Instance) instance1);
        DefaultBaseInstance instance2 = new DefaultInstance();
        instance2.setNamespace(testNamespace2);
        instance2.setHost("2.2.2.2");
        instances.add((Instance) instance2);

        ServiceInstances serviceInstances = new DefaultServiceInstances(new ServiceKey(), instances);
        RouteResult result = namespaceRouter.router(routeInfo, serviceInstances);
        assertThat(result.getInstances().size()).isEqualTo(3);
        for (Instance instance : result.getInstances()) {
            assertThat(instance.getNamespace()).isEqualTo(testNamespace2);
        }
    }

    @Test
    public void test3() {
        SourceService sourceService = new SourceService();
        sourceService.setNamespace(testNamespace1);
        RouteInfo routeInfo = new RouteInfo(sourceService, null, null, null);
        routeInfo.setNamespaceRouterFailoverType(NamespaceRouterFailoverType.none);

        List<Instance> instances = new ArrayList<>();
        DefaultBaseInstance instance0 = new DefaultInstance();
        instance0.setNamespace(testNamespace2);
        instance0.setHost("0.0.0.0");
        instances.add((Instance) instance0);
        DefaultBaseInstance instance1 = new DefaultInstance();
        instance1.setNamespace(testNamespace2);
        instance1.setHost("1.1.1.1");
        instances.add((Instance) instance1);
        DefaultBaseInstance instance2 = new DefaultInstance();
        instance2.setNamespace(testNamespace2);
        instance2.setHost("2.2.2.2");
        instances.add((Instance) instance2);

        ServiceInstances serviceInstances = new DefaultServiceInstances(new ServiceKey(), instances);
        RouteResult result = namespaceRouter.router(routeInfo, serviceInstances);
        assertThat(result.getInstances().size()).isEqualTo(0);
    }

    @Test
    public void testAspect() {
        assertThat(namespaceRouter.getAspect()).isEqualTo(ServiceRouter.Aspect.MIDDLE);
    }

    @Test
    public void testName() {
        assertThat(namespaceRouter.getName()).isEqualTo(ServiceRouterConfig.DEFAULT_ROUTER_NAMESPACE);
    }
}
