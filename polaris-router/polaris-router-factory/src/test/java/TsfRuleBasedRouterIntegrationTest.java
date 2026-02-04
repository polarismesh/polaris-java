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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.protobuf.StringValue;
import com.google.protobuf.UInt32Value;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.plugin.route.RouterConstants;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.GetAllInstancesRequest;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.api.utils.RuleUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.api.BaseEngine;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.client.pojo.Node;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import com.tencent.polaris.factory.api.RouterAPIFactory;
import com.tencent.polaris.metadata.core.MetadataContainer;
import com.tencent.polaris.metadata.core.MetadataType;
import com.tencent.polaris.metadata.core.TransitiveType;
import com.tencent.polaris.metadata.core.manager.MetadataContext;
import com.tencent.polaris.metadata.core.manager.MetadataContextHolder;
import com.tencent.polaris.plugins.connector.consul.service.common.TagConstant;
import com.tencent.polaris.plugins.connector.consul.service.router.RouterUtils;
import com.tencent.polaris.plugins.connector.consul.service.router.entity.RouteTag;
import com.tencent.polaris.router.api.core.RouterAPI;
import com.tencent.polaris.router.api.rpc.ProcessRoutersRequest;
import com.tencent.polaris.router.api.rpc.ProcessRoutersResponse;
import com.tencent.polaris.specification.api.v1.model.ModelProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.RoutingProto;
import com.tencent.polaris.test.common.TestUtils;
import com.tencent.polaris.test.mock.discovery.NamingServer;
import com.tencent.polaris.test.mock.discovery.NamingService.InstanceParameter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static com.tencent.polaris.metadata.core.constant.TsfMetadataConstants.TSF_APPLICATION_ID;
import static com.tencent.polaris.metadata.core.constant.TsfMetadataConstants.TSF_GROUP_ID;
import static com.tencent.polaris.metadata.core.constant.TsfMetadataConstants.TSF_NAMESPACE_ID;

/**
 * RuleBasedRouter in TSF 集成测试
 *
 * 场景说明：
 * - caller (主调服务) 作为主调服务
 * - callee (被调服务) 作为被调服务
 */
public class TsfRuleBasedRouterIntegrationTest {

    private static final String POLARIS_SERVER_ADDRESS_PROPERTY = "POLARIS_SEVER_ADDRESS";
    private static final String NAMESPACE = "namespace-xxx";
    private static final String CALLER_SERVICE = "CallerService";  // 主调服务
    private static final String CALLEE_SERVICE = "CalleeService";  // 被调服务

    private NamingServer namingServer;
    private SDKContext sdkContext;
    private RouterAPI routerAPI;
    private ConsumerAPI consumerAPI;
    private ServiceKey callerServiceKey;  // 主调服务 key
    private ServiceKey calleeServiceKey;  // 被调服务 key

    private String callerApplicationId = "application-id-caller-1";
    private String callerGroupId = "group-id-caller-1";
    private String insCalleeApplicationId = "application-id-callee-1";
    private String ruleCalleeApplicationId = insCalleeApplicationId;
    private String insCalleeGroupId1 = "group-id-callee-1";
    private String insCalleeGroupId2 = "group-id-callee-2";
    private String ruleCalleeGroupId = insCalleeGroupId1;

    @Before
    public void setUp() {
        try {
            // 1. 启动 Mock NamingServer
            namingServer = NamingServer.startNamingServer(-1);
            System.setProperty(POLARIS_SERVER_ADDRESS_PROPERTY, String.format("127.0.0.1:%d", namingServer.getPort()));

            // 2. 创建 SDK 上下文和 API
            Configuration configuration = TestUtils.configWithEnvAddress();

            sdkContext = SDKContext.initContextByConfig(configuration);
            routerAPI = RouterAPIFactory.createRouterAPIByContext(sdkContext);
            consumerAPI = DiscoveryAPIFactory.createConsumerAPIByContext(sdkContext);

            // 3. 初始化服务 Key
            callerServiceKey = new ServiceKey(NAMESPACE, CALLER_SERVICE);
            calleeServiceKey = new ServiceKey(NAMESPACE, CALLEE_SERVICE);

            // 4. 注册 callee 服务实例（被调服务需要有实例供路由选择）
            registerCalleeServiceInstances();

        } catch (IOException e) {
            Assert.fail("Failed to start NamingServer: " + e.getMessage());
        }
    }

