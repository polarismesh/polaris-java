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

import com.google.protobuf.Any;
import com.google.protobuf.StringValue;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.GetAllInstancesRequest;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.client.api.BaseEngine;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.client.pojo.Node;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import com.tencent.polaris.factory.api.RouterAPIFactory;
import com.tencent.polaris.metadata.core.MessageMetadataContainer;
import com.tencent.polaris.metadata.core.MetadataType;
import com.tencent.polaris.metadata.core.TransitiveType;
import com.tencent.polaris.metadata.core.manager.MetadataContext;
import com.tencent.polaris.metadata.core.manager.MetadataContextHolder;
import com.tencent.polaris.router.api.core.RouterAPI;
import com.tencent.polaris.router.api.rpc.ProcessRoutersRequest;
import com.tencent.polaris.router.api.rpc.ProcessRoutersResponse;
import com.tencent.polaris.specification.api.v1.model.ModelProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.LaneProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.RoutingProto;
import com.tencent.polaris.test.common.TestUtils;
import com.tencent.polaris.test.mock.discovery.NamingServer;
import com.tencent.polaris.test.mock.discovery.NamingService.InstanceParameter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LaneRouter 集成测试
 * 使用 NamingServer Mock 服务器和 RouterAPI 进行完整的泳道路由测试
 * 
 * 场景说明：
 * - caller (主调服务) 作为泳道入口，负责根据请求头判断是否需要染色
 * - callee (被调服务) 作为下游服务，接收染色后的请求并路由到对应泳道实例
 * 
 * 泳道组配置：
 * - entries (入口服务): caller
 * - destinations (目标服务): callee
 */
public class LaneRouterIntegrationTest {

    private static final String POLARIS_SERVER_ADDRESS_PROPERTY = "POLARIS_SEVER_ADDRESS";
    private static final String NAMESPACE = "Test";
    private static final String CALLER_SERVICE = "CallerService";  // 主调服务，泳道入口
    private static final String CALLEE_SERVICE = "CalleeService";  // 被调服务，下游服务
    private static final String GRAY_LANE = "gray";
    private static final String BLUE_LANE = "blue";

    private NamingServer namingServer;
    private SDKContext sdkContext;
    private RouterAPI routerAPI;
    private ConsumerAPI consumerAPI;
    private ServiceKey callerServiceKey;  // 主调服务 key
    private ServiceKey calleeServiceKey;  // 被调服务 key

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

