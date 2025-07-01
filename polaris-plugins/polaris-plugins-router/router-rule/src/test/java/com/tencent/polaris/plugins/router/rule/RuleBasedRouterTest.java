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

package com.tencent.polaris.plugins.router.rule;

import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;
import com.google.protobuf.util.JsonFormat;
import com.tencent.polaris.api.plugin.route.RouteInfo;
import com.tencent.polaris.api.plugin.route.RouteResult;
import com.tencent.polaris.api.pojo.DefaultInstance;
import com.tencent.polaris.api.pojo.DefaultServiceInstances;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.RouteArgument;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.pojo.ServiceRule;
import com.tencent.polaris.api.pojo.SourceService;
import com.tencent.polaris.api.rpc.RuleBasedRouterFailoverType;
import com.tencent.polaris.client.pojo.ServiceRuleByProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.RoutingProto.Routing;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

public class RuleBasedRouterTest {

    /**
     * Makes a new instance of message based on the json and the class
     *
     * @param <T> is the class type
     * @param json is the json instance
     * @param clazz is the class instance
     * @return An instance of T based on the json values
     * @throws IOException if any error occurs
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T extends Message> T fromJson(InputStreamReader json, Class<T> clazz) throws IOException {
        Builder builder = null;
        try {
            // Since we are dealing with a Message type, we can call newBuilder()
            builder = (Builder) clazz.getMethod("newBuilder").invoke(null);

        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            return null;
        }

        // The instance is placed into the builder values
        JsonFormat.parser().ignoringUnknownFields().merge(json, builder);

        // the instance will be from the build
        return (T) builder.build();
    }

    private static Routing loadRouting(String fileName) {
        InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("route-rules/" + fileName);
        if (null == stream) {
            return null;
        }
        try (InputStreamReader reader = new InputStreamReader(stream)) {
            return fromJson(reader, Routing.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static final String ROUTE_TEST_SVC_NAME = "RuleTestService";

    private static final String ROUTE_TEST_NAMESPACE = "Test";

    private List<Instance> mockInstances() {
        List<Instance> instances = new ArrayList<>();
        DefaultInstance v1Instance = new DefaultInstance();
        v1Instance.setNamespace(ROUTE_TEST_NAMESPACE);
        v1Instance.setService(ROUTE_TEST_SVC_NAME);
        v1Instance.setHost("127.0.0.1");
        v1Instance.setPort(20881);
        Map<String, String> v1Meta = new HashMap<>();
        v1Meta.put("version", "2.0.0");
        v1Meta.put("dubbo", "2.0.0");
        v1Meta.put("interface", "cn.polarismesh.com.demo.HelloWorld");
        v1Meta.put("methods", "throwNPE,bid");
        v1Instance.setMetadata(v1Meta);
        instances.add(v1Instance);

        return instances;
    }

    @Test
    public void testMatchLabelsUseIn() {
        Routing routeRule = loadRouting("match-function-in-rule.json");
        Assert.assertNotNull(routeRule);
        ServiceRule rule = new ServiceRuleByProto(routeRule, "1234", false, EventType.ROUTING);
        List<Instance> instances = mockInstances();

        DefaultServiceInstances defaultServiceInstances = new DefaultServiceInstances(
                new ServiceKey(ROUTE_TEST_NAMESPACE, ROUTE_TEST_SVC_NAME), instances);
        RuleBasedRouter ruleBasedRouter = new RuleBasedRouter();
        // first check req:v1, expect all to version:v2
        SourceService v1ServiceInfo = new SourceService();
        Map<String, String> v1ReqMeta = new HashMap<>();
        v1ReqMeta.put("owner", "programmer");
        v1ReqMeta.put("side", "consumer");
        v1ReqMeta.put("methods", "throwNPE,bid");
        v1ReqMeta.put("dubbo", "2.0.0");
        v1ReqMeta.put("pid", "51356");
        v1ReqMeta.put("interface", "cn.polarismesh.com.demo.HelloWorld");
        v1ReqMeta.put("version", "*");
        v1ReqMeta.put("application", "demo-consumer");
        v1ServiceInfo.setMetadata(v1ReqMeta);
        RouteInfo v1RouteInfo = new RouteInfo(
                v1ServiceInfo, null, defaultServiceInstances, rule, "bid", null);
        Map<String, Set<RouteArgument>> argumentsMap = new HashMap<>();
        argumentsMap.put("ruleRouter", new HashSet<>());
        argumentsMap.get("ruleRouter").add(RouteArgument.buildHeader("req", "v1"));
        v1RouteInfo.setRouterArguments(argumentsMap);
        boolean enableV1 = ruleBasedRouter.enable(v1RouteInfo, defaultServiceInstances);
        Assert.assertTrue(enableV1);
        RouteResult result = ruleBasedRouter.router(v1RouteInfo, defaultServiceInstances);
        Assert.assertEquals(1, result.getInstances().size());
    }

    @Test
    public void testMatchLabelsUseNotIn() {
        Routing routeRule = loadRouting("match-function-not_in-rule.json");
        Assert.assertNotNull(routeRule);
        ServiceRule rule = new ServiceRuleByProto(routeRule, "1234", false, EventType.ROUTING);
        List<Instance> instances = mockInstances();

        DefaultServiceInstances defaultServiceInstances = new DefaultServiceInstances(
                new ServiceKey(ROUTE_TEST_NAMESPACE, ROUTE_TEST_SVC_NAME), instances);
        RuleBasedRouter ruleBasedRouter = new RuleBasedRouter();
        // first check req:v1, expect all to version:v2
        SourceService v1ServiceInfo = new SourceService();
        Map<String, String> v1ReqMeta = new HashMap<>();
        v1ReqMeta.put("owner", "programmer");
        v1ReqMeta.put("side", "consumer");
        v1ReqMeta.put("methods", "throwNPE,bid");
        v1ReqMeta.put("dubbo", "2.0.0");
        v1ReqMeta.put("pid", "51356");
        v1ReqMeta.put("interface", "cn.polarismesh.com.demo.HelloWorld");
        v1ReqMeta.put("version", "*");
        v1ReqMeta.put("application", "demo-consumer");
        v1ServiceInfo.setMetadata(v1ReqMeta);
        v1ServiceInfo.appendArguments(RouteArgument.buildHeader("req", "v1_copy"));
        RouteInfo v1RouteInfo = new RouteInfo(
                v1ServiceInfo, null, defaultServiceInstances, rule, "bid", null);
        boolean enableV1 = ruleBasedRouter.enable(v1RouteInfo, defaultServiceInstances);
        Assert.assertTrue(enableV1);
        RouteResult result = ruleBasedRouter.router(v1RouteInfo, defaultServiceInstances);
        Assert.assertEquals(1, result.getInstances().size());
    }

    @Test
    public void testMatchLabelsUseNotEquals() {
        Routing routeRule = loadRouting("match-function-not_equals-rule.json");
        Assert.assertNotNull(routeRule);
        ServiceRule rule = new ServiceRuleByProto(routeRule, "1234", false, EventType.ROUTING);
        List<Instance> instances = mockInstances();

        DefaultServiceInstances defaultServiceInstances = new DefaultServiceInstances(
                new ServiceKey(ROUTE_TEST_NAMESPACE, ROUTE_TEST_SVC_NAME), instances);
        RuleBasedRouter ruleBasedRouter = new RuleBasedRouter();
        // first check req:v1, expect all to version:v2
        SourceService v1ServiceInfo = new SourceService();
        Map<String, String> v1ReqMeta = new HashMap<>();
        v1ReqMeta.put("owner", "programmer");
        v1ReqMeta.put("side", "consumer");
        v1ReqMeta.put("methods", "throwNPE,bid");
        v1ReqMeta.put("dubbo", "2.0.0");
        v1ReqMeta.put("pid", "51356");
        v1ReqMeta.put("interface", "cn.polarismesh.com.demo.HelloWorld");
        v1ReqMeta.put("version", "*");
        v1ReqMeta.put("application", "demo-consumer");
        v1ServiceInfo.setMetadata(v1ReqMeta);
        v1ServiceInfo.appendArguments(RouteArgument.buildHeader("req", "v1_copy"));
        RouteInfo v1RouteInfo = new RouteInfo(
                v1ServiceInfo, null, defaultServiceInstances, rule, "bid", null);
        boolean enableV1 = ruleBasedRouter.enable(v1RouteInfo, defaultServiceInstances);
        Assert.assertTrue(enableV1);
        RouteResult result = ruleBasedRouter.router(v1RouteInfo, defaultServiceInstances);
        Assert.assertEquals(1, result.getInstances().size());
    }

    @Test
    public void testMatchLabelsUseRegx() {
        Routing routeRule = loadRouting("match-function-regx-rule.json");
        Assert.assertNotNull(routeRule);
        ServiceRule rule = new ServiceRuleByProto(routeRule, "1234", false, EventType.ROUTING);
        List<Instance> instances = mockInstances();

        DefaultServiceInstances defaultServiceInstances = new DefaultServiceInstances(
                new ServiceKey(ROUTE_TEST_NAMESPACE, ROUTE_TEST_SVC_NAME), instances);
        RuleBasedRouter ruleBasedRouter = new RuleBasedRouter();
        // first check req:v1, expect all to version:v2
        SourceService v1ServiceInfo = new SourceService();
        Map<String, String> v1ReqMeta = new HashMap<>();
        v1ReqMeta.put("owner", "programmer");
        v1ReqMeta.put("side", "consumer");
        v1ReqMeta.put("methods", "throwNPE,bid");
        v1ReqMeta.put("dubbo", "2.0.0");
        v1ReqMeta.put("pid", "51356");
        v1ReqMeta.put("interface", "cn.polarismesh.com.demo.HelloWorld");
        v1ReqMeta.put("version", "*");
        v1ReqMeta.put("application", "demo-consumer");
        v1ServiceInfo.setMetadata(v1ReqMeta);
        v1ServiceInfo.appendArguments(RouteArgument.buildHeader("req", "polarissssssmesh"));
        RouteInfo v1RouteInfo = new RouteInfo(
                v1ServiceInfo, null, defaultServiceInstances, rule, "bid", null);
        boolean enableV1 = ruleBasedRouter.enable(v1RouteInfo, defaultServiceInstances);
        Assert.assertTrue(enableV1);
        RouteResult result = ruleBasedRouter.router(v1RouteInfo, defaultServiceInstances);
        Assert.assertEquals(1, result.getInstances().size());
    }

    @Test
    public void testMultipleLabels() {
        Routing routeRule = loadRouting("multi-labels-rule.json");
        Assert.assertNotNull(routeRule);
        ServiceRule rule = new ServiceRuleByProto(routeRule, "1234", false, EventType.ROUTING);
        List<Instance> instances = mockInstances();

        DefaultServiceInstances defaultServiceInstances = new DefaultServiceInstances(
                new ServiceKey(ROUTE_TEST_NAMESPACE, ROUTE_TEST_SVC_NAME), instances);
        RuleBasedRouter ruleBasedRouter = new RuleBasedRouter();
        // first check req:v1, expect all to version:v2
        SourceService v1ServiceInfo = new SourceService();
        Map<String, String> v1ReqMeta = new HashMap<>();
        v1ReqMeta.put("owner", "programmer");
        v1ReqMeta.put("side", "consumer");
        v1ReqMeta.put("methods", "throwNPE,bid");
        v1ReqMeta.put("dubbo", "2.0.0");
        v1ReqMeta.put("pid", "51356");
        v1ReqMeta.put("interface", "cn.polarismesh.com.demo.HelloWorld");
        v1ReqMeta.put("version", "*");
        v1ReqMeta.put("application", "demo-consumer");
        v1ReqMeta.put("req", "v1");
        v1ServiceInfo.setMetadata(v1ReqMeta);
        RouteInfo v1RouteInfo = new RouteInfo(
                v1ServiceInfo, null, defaultServiceInstances, rule, "bid", null);
        boolean enableV1 = ruleBasedRouter.enable(v1RouteInfo, defaultServiceInstances);
        Assert.assertTrue(enableV1);
        RouteResult result = ruleBasedRouter.router(v1RouteInfo, defaultServiceInstances);
        Assert.assertEquals(1, result.getInstances().size());
    }

    @Test
    public void testRouteByIsolatedDestination() {
        Routing routeRule = loadRouting("isolated-rule.json");
        Assert.assertNotNull(routeRule);
        ServiceRule rule = new ServiceRuleByProto(routeRule, "1234", false, EventType.ROUTING);
        // build instances
        List<Instance> instances = new ArrayList<>();
        DefaultInstance v1Instance = new DefaultInstance();
        v1Instance.setNamespace(ROUTE_TEST_NAMESPACE);
        v1Instance.setService(ROUTE_TEST_SVC_NAME);
        v1Instance.setHost("127.0.0.1");
        v1Instance.setPort(1015);
        Map<String, String> v1Meta = new HashMap<>();
        v1Meta.put("version", "v1");
        v1Instance.setMetadata(v1Meta);
        instances.add(v1Instance);
        DefaultInstance v2Instance = new DefaultInstance();
        v1Instance.setNamespace(ROUTE_TEST_NAMESPACE);
        v1Instance.setService(ROUTE_TEST_SVC_NAME);
        v1Instance.setHost("127.0.0.1");
        v1Instance.setPort(1016);
        Map<String, String> v2Meta = new HashMap<>();
        v2Meta.put("version", "v2");
        v2Instance.setMetadata(v2Meta);
        instances.add(v2Instance);
        DefaultServiceInstances defaultServiceInstances = new DefaultServiceInstances(
                new ServiceKey(ROUTE_TEST_NAMESPACE, ROUTE_TEST_SVC_NAME), instances);
        RuleBasedRouter ruleBasedRouter = new RuleBasedRouter();
        RuleBasedRouterConfig routerConfig = new RuleBasedRouterConfig();
        routerConfig.setFailoverType(RuleBasedRouterFailoverType.none);
        ruleBasedRouter.setRouterConfig(routerConfig);
        // first check req:v1, expect all to version:v2
        SourceService v1ServiceInfo = new SourceService();
        Map<String, String> v1ReqMeta = new HashMap<>();
        v1ReqMeta.put("req", "v1");
        v1ServiceInfo.setMetadata(v1ReqMeta);
        RouteInfo v1RouteInfo = new RouteInfo(
                v1ServiceInfo, null, defaultServiceInstances, rule, null, null);
        boolean enableV1 = ruleBasedRouter.enable(v1RouteInfo, defaultServiceInstances);
        Assert.assertTrue(enableV1);
        for (int i = 0; i < 10; i++) {
            RouteResult result = ruleBasedRouter.router(v1RouteInfo, defaultServiceInstances);
            checkResultMetadata(result, "version", "v2");
        }

        // first check req:v2, expect all to empty
        SourceService v2ServiceInfo = new SourceService();
        Map<String, String> v2ReqMeta = new HashMap<>();
        v2ReqMeta.put("req", "v2");
        v2ServiceInfo.setMetadata(v2ReqMeta);
        RouteInfo v2RouteInfo = new RouteInfo(
                v2ServiceInfo, null, defaultServiceInstances, rule, null, null);
        boolean enableV2 = ruleBasedRouter.enable(v1RouteInfo, defaultServiceInstances);
        Assert.assertTrue(enableV2);
        for (int i = 0; i < 10; i++) {
            RouteResult result = ruleBasedRouter.router(v2RouteInfo, defaultServiceInstances);
            Assert.assertTrue(result.getInstances().isEmpty());
        }
    }

    @Test
    public void testFailoverAll() {
        Routing routeRule = loadRouting("isolated-rule.json");
        Assert.assertNotNull(routeRule);
        ServiceRule rule = new ServiceRuleByProto(routeRule, "1234", false, EventType.ROUTING);
        // build instances
        List<Instance> instances = new ArrayList<>();
        DefaultInstance v1Instance = new DefaultInstance();
        v1Instance.setNamespace(ROUTE_TEST_NAMESPACE);
        v1Instance.setService(ROUTE_TEST_SVC_NAME);
        v1Instance.setHost("127.0.0.1");
        v1Instance.setPort(1015);
        Map<String, String> v1Meta = new HashMap<>();
        v1Meta.put("version", "v1");
        v1Instance.setMetadata(v1Meta);
        instances.add(v1Instance);
        DefaultInstance v2Instance = new DefaultInstance();
        v1Instance.setNamespace(ROUTE_TEST_NAMESPACE);
        v1Instance.setService(ROUTE_TEST_SVC_NAME);
        v1Instance.setHost("127.0.0.1");
        v1Instance.setPort(1016);
        Map<String, String> v2Meta = new HashMap<>();
        v2Meta.put("version", "v2");
        v2Instance.setMetadata(v2Meta);
        instances.add(v2Instance);
        DefaultServiceInstances defaultServiceInstances = new DefaultServiceInstances(
                new ServiceKey(ROUTE_TEST_NAMESPACE, ROUTE_TEST_SVC_NAME), instances);
        RuleBasedRouter ruleBasedRouter = new RuleBasedRouter();
        RuleBasedRouterConfig routerConfig = new RuleBasedRouterConfig();
        routerConfig.setFailoverType(RuleBasedRouterFailoverType.all);
        ruleBasedRouter.setRouterConfig(routerConfig);
        // first check req:v1, expect all to version:v2
        SourceService v1ServiceInfo = new SourceService();
        Map<String, String> v1ReqMeta = new HashMap<>();
        v1ReqMeta.put("req", "v1");
        v1ServiceInfo.setMetadata(v1ReqMeta);
        RouteInfo v1RouteInfo = new RouteInfo(
                v1ServiceInfo, null, defaultServiceInstances, rule, null, null);
        boolean enableV1 = ruleBasedRouter.enable(v1RouteInfo, defaultServiceInstances);
        Assert.assertTrue(enableV1);
        for (int i = 0; i < 10; i++) {
            RouteResult result = ruleBasedRouter.router(v1RouteInfo, defaultServiceInstances);
            checkResultMetadata(result, "version", "v2");
        }

        // first check req:v2, expect all to empty
        SourceService v2ServiceInfo = new SourceService();
        Map<String, String> v2ReqMeta = new HashMap<>();
        v2ReqMeta.put("req", "v2");
        v2ServiceInfo.setMetadata(v2ReqMeta);
        RouteInfo v2RouteInfo = new RouteInfo(
                v2ServiceInfo, null, defaultServiceInstances, rule, null, null);
        boolean enableV2 = ruleBasedRouter.enable(v1RouteInfo, defaultServiceInstances);
        Assert.assertTrue(enableV2);
        for (int i = 0; i < 10; i++) {
            RouteResult result = ruleBasedRouter.router(v2RouteInfo, defaultServiceInstances);
            Assert.assertEquals(2, result.getInstances().size());
        }
    }

    private static void checkResultMetadata(RouteResult result, String key, String value) {
        Assert.assertFalse(result.getInstances().isEmpty());
        for (Instance instance : result.getInstances()) {
            String instValue = instance.getMetadata().get(key);
            Assert.assertEquals(value, instValue);
        }
    }
}