    /**
     * 注册 callee 服务实例
     * 只有被调服务（callee）需要注册实例，因为路由选择的是下游服务的实例
     */
    private void registerCalleeServiceInstances() {

        InstanceParameter parameter = new InstanceParameter();
        parameter.setWeight(100);
        parameter.setHealthy(true);
        parameter.setIsolated(false);
        Map<String, String> meta = new HashMap<>();
        meta.put(TSF_NAMESPACE_ID, NAMESPACE);
        meta.put(TSF_APPLICATION_ID, insCalleeApplicationId);
        meta.put(TSF_GROUP_ID, insCalleeGroupId1);
        parameter.setMetadata(meta);
        namingServer.getNamingService().addInstance(calleeServiceKey, new Node("127.0.0.1", 8080), parameter);
        namingServer.getNamingService().addInstance(calleeServiceKey, new Node("127.0.0.1", 8081), parameter);

        InstanceParameter parameter2 = new InstanceParameter();
        parameter2.setWeight(100);
        parameter2.setHealthy(true);
        parameter2.setIsolated(false);
        Map<String, String> meta2 = new HashMap<>();
        meta2.put(TSF_NAMESPACE_ID, NAMESPACE);
        meta2.put(TSF_APPLICATION_ID, insCalleeApplicationId);
        meta2.put(TSF_GROUP_ID, insCalleeGroupId2);
        parameter2.setMetadata(meta2);
        namingServer.getNamingService().addInstance(calleeServiceKey, new Node("127.0.0.1", 9080), parameter2);
        namingServer.getNamingService().addInstance(calleeServiceKey, new Node("127.0.0.1", 9081), parameter2);
    }

    @After
    public void tearDown() {
        if (namingServer != null) {
            namingServer.terminate();
            System.clearProperty(POLARIS_SERVER_ADDRESS_PROPERTY);
        }
        if (sdkContext != null) {
            sdkContext.destroy();
        }
        if (routerAPI != null) {
            ((BaseEngine) routerAPI).destroy();
        }
        if (consumerAPI != null) {
            consumerAPI.destroy();
        }
        // 清理 MetadataContextHolder 中的线程本地变量
        MetadataContextHolder.remove();
    }

    @Test
    public void testRouting1() throws InterruptedException {
        // 设置规则：NOT_IN + 多个服务名逗号分隔 + 不匹配（测试多个服务名，当前上游服务名在里面的情况）
        setupSourceServiceNameRules(Arrays.asList(CALLER_SERVICE + ",mock-service"), "*", TagConstant.OPERATOR.NOT_IN);

        // 等待服务发现数据同步
        Thread.sleep(3000);

        // 获取 callee 服务的所有实例
        ServiceInstances serviceInstances = getAllInstances(CALLEE_SERVICE);
        Assert.assertNotNull(serviceInstances);
        Assert.assertEquals(4, serviceInstances.getInstances().size());

        for (int i = 0; i < 10; i++) {
            ProcessRoutersRequest request = buildRouterRequest(serviceInstances, null, null);

            // 执行路由
            ProcessRoutersResponse response = routerAPI.processRouters(request);

            // 验证结果
            List<Instance> routedInstances = response.getServiceInstances().getInstances();
            Assert.assertNotNull(routedInstances);
            Assert.assertEquals(4, routedInstances.size());
        }
    }