            // 5. 设置泳道规则（入口为 caller，目标为 callee）
            setupLaneRules();

        } catch (IOException e) {
            Assert.fail("Failed to start NamingServer: " + e.getMessage());
        }
    }

    /**
     * 注册 callee 服务实例：基线实例 + gray 泳道实例 + blue 泳道实例
     * 只有被调服务（callee）需要注册实例，因为路由选择的是下游服务的实例
     */
    private void registerCalleeServiceInstances() {
        // 基线实例（无泳道标签）- 2个
        InstanceParameter baseParam = new InstanceParameter();
        baseParam.setWeight(100);
        baseParam.setHealthy(true);
        baseParam.setIsolated(false);
        Map<String, String> baseMeta = new HashMap<>();
        baseMeta.put("version", "1.0.0");
        baseParam.setMetadata(baseMeta);
        namingServer.getNamingService().addInstance(calleeServiceKey, new Node("127.0.0.1", 8080), baseParam);
        namingServer.getNamingService().addInstance(calleeServiceKey, new Node("127.0.0.1", 8081), baseParam);

        // gray 泳道实例 - 2个
        InstanceParameter grayParam = new InstanceParameter();
        grayParam.setWeight(100);
        grayParam.setHealthy(true);
        grayParam.setIsolated(false);
        Map<String, String> grayMeta = new HashMap<>();
        grayMeta.put("version", "1.0.0");
        grayMeta.put("lane", GRAY_LANE);
        grayParam.setMetadata(grayMeta);
        namingServer.getNamingService().addInstance(calleeServiceKey, new Node("127.0.0.1", 9080), grayParam);
        namingServer.getNamingService().addInstance(calleeServiceKey, new Node("127.0.0.1", 9081), grayParam);

        // blue 泳道实例 - 2个
        InstanceParameter blueParam = new InstanceParameter();
        blueParam.setWeight(100);
        blueParam.setHealthy(true);
        blueParam.setIsolated(false);
        Map<String, String> blueMeta = new HashMap<>();
        blueMeta.put("version", "1.0.0");
        blueMeta.put("lane", BLUE_LANE);
        blueParam.setMetadata(blueMeta);
        namingServer.getNamingService().addInstance(calleeServiceKey, new Node("127.0.0.1", 10080), blueParam);
        namingServer.getNamingService().addInstance(calleeServiceKey, new Node("127.0.0.1", 10081), blueParam);
    }

    /**
     * 设置泳道规则
     * 入口服务（entries）: caller - 负责染色
     * 目标服务（destinations）: callee - 接收染色请求并路由
     */
    private void setupLaneRules() {
        // 构建 gray 泳道规则 - 匹配 header x-lane=gray
        LaneProto.LaneRule grayRule = buildLaneRuleWithHeaderMatch(
                "gray-rule",
                "lane-group",
                GRAY_LANE,
                "x-lane",
                GRAY_LANE,
                1
        );

        // 构建 blue 泳道规则 - 匹配 header x-lane=blue
        LaneProto.LaneRule blueRule = buildLaneRuleWithHeaderMatch(
                "blue-rule",
                "lane-group",
                BLUE_LANE,
                "x-lane",
                BLUE_LANE,
                2
        );

        // 构建泳道组：入口服务为 caller，目标服务为 callee
        LaneProto.LaneGroup laneGroup = buildLaneGroup(
                "lane-group",
                NAMESPACE,
                CALLER_SERVICE,   // 入口服务（entries）
                CALLEE_SERVICE,   // 目标服务（destinations）
                Arrays.asList(grayRule, blueRule)
        );

        // 设置到 NamingServer - 需要同时为 caller 和 callee 设置泳道规则
        // caller 需要泳道规则来判断是否染色
        // callee 需要泳道规则来执行路由
        namingServer.getNamingService().setLaneGroup(laneGroup);
        namingServer.getNamingService().setLaneGroup(laneGroup);
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

    /**
     * 测试基线流量路由
     * 当请求没有泳道标签时，应该路由到基线实例
     */
    @Test
    public void testBaselineRouting() throws InterruptedException {
        // 等待服务发现数据同步
        Thread.sleep(3000);

        // 获取 callee 服务的所有实例
        ServiceInstances serviceInstances = getAllInstances(CALLEE_SERVICE);
        Assert.assertNotNull(serviceInstances);
        Assert.assertEquals(6, serviceInstances.getInstances().size());

        // 构建路由请求（不设置泳道标签）- caller 调用 callee
        ProcessRoutersRequest request = buildRouterRequest(serviceInstances, null, null);

        // 执行路由
        ProcessRoutersResponse response = routerAPI.processRouters(request);

        // 验证结果：应该返回基线实例（无 lane 标签的实例）
        List<Instance> routedInstances = response.getServiceInstances().getInstances();
        Assert.assertNotNull(routedInstances);
        Assert.assertEquals(2, routedInstances.size());

        // 验证返回的实例是基线实例（端口 8080, 8081）
        int[] ports = routedInstances.stream().mapToInt(Instance::getPort).sorted().toArray();
        Assert.assertArrayEquals(new int[]{8080, 8081}, ports);
    }

    /**
     * 测试 gray 泳道路由
     * 当请求头包含 x-lane=gray 时，应该路由到 gray 泳道实例
     * 场景：caller（泳道入口）收到带有 x-lane=gray 的请求，染色后转发给 callee
     */
    @Test
    public void testGrayLaneRouting() throws InterruptedException {
        // 等待服务发现数据同步
        Thread.sleep(3000);

        // 获取 callee 服务的所有实例
        ServiceInstances serviceInstances = getAllInstances(CALLEE_SERVICE);

        // 构建路由请求（设置 x-lane=gray）- caller 调用 callee
        ProcessRoutersRequest request = buildRouterRequest(serviceInstances, "x-lane", GRAY_LANE);

        // 执行路由
        ProcessRoutersResponse response = routerAPI.processRouters(request);

        // 验证结果：应该返回 gray 泳道实例
        List<Instance> routedInstances = response.getServiceInstances().getInstances();
        Assert.assertNotNull(routedInstances);
        Assert.assertEquals(2, routedInstances.size());

        // 验证返回的实例是 gray 泳道实例（端口 9080, 9081）
        int[] ports = routedInstances.stream().mapToInt(Instance::getPort).sorted().toArray();
        Assert.assertArrayEquals(new int[]{9080, 9081}, ports);

        // 验证实例的 lane 标签
        for (Instance instance : routedInstances) {
            Assert.assertEquals(GRAY_LANE, instance.getMetadata().get("lane"));
        }
    }

    /**
     * 测试 blue 泳道路由
     * 当请求头包含 x-lane=blue 时，应该路由到 blue 泳道实例
     * 场景：caller（泳道入口）收到带有 x-lane=blue 的请求，染色后转发给 callee
     */
    @Test
    public void testBlueLaneRouting() throws InterruptedException {
        // 等待服务发现数据同步
        Thread.sleep(3000);

        // 获取 callee 服务的所有实例
        ServiceInstances serviceInstances = getAllInstances(CALLEE_SERVICE);

        // 构建路由请求（设置 x-lane=blue）- caller 调用 callee
        ProcessRoutersRequest request = buildRouterRequest(serviceInstances, "x-lane", BLUE_LANE);

        // 执行路由
        ProcessRoutersResponse response = routerAPI.processRouters(request);

        // 验证结果：应该返回 blue 泳道实例
        List<Instance> routedInstances = response.getServiceInstances().getInstances();
        Assert.assertNotNull(routedInstances);
        Assert.assertEquals(2, routedInstances.size());

        // 验证返回的实例是 blue 泳道实例（端口 10080, 10081）
        int[] ports = routedInstances.stream().mapToInt(Instance::getPort).sorted().toArray();
        Assert.assertArrayEquals(new int[]{10080, 10081}, ports);

        // 验证实例的 lane 标签
        for (Instance instance : routedInstances) {
            Assert.assertEquals(BLUE_LANE, instance.getMetadata().get("lane"));
        }
    }

    /**
     * 测试不匹配的泳道标签
     * 当请求头的泳道标签不匹配任何规则时，应该路由到基线实例
     */
    @Test
    public void testUnmatchedLaneRouting() throws InterruptedException {
        // 等待服务发现数据同步
        Thread.sleep(3000);

        // 获取 callee 服务的所有实例
        ServiceInstances serviceInstances = getAllInstances(CALLEE_SERVICE);

        // 构建路由请求（设置一个不存在的泳道标签）- caller 调用 callee
        ProcessRoutersRequest request = buildRouterRequest(serviceInstances, "x-lane", "nonexistent");

        // 执行路由
        ProcessRoutersResponse response = routerAPI.processRouters(request);

        // 验证结果：应该路由到基线实例
        List<Instance> routedInstances = response.getServiceInstances().getInstances();
        Assert.assertNotNull(routedInstances);
        Assert.assertEquals(2, routedInstances.size());

        // 验证返回的实例是基线实例
        int[] ports = routedInstances.stream().mapToInt(Instance::getPort).sorted().toArray();
        Assert.assertArrayEquals(new int[]{8080, 8081}, ports);
    }

    /**
     * 测试多次路由的一致性
     * 验证同一泳道标签的多次路由结果一致
     */
    @Test
    public void testRoutingConsistency() throws InterruptedException {
        // 等待服务发现数据同步
        Thread.sleep(3000);

        // 获取 callee 服务的所有实例
        ServiceInstances serviceInstances = getAllInstances(CALLEE_SERVICE);

        // 多次调用路由，验证结果一致性
        for (int i = 0; i < 10; i++) {
            ProcessRoutersRequest request = buildRouterRequest(serviceInstances, "x-lane", GRAY_LANE);
            ProcessRoutersResponse response = routerAPI.processRouters(request);
            List<Instance> routedInstances = response.getServiceInstances().getInstances();

            Assert.assertEquals(2, routedInstances.size());
            for (Instance instance : routedInstances) {
                Assert.assertEquals(GRAY_LANE, instance.getMetadata().get("lane"));
            }
        }
    }

    // ==================== TrafficGray 流量灰度测试 ====================

    /**
     * 测试 TrafficGray 百分比模式（100% 染色）
     * 当配置 percentage=100 时，所有匹配的流量都应该被染色到指定泳道
     */
    @Test
    public void testTrafficGrayPercentageMode() throws InterruptedException {
        // 1. 先清理之前的泳道规则
        tearDown();
        
        // 2. 重新启动 NamingServer 并设置百分比染色规则
        try {
            namingServer = NamingServer.startNamingServer(-1);
            System.setProperty(POLARIS_SERVER_ADDRESS_PROPERTY, String.format("127.0.0.1:%d", namingServer.getPort()));
            
            Configuration configuration = TestUtils.configWithEnvAddress();
            sdkContext = SDKContext.initContextByConfig(configuration);
            routerAPI = RouterAPIFactory.createRouterAPIByContext(sdkContext);
            consumerAPI = DiscoveryAPIFactory.createConsumerAPIByContext(sdkContext);
            
            // 注册 callee 服务实例
            registerCalleeServiceInstances();
            
            // 构建百分比染色的泳道规则（100% 染色到 gray 泳道）
            LaneProto.LaneRule percentageRule = buildPercentageLaneRule(
                    "percentage-rule",
                    "percentage-group",
                    GRAY_LANE,
                    100,  // 100% 染色
                    1
            );
            
            // 构建泳道组
            LaneProto.LaneGroup laneGroup = buildLaneGroup(
                    "percentage-group",
                    NAMESPACE,
                    CALLER_SERVICE,
                    CALLEE_SERVICE,
                    Collections.singletonList(percentageRule)
            );
            
            namingServer.getNamingService().setLaneGroup(laneGroup);
            
            // 等待服务发现数据同步
            Thread.sleep(3000);
            
            // 获取 callee 服务的所有实例
            ServiceInstances serviceInstances = getAllInstances(CALLEE_SERVICE);
            
            // 执行多次路由，验证 100% 染色到 gray 泳道
            int grayCount = 0;
            int totalRoutes = 10;
            for (int i = 0; i < totalRoutes; i++) {
                // 不设置 header，让 TrafficGray 的 percentage 来决定染色
                ProcessRoutersRequest request = buildRouterRequest(serviceInstances, null, null);
                ProcessRoutersResponse response = routerAPI.processRouters(request);
                List<Instance> routedInstances = response.getServiceInstances().getInstances();
                
                if (routedInstances.size() == 2) {
                    boolean allGray = routedInstances.stream()
                            .allMatch(inst -> GRAY_LANE.equals(inst.getMetadata().get("lane")));
                    if (allGray) {
                        grayCount++;
                    }
                }
            }
            
            // 100% 染色，应该全部路由到 gray 泳道
            Assert.assertEquals("100% 百分比染色应该全部路由到 gray 泳道", totalRoutes, grayCount);
            
        } catch (IOException e) {
            Assert.fail("Failed to start NamingServer: " + e.getMessage());
        }
    }

    /**
     * 测试 TrafficGray 预热模式
     * 当预热时间已过时，应该 100% 染色到指定泳道
     */
    @Test
    public void testTrafficGrayWarmupMode() throws InterruptedException {
        // 1. 先清理之前的泳道规则
        tearDown();
        
        // 2. 重新启动 NamingServer 并设置预热染色规则
        try {
            namingServer = NamingServer.startNamingServer(-1);
            System.setProperty(POLARIS_SERVER_ADDRESS_PROPERTY, String.format("127.0.0.1:%d", namingServer.getPort()));
            
            Configuration configuration = TestUtils.configWithEnvAddress();
            sdkContext = SDKContext.initContextByConfig(configuration);
            routerAPI = RouterAPIFactory.createRouterAPIByContext(sdkContext);
            consumerAPI = DiscoveryAPIFactory.createConsumerAPIByContext(sdkContext);
            
            // 注册 callee 服务实例
            registerCalleeServiceInstances();
            
            // 构建预热染色的泳道规则
            // etime 设置为过去的时间，表示预热已完成
            String pastTime = java.time.LocalDateTime.now()
                    .minusHours(1)
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            
            LaneProto.LaneRule warmupRule = buildWarmupLaneRule(
                    "warmup-rule",
                    "warmup-group",
                    GRAY_LANE,
                    pastTime,     // 预热开始时间为1小时前
                    60,           // 预热间隔60秒（已过）
                    2,            // 曲率
                    1
            );
            
            // 构建泳道组
            LaneProto.LaneGroup laneGroup = buildLaneGroup(
                    "warmup-group",
                    NAMESPACE,
                    CALLER_SERVICE,
                    CALLEE_SERVICE,
                    Collections.singletonList(warmupRule)
            );
            
            namingServer.getNamingService().setLaneGroup(laneGroup);
            
            // 等待服务发现数据同步
            Thread.sleep(3000);
            
            // 获取 callee 服务的所有实例
            ServiceInstances serviceInstances = getAllInstances(CALLEE_SERVICE);
            
            // 执行多次路由，验证预热完成后 100% 染色到 gray 泳道
            int grayCount = 0;
            int totalRoutes = 10;
            for (int i = 0; i < totalRoutes; i++) {
                // 不设置 header，让 TrafficGray 的 warmup 来决定染色
                ProcessRoutersRequest request = buildRouterRequest(serviceInstances, null, null);
                ProcessRoutersResponse response = routerAPI.processRouters(request);
                List<Instance> routedInstances = response.getServiceInstances().getInstances();
                
                if (routedInstances.size() == 2) {
                    boolean allGray = routedInstances.stream()
                            .allMatch(inst -> GRAY_LANE.equals(inst.getMetadata().get("lane")));
                    if (allGray) {
                        grayCount++;
                    }
                }
            }
            
            // 预热完成后，应该全部路由到 gray 泳道
            Assert.assertEquals("预热完成后应该全部路由到 gray 泳道", totalRoutes, grayCount);
            
        } catch (IOException e) {
            Assert.fail("Failed to start NamingServer: " + e.getMessage());
        }
    }

    /**
     * 测试 TrafficGray 百分比模式（50% 染色）
     * 验证部分流量染色的场景
     */
    @Test
    public void testTrafficGrayPartialPercentage() throws InterruptedException {
        // 1. 先清理之前的泳道规则
        tearDown();
        
        // 2. 重新启动 NamingServer 并设置 50% 百分比染色规则
        try {
            namingServer = NamingServer.startNamingServer(-1);
            System.setProperty(POLARIS_SERVER_ADDRESS_PROPERTY, String.format("127.0.0.1:%d", namingServer.getPort()));
            
            Configuration configuration = TestUtils.configWithEnvAddress();
            sdkContext = SDKContext.initContextByConfig(configuration);
            routerAPI = RouterAPIFactory.createRouterAPIByContext(sdkContext);
            consumerAPI = DiscoveryAPIFactory.createConsumerAPIByContext(sdkContext);
            
            // 注册 callee 服务实例
            registerCalleeServiceInstances();
            
            // 构建百分比染色的泳道规则（50% 染色到 gray 泳道）
            LaneProto.LaneRule percentageRule = buildPercentageLaneRule(
                    "partial-percentage-rule",
                    "partial-percentage-group",
                    GRAY_LANE,
                    50,  // 50% 染色
                    1
            );
            
            // 构建泳道组
            LaneProto.LaneGroup laneGroup = buildLaneGroup(
                    "partial-percentage-group",
                    NAMESPACE,
                    CALLER_SERVICE,
                    CALLEE_SERVICE,
                    Collections.singletonList(percentageRule)
            );
            
            namingServer.getNamingService().setLaneGroup(laneGroup);
            
            // 等待服务发现数据同步
            Thread.sleep(3000);
            
            // 获取 callee 服务的所有实例
            ServiceInstances serviceInstances = getAllInstances(CALLEE_SERVICE);
            
            // 执行多次路由，统计染色比例
            int grayCount = 0;
            int baselineCount = 0;
            int totalRoutes = 100;
            
            for (int i = 0; i < totalRoutes; i++) {
                // 不设置 header，让 TrafficGray 的 percentage 来决定染色
                ProcessRoutersRequest request = buildRouterRequest(serviceInstances, null, null);
                ProcessRoutersResponse response = routerAPI.processRouters(request);
                List<Instance> routedInstances = response.getServiceInstances().getInstances();
                
                if (routedInstances.size() == 2) {
                    String lane = routedInstances.get(0).getMetadata().get("lane");
                    if (GRAY_LANE.equals(lane)) {
                        grayCount++;
                    } else if (lane == null) {
                        baselineCount++;
                    }
                }
            }
            
            // 50% 染色，gray 和 baseline 的比例应该大致接近
            // 由于是随机的，我们只验证两者都有一定数量
            System.out.println("TrafficGray 50% 染色结果: gray=" + grayCount + ", baseline=" + baselineCount);
            Assert.assertTrue("50% 染色应该有 gray 泳道的流量", grayCount > 0);
            Assert.assertTrue("50% 染色应该有 baseline 的流量", baselineCount > 0);
            
        } catch (IOException e) {
            Assert.fail("Failed to start NamingServer: " + e.getMessage());
        }
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
                                                      String headerKey,
                                                      String headerValue) {
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

        // 通过 MetadataContextHolder 设置 header 到 MessageMetadataContainer
        // LaneRouter 会从 MetadataContext 的 caller/callee MessageMetadataContainer 中获取 header
        if (headerKey != null && headerValue != null) {
            MetadataContext metadataContext = MetadataContextHolder.getOrCreate();
            // 设置到 caller（主调方）的 MessageMetadataContainer
            MessageMetadataContainer callerMessageContainer = metadataContext.getMetadataContainer(MetadataType.MESSAGE, true);
            callerMessageContainer.setHeader(headerKey, headerValue, TransitiveType.PASS_THROUGH);
        }

        return request;
    }

    /**
     * 构建带有请求头匹配的泳道规则
     */
    private LaneProto.LaneRule buildLaneRuleWithHeaderMatch(String ruleName,
                                                            String groupName,
                                                            String laneName,
                                                            String headerKey,
                                                            String headerValue,
                                                            int priority) {
        // 构建匹配条件
        ModelProto.MatchString matchString = ModelProto.MatchString.newBuilder()
                .setType(ModelProto.MatchString.MatchStringType.EXACT)
                .setValue(StringValue.newBuilder().setValue(headerValue).build())
                .build();

        // 构建参数匹配
        RoutingProto.SourceMatch sourceMatch = RoutingProto.SourceMatch.newBuilder()
                .setType(RoutingProto.SourceMatch.Type.HEADER)
                .setKey(headerKey)
                .setValue(matchString)
                .build();

        // 构建流量匹配规则
        LaneProto.TrafficMatchRule trafficMatchRule = LaneProto.TrafficMatchRule.newBuilder()
                .addArguments(sourceMatch)
                .build();

        // 构建泳道规则
        return LaneProto.LaneRule.newBuilder()
                .setName(ruleName)
                .setGroupName(groupName)
                .setEnable(true)
                .setDefaultLabelValue(laneName)
                .setMatchMode(LaneProto.LaneRule.LaneMatchMode.PERMISSIVE)
                .setTrafficMatchRule(trafficMatchRule)
                .setPriority(priority)
                .build();
    }

    /**
     * 构建百分比染色的泳道规则
     * 
     * @param ruleName 规则名称
     * @param groupName 泳道组名称
     * @param laneName 目标泳道名称
     * @param percentage 染色百分比（0-100）
     * @param priority 优先级
     */
    private LaneProto.LaneRule buildPercentageLaneRule(String ruleName,
                                                        String groupName,
                                                        String laneName,
                                                        int percentage,
                                                        int priority) {
        LaneProto.LaneRule.Builder builder = LaneProto.LaneRule.newBuilder();
        builder.setId(ruleName);
        builder.setName(ruleName);
        builder.setGroupName(groupName);
        builder.setEnable(true);
        builder.setMatchMode(LaneProto.LaneRule.LaneMatchMode.PERMISSIVE);
        builder.setDefaultLabelValue(laneName);
        builder.setPriority(priority);
        builder.setCtime("2024-01-01 00:00:00");
        builder.setEtime("2024-01-01 00:00:00");
        
        // 设置空的流量匹配规则（匹配所有流量）
        builder.setTrafficMatchRule(LaneProto.TrafficMatchRule.newBuilder()
                .setMatchMode(LaneProto.TrafficMatchRule.TrafficMatchMode.AND)
                .build());

        // 设置百分比灰度配置
        LaneProto.TrafficGray.Builder grayBuilder = LaneProto.TrafficGray.newBuilder();
        grayBuilder.setMode(LaneProto.TrafficGray.Mode.PERCENTAGE);
        LaneProto.TrafficGray.Percentage.Builder percentageBuilder = LaneProto.TrafficGray.Percentage.newBuilder();
        percentageBuilder.setPercent(percentage);
        grayBuilder.setPercentage(percentageBuilder.build());
        builder.setTrafficGray(grayBuilder.build());

        return builder.build();
    }

    /**
     * 构建预热染色的泳道规则
     * 
     * @param ruleName 规则名称
     * @param groupName 泳道组名称
     * @param laneName 目标泳道名称
     * @param etime 规则启用时间（格式：yyyy-MM-dd HH:mm:ss）
     * @param warmupIntervalSeconds 预热间隔（秒）
     * @param curvature 曲率
     * @param priority 优先级
     */
    private LaneProto.LaneRule buildWarmupLaneRule(String ruleName,
                                                    String groupName,
                                                    String laneName,
                                                    String etime,
                                                    int warmupIntervalSeconds,
                                                    int curvature,
                                                    int priority) {
        LaneProto.LaneRule.Builder builder = LaneProto.LaneRule.newBuilder();
        builder.setId(ruleName);
        builder.setName(ruleName);
        builder.setGroupName(groupName);
        builder.setEnable(true);
        builder.setMatchMode(LaneProto.LaneRule.LaneMatchMode.PERMISSIVE);
        builder.setDefaultLabelValue(laneName);
        builder.setPriority(priority);
        builder.setCtime("2024-01-01 00:00:00");
        builder.setEtime(etime);
        
        // 设置空的流量匹配规则（匹配所有流量）
        builder.setTrafficMatchRule(LaneProto.TrafficMatchRule.newBuilder()
                .setMatchMode(LaneProto.TrafficMatchRule.TrafficMatchMode.AND)
                .build());

        // 设置预热灰度配置
        LaneProto.TrafficGray.Builder grayBuilder = LaneProto.TrafficGray.newBuilder();
        grayBuilder.setMode(LaneProto.TrafficGray.Mode.WARMUP);
        LaneProto.TrafficGray.Warmup.Builder warmupBuilder = LaneProto.TrafficGray.Warmup.newBuilder();
        warmupBuilder.setIntervalSecond(warmupIntervalSeconds);
        warmupBuilder.setCurvature(curvature);
        grayBuilder.setWarmup(warmupBuilder.build());
        builder.setTrafficGray(grayBuilder.build());

        return builder.build();
    }

    /**
     * 构建泳道组
     * 
     * @param groupName 泳道组名称
     * @param namespace 命名空间
     * @param entryService 入口服务名称（caller，负责染色）
     * @param destService 目标服务名称（callee，接收染色请求）
     * @param rules 泳道规则列表
     */
    private LaneProto.LaneGroup buildLaneGroup(String groupName,
                                                String namespace,
                                                String entryService,
                                                String destService,
                                                List<LaneProto.LaneRule> rules) {
        LaneProto.LaneGroup.Builder builder = LaneProto.LaneGroup.newBuilder();
        builder.setName(groupName);

        // 添加目标服务（destinations）- callee，接收染色后的请求
        RoutingProto.DestinationGroup.Builder destBuilder = RoutingProto.DestinationGroup.newBuilder();
        destBuilder.setNamespace(namespace);
        destBuilder.setService(destService);
        builder.addDestinations(destBuilder.build());

        // 添加流量入口（entries）- caller，负责染色
        LaneProto.TrafficEntry.Builder entryBuilder = LaneProto.TrafficEntry.newBuilder();
        entryBuilder.setType("polarismesh.cn/service");
        LaneProto.ServiceSelector.Builder selectorBuilder = LaneProto.ServiceSelector.newBuilder();
        selectorBuilder.setNamespace(namespace);
        selectorBuilder.setService(entryService);
        entryBuilder.setSelector(Any.pack(selectorBuilder.build()));
        builder.addEntries(entryBuilder.build());

        // 添加规则
        if (rules != null) {
            builder.addAllRules(rules);
        }

        return builder.build();
    }
}