    @Test
    public void testRouting2() throws InterruptedException {
        // 设置规则：NOT_IN + 多个服务名 + 匹配（测试多个服务名，当前上游服务名不在里面的情况）
        setupSourceServiceNameRules(Arrays.asList("mock-service,mock-service2"), "*", TagConstant.OPERATOR.NOT_IN);

        // 等待服务发现数据同步
        Thread.sleep(3000);

        // 获取 callee 服务的所有实例
        ServiceInstances serviceInstances = getAllInstances(CALLEE_SERVICE);
        Assert.assertNotNull(serviceInstances);
        Assert.assertEquals(4, serviceInstances.getInstances().size());

        for (int i = 0; i < 10; i++) {
            ProcessRoutersRequest request = buildRouterRequest(serviceInstances, null, null);

            // 执行路由
            ProcessRoutersResponse response = routerAPI.processRouters(request);

            // 验证结果：应该返回 group1 实例
            List<Instance> routedInstances = response.getServiceInstances().getInstances();
            Assert.assertNotNull(routedInstances);
            Assert.assertEquals(2, routedInstances.size());

            int[] ports = routedInstances.stream().mapToInt(Instance::getPort).sorted().toArray();
            Assert.assertArrayEquals(new int[] {8080, 8081}, ports);
        }
    }

    @Test
    public void testRouting3() throws InterruptedException {
        // 设置规则：EXACT + SOURCE_SERVICE_NAME + 匹配（精确匹配CallerService）
        setupSourceServiceNameRules(Arrays.asList(CALLER_SERVICE), "*", TagConstant.OPERATOR.EQUAL);

        // 等待服务发现数据同步
        Thread.sleep(3000);

        // 获取 callee 服务的所有实例
        ServiceInstances serviceInstances = getAllInstances(CALLEE_SERVICE);
        Assert.assertNotNull(serviceInstances);
        Assert.assertEquals(4, serviceInstances.getInstances().size());

        for (int i = 0; i < 10; i++) {
            ProcessRoutersRequest request = buildRouterRequest(serviceInstances, null, null);

            // 执行路由
            ProcessRoutersResponse response = routerAPI.processRouters(request);

            // 验证结果：应该返回 group1 实例
            List<Instance> routedInstances = response.getServiceInstances().getInstances();
            Assert.assertNotNull(routedInstances);
            Assert.assertEquals(2, routedInstances.size());

            int[] ports = routedInstances.stream().mapToInt(Instance::getPort).sorted().toArray();
            Assert.assertArrayEquals(new int[] {8080, 8081}, ports);
        }
    }

    @Test
    public void testRouting4() throws InterruptedException {
        // 设置规则：REGEX + SOURCE_SERVICE_NAME + 匹配（正则表达式匹配CallerService）
        setupSourceServiceNameRules(Arrays.asList("Caller.*"), "*", TagConstant.OPERATOR.REGEX);

        // 等待服务发现数据同步
        Thread.sleep(3000);

        // 获取 callee 服务的所有实例
        ServiceInstances serviceInstances = getAllInstances(CALLEE_SERVICE);
        Assert.assertNotNull(serviceInstances);
        Assert.assertEquals(4, serviceInstances.getInstances().size());

        for (int i = 0; i < 10; i++) {
            ProcessRoutersRequest request = buildRouterRequest(serviceInstances, null, null);

            // 执行路由
            ProcessRoutersResponse response = routerAPI.processRouters(request);

            // 验证结果：应该返回 group1 实例
            List<Instance> routedInstances = response.getServiceInstances().getInstances();
            Assert.assertNotNull(routedInstances);
            Assert.assertEquals(2, routedInstances.size());

            int[] ports = routedInstances.stream().mapToInt(Instance::getPort).sorted().toArray();
            Assert.assertArrayEquals(new int[] {8080, 8081}, ports);
        }
    }

    @Test
    public void testRouting5() throws InterruptedException {
        // 设置规则：REGEX + SOURCE_SERVICE_NAME + 不匹配（正则表达式不匹配CallerService）
        setupSourceServiceNameRules(Arrays.asList("MockService.*"), "*", TagConstant.OPERATOR.REGEX);

        // 等待服务发现数据同步
        Thread.sleep(3000);

        // 获取 callee 服务的所有实例
        ServiceInstances serviceInstances = getAllInstances(CALLEE_SERVICE);
        Assert.assertNotNull(serviceInstances);
        Assert.assertEquals(4, serviceInstances.getInstances().size());

        for (int i = 0; i < 10; i++) {
            ProcessRoutersRequest request = buildRouterRequest(serviceInstances, null, null);

            // 执行路由
            ProcessRoutersResponse response = routerAPI.processRouters(request);

            // 验证结果：应该返回所有实例（因为正则不匹配，规则不生效）
            List<Instance> routedInstances = response.getServiceInstances().getInstances();
            Assert.assertNotNull(routedInstances);
            Assert.assertEquals(4, routedInstances.size());
        }
    }

    @Test
    public void testRouting6() throws InterruptedException {
        // 设置规则：IN + SOURCE_SERVICE_NAME + 匹配（在列表中匹配）
        setupSourceServiceNameRules(Arrays.asList("CallerService,MockService"), "*", TagConstant.OPERATOR.IN);

        // 等待服务发现数据同步
        Thread.sleep(3000);

        // 获取 callee 服务的所有实例
        ServiceInstances serviceInstances = getAllInstances(CALLEE_SERVICE);
        Assert.assertNotNull(serviceInstances);
        Assert.assertEquals(4, serviceInstances.getInstances().size());

        for (int i = 0; i < 10; i++) {
            ProcessRoutersRequest request = buildRouterRequest(serviceInstances, null, null);

            // 执行路由
            ProcessRoutersResponse response = routerAPI.processRouters(request);

            // 验证结果：应该返回 group1 实例
            List<Instance> routedInstances = response.getServiceInstances().getInstances();
            Assert.assertNotNull(routedInstances);
            Assert.assertEquals(2, routedInstances.size());

            int[] ports = routedInstances.stream().mapToInt(Instance::getPort).sorted().toArray();
            Assert.assertArrayEquals(new int[] {8080, 8081}, ports);
        }
    }

    @Test
    public void testRouting7() throws InterruptedException {
        // 设置规则：IN + SOURCE_SERVICE_NAME + 不匹配（不在列表中）
        setupSourceServiceNameRules(Arrays.asList("MockService1,MockService2"), "*", TagConstant.OPERATOR.IN);

        // 等待服务发现数据同步
        Thread.sleep(3000);

        // 获取 callee 服务的所有实例
        ServiceInstances serviceInstances = getAllInstances(CALLEE_SERVICE);
        Assert.assertNotNull(serviceInstances);
        Assert.assertEquals(4, serviceInstances.getInstances().size());

        for (int i = 0; i < 10; i++) {
            ProcessRoutersRequest request = buildRouterRequest(serviceInstances, null, null);

            // 执行路由
            ProcessRoutersResponse response = routerAPI.processRouters(request);

            // 验证结果：应该返回所有实例（因为不在列表中，规则不生效）
            List<Instance> routedInstances = response.getServiceInstances().getInstances();
            Assert.assertNotNull(routedInstances);
            Assert.assertEquals(4, routedInstances.size());
        }
    }

    @Test
    public void testRouting8() throws InterruptedException {
        // 设置规则：NOT_EQUALS + SOURCE_SERVICE_NAME + 匹配（不等于指定服务名）
        setupSourceServiceNameRules(Arrays.asList("MockService"), "*", TagConstant.OPERATOR.NOT_EQUAL);

        // 等待服务发现数据同步
        Thread.sleep(3000);

        // 获取 callee 服务的所有实例
        ServiceInstances serviceInstances = getAllInstances(CALLEE_SERVICE);
        Assert.assertNotNull(serviceInstances);
        Assert.assertEquals(4, serviceInstances.getInstances().size());

        for (int i = 0; i < 10; i++) {
            ProcessRoutersRequest request = buildRouterRequest(serviceInstances, null, null);

            // 执行路由
            ProcessRoutersResponse response = routerAPI.processRouters(request);

            // 验证结果：应该返回 group1 实例（因为CallerService不等于MockService）
            List<Instance> routedInstances = response.getServiceInstances().getInstances();
            Assert.assertNotNull(routedInstances);
            Assert.assertEquals(2, routedInstances.size());

            int[] ports = routedInstances.stream().mapToInt(Instance::getPort).sorted().toArray();
            Assert.assertArrayEquals(new int[] {8080, 8081}, ports);
        }
    }

    @Test
    public void testRouting9() throws InterruptedException {
        // 设置规则：NOT_EQUALS + SOURCE_SERVICE_NAME + 不匹配（等于指定服务名）
        setupSourceServiceNameRules(Arrays.asList(CALLER_SERVICE), "*", TagConstant.OPERATOR.NOT_EQUAL);

        // 等待服务发现数据同步
        Thread.sleep(3000);

        // 获取 callee 服务的所有实例
        ServiceInstances serviceInstances = getAllInstances(CALLEE_SERVICE);
        Assert.assertNotNull(serviceInstances);
        Assert.assertEquals(4, serviceInstances.getInstances().size());

        for (int i = 0; i < 10; i++) {
            ProcessRoutersRequest request = buildRouterRequest(serviceInstances, null, null);

            // 执行路由
            ProcessRoutersResponse response = routerAPI.processRouters(request);

            // 验证结果：应该返回所有实例（因为等于指定服务名，规则不生效）
            List<Instance> routedInstances = response.getServiceInstances().getInstances();
            Assert.assertNotNull(routedInstances);
            Assert.assertEquals(4, routedInstances.size());
        }
    }

    @Test
    public void testRouting10() throws InterruptedException {
        // 设置规则：空服务名 + 边界情况测试, 空服务名与 * 一样，满足 RuleUtils.isMatchAllValue
        setupSourceServiceNameRules(Arrays.asList(""), "*", TagConstant.OPERATOR.EQUAL);

        // 等待服务发现数据同步
        Thread.sleep(3000);

        // 获取 callee 服务的所有实例
        ServiceInstances serviceInstances = getAllInstances(CALLEE_SERVICE);
        Assert.assertNotNull(serviceInstances);
        Assert.assertEquals(4, serviceInstances.getInstances().size());

        for (int i = 0; i < 10; i++) {
            ProcessRoutersRequest request = buildRouterRequest(serviceInstances, null, null);

            // 执行路由
            ProcessRoutersResponse response = routerAPI.processRouters(request);

            // 验证结果：应该返回 group1 实例
            List<Instance> routedInstances = response.getServiceInstances().getInstances();
            Assert.assertNotNull(routedInstances);
            Assert.assertEquals(2, routedInstances.size());

            int[] ports = routedInstances.stream().mapToInt(Instance::getPort).sorted().toArray();
            Assert.assertArrayEquals(new int[] {8080, 8081}, ports);
        }
    }

    @Test
    public void testRouting11() throws InterruptedException {
        // 设置规则：通配符 * + 全匹配（测试边界情况）
        setupSourceServiceNameRules(Arrays.asList("*"), "*", TagConstant.OPERATOR.EQUAL);

        // 等待服务发现数据同步
        Thread.sleep(3000);

        // 获取 callee 服务的所有实例
        ServiceInstances serviceInstances = getAllInstances(CALLEE_SERVICE);
        Assert.assertNotNull(serviceInstances);
        Assert.assertEquals(4, serviceInstances.getInstances().size());

        for (int i = 0; i < 10; i++) {
            ProcessRoutersRequest request = buildRouterRequest(serviceInstances, null, null);

            // 执行路由
            ProcessRoutersResponse response = routerAPI.processRouters(request);

            // 验证结果：应该返回 group1 实例（通配符匹配所有服务）
            List<Instance> routedInstances = response.getServiceInstances().getInstances();
            Assert.assertNotNull(routedInstances);
            Assert.assertEquals(2, routedInstances.size());

            int[] ports = routedInstances.stream().mapToInt(Instance::getPort).sorted().toArray();
            Assert.assertArrayEquals(new int[] {8080, 8081}, ports);
        }
    }

    @Test
    public void testRouting12() throws InterruptedException {
        // 设置规则：EXACT + 不同命名空间 + 不匹配（测试命名空间隔离）
        setupSourceServiceNameRules(Arrays.asList(CALLER_SERVICE), "different-namespace", TagConstant.OPERATOR.EQUAL);

        // 等待服务发现数据同步
        Thread.sleep(3000);

        // 获取 callee 服务的所有实例
        ServiceInstances serviceInstances = getAllInstances(CALLEE_SERVICE);
        Assert.assertNotNull(serviceInstances);
        Assert.assertEquals(4, serviceInstances.getInstances().size());

        for (int i = 0; i < 10; i++) {
            ProcessRoutersRequest request = buildRouterRequest(serviceInstances, null, null);

            // 执行路由
            ProcessRoutersResponse response = routerAPI.processRouters(request);

            // 验证结果：应该返回所有实例（因为命名空间不匹配，规则不生效）
            List<Instance> routedInstances = response.getServiceInstances().getInstances();
            Assert.assertNotNull(routedInstances);
            Assert.assertEquals(4, routedInstances.size());
        }
    }

    @Test
    public void testRouting13() throws InterruptedException {
        // 设置规则：大小写敏感测试（EXACT匹配默认不区分大小写）
        setupSourceServiceNameRules(Arrays.asList("callerservice"), "*", TagConstant.OPERATOR.EQUAL);

        // 等待服务发现数据同步
        Thread.sleep(3000);

        // 获取 callee 服务的所有实例
        ServiceInstances serviceInstances = getAllInstances(CALLEE_SERVICE);
        Assert.assertNotNull(serviceInstances);
        Assert.assertEquals(4, serviceInstances.getInstances().size());

        for (int i = 0; i < 10; i++) {
            ProcessRoutersRequest request = buildRouterRequest(serviceInstances, null, null);

            // 执行路由
            ProcessRoutersResponse response = routerAPI.processRouters(request);

            // 验证结果：应该返回 group1 实例（EXACT匹配不区分大小写）
            List<Instance> routedInstances = response.getServiceInstances().getInstances();
            Assert.assertNotNull(routedInstances);
            Assert.assertEquals(2, routedInstances.size());

            int[] ports = routedInstances.stream().mapToInt(Instance::getPort).sorted().toArray();
            Assert.assertArrayEquals(new int[] {8080, 8081}, ports);
        }
    }

    @Test
    public void testRouting14() throws InterruptedException {
        // 设置规则：复杂正则表达式匹配（测试正则表达式的边界情况）
        setupSourceServiceNameRules(Arrays.asList("^Caller.*Service$"), "*", TagConstant.OPERATOR.REGEX);

        // 等待服务发现数据同步
        Thread.sleep(3000);

        // 获取 callee 服务的所有实例
        ServiceInstances serviceInstances = getAllInstances(CALLEE_SERVICE);
        Assert.assertNotNull(serviceInstances);
        Assert.assertEquals(4, serviceInstances.getInstances().size());

        for (int i = 0; i < 10; i++) {
            ProcessRoutersRequest request = buildRouterRequest(serviceInstances, null, null);

            // 执行路由
            ProcessRoutersResponse response = routerAPI.processRouters(request);

            // 验证结果：应该返回 group1 实例（正则表达式匹配CallerService）
            List<Instance> routedInstances = response.getServiceInstances().getInstances();
            Assert.assertNotNull(routedInstances);
            Assert.assertEquals(2, routedInstances.size());

            int[] ports = routedInstances.stream().mapToInt(Instance::getPort).sorted().toArray();
            Assert.assertArrayEquals(new int[] {8080, 8081}, ports);
        }
    }

    @Test
    public void testRouting15() throws InterruptedException {
        // 设置规则：空规则测试（测试无规则时的默认行为）
        // 不设置任何规则，测试默认路由行为

        // 等待服务发现数据同步
        Thread.sleep(3000);

        // 获取 callee 服务的所有实例
        ServiceInstances serviceInstances = getAllInstances(CALLEE_SERVICE);
        Assert.assertNotNull(serviceInstances);
        Assert.assertEquals(4, serviceInstances.getInstances().size());

        for (int i = 0; i < 10; i++) {
            ProcessRoutersRequest request = buildRouterRequest(serviceInstances, null, null);

            // 执行路由
            ProcessRoutersResponse response = routerAPI.processRouters(request);

            // 验证结果：应该返回所有实例（无规则时默认返回所有可用实例）
            List<Instance> routedInstances = response.getServiceInstances().getInstances();
            Assert.assertNotNull(routedInstances);
            Assert.assertEquals(4, routedInstances.size());
        }
    }

    @Test
    public void testRouting16() throws InterruptedException {
        // 设置规则：多个规则，都是服务名 EQUAL，无法满足
        setupSourceServiceNameRules(Arrays.asList(CALLER_SERVICE, "mock-service"), "*", TagConstant.OPERATOR.EQUAL);

        // 等待服务发现数据同步
        Thread.sleep(3000);

        // 获取 callee 服务的所有实例
        ServiceInstances serviceInstances = getAllInstances(CALLEE_SERVICE);
        Assert.assertNotNull(serviceInstances);
        Assert.assertEquals(4, serviceInstances.getInstances().size());

        for (int i = 0; i < 10; i++) {
            ProcessRoutersRequest request = buildRouterRequest(serviceInstances, null, null);

            // 执行路由
            ProcessRoutersResponse response = routerAPI.processRouters(request);

            // 验证结果
            List<Instance> routedInstances = response.getServiceInstances().getInstances();
            Assert.assertNotNull(routedInstances);
            Assert.assertEquals(4, routedInstances.size());
        }
    }

    @Test
    public void testRouting17() throws InterruptedException {
        // 设置规则：多个规则，服务名 NOT_EQUAL，满足
        setupSourceServiceNameRules(Arrays.asList("mock-service", "mock-service2"), "*", TagConstant.OPERATOR.NOT_EQUAL);

        // 等待服务发现数据同步
        Thread.sleep(3000);

        // 获取 callee 服务的所有实例
        ServiceInstances serviceInstances = getAllInstances(CALLEE_SERVICE);
        Assert.assertNotNull(serviceInstances);
        Assert.assertEquals(4, serviceInstances.getInstances().size());

        for (int i = 0; i < 10; i++) {
            ProcessRoutersRequest request = buildRouterRequest(serviceInstances, null, null);

            // 执行路由
            ProcessRoutersResponse response = routerAPI.processRouters(request);

            // 验证结果
            List<Instance> routedInstances = response.getServiceInstances().getInstances();
            Assert.assertNotNull(routedInstances);
            Assert.assertEquals(2, routedInstances.size());

            int[] ports = routedInstances.stream().mapToInt(Instance::getPort).sorted().toArray();
            Assert.assertArrayEquals(new int[] {8080, 8081}, ports);
        }
    }



    private void setupSourceServiceNameRules(List<String> sourceServiceNames, String sourceNamespace, String operator) {
        List<RouteTag> routeTags = new ArrayList<>();
        for (String sourceServiceName : sourceServiceNames) {
            RouteTag routeTag = new RouteTag();
            if (StringUtils.equals(sourceNamespace, RuleUtils.MATCH_ALL)) {
                routeTag.setTagField(TagConstant.SYSTEM_FIELD.SOURCE_SERVICE_NAME);
                routeTag.setTagValue(sourceServiceName);
            } else {
                routeTag.setTagField(TagConstant.SYSTEM_FIELD.SOURCE_NAMESPACE_SERVICE_NAME);
                routeTag.setTagValue(sourceNamespace + "/" + sourceServiceName);
            }
            routeTag.setTagOperator(operator);
            routeTags.add(routeTag);
        }

        RoutingProto.Destination destination = RoutingProto.Destination.newBuilder()
                .setNamespace(StringValue.newBuilder().setValue(NAMESPACE).build())
                .setService(StringValue.newBuilder().setValue(CALLEE_SERVICE).build())
                .putMetadata(TSF_GROUP_ID, ModelProto.MatchString.newBuilder().setValue(StringValue.newBuilder().setValue(ruleCalleeGroupId)).build())
                .setWeight(UInt32Value.newBuilder().setValue(100).build())
                .build();

        RoutingProto.Routing routing = RoutingProto.Routing.newBuilder()
                .setService(StringValue.newBuilder().setValue(CALLEE_SERVICE).build())
                .setNamespace(StringValue.newBuilder().setValue(NAMESPACE).build())
                .addInbounds(RoutingProto.Route.newBuilder()
                        .putMetadata(RouterConstants.MATCH_ALL_SOURCES, "true")
                        .addDestinations(destination)
                        .addAllSources(RouterUtils.parseTagListToSourceList(routeTags)).build())
                .build();

        namingServer.getNamingService().setRouting(calleeServiceKey, routing);
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取指定服务的所有实例
     */
    private ServiceInstances getAllInstances(String serviceName) {
        GetAllInstancesRequest request = new GetAllInstancesRequest();
        request.setNamespace(NAMESPACE);
        request.setService(serviceName);
        InstancesResponse response = consumerAPI.getAllInstances(request);
        return response.getServiceInstances();
    }

    /**
     * 构建路由请求
     * 模拟 caller（主调服务）调用 callee（被调服务）
     * 
     * Header 需要设置在 MetadataContext 的 MessageMetadataContainer 中，
     * 而不是通过 addRouterMetadata 方法
     */
    private ProcessRoutersRequest buildRouterRequest(ServiceInstances serviceInstances,
                                                      String key,
                                                      String value) {
        // 先清理之前的 MetadataContext，确保每次测试使用干净的上下文
        MetadataContextHolder.remove();
        
        ProcessRoutersRequest request = new ProcessRoutersRequest();
        request.setNamespace(NAMESPACE);
        request.setService(CALLEE_SERVICE);  // 目标服务为 callee

        // 设置源服务信息（caller - 主调服务）
        ServiceInfo sourceService = new ServiceInfo();
        sourceService.setNamespace(NAMESPACE);
        sourceService.setService(CALLER_SERVICE);  // 源服务为 caller
        request.setSourceService(sourceService);

        // 设置目标实例（callee 的实例）
        request.setDstInstances(serviceInstances);

        // 通过 MetadataContextHolder 设置 header 到 MetadataContainer
        // LaneRouter 会从 MetadataContext 的 caller/callee MetadataContainer 中获取 key
        if (key != null && value != null) {
            MetadataContext metadataContext = MetadataContextHolder.getOrCreate();
            // 设置到 caller（主调方）的 MetadataContainer
            MetadataContainer calleeMessageContainer = metadataContext.getMetadataContainer(MetadataType.CUSTOM, false);

            calleeMessageContainer.putMetadataStringValue(key, value, TransitiveType.PASS_THROUGH);
        }

        return request;
    }
}