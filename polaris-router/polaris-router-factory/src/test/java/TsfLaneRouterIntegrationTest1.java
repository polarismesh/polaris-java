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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.protobuf.Any;
import com.google.protobuf.StringValue;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.config.consumer.ServiceRouterConfig;
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
import com.tencent.polaris.metadata.core.MetadataContainer;
import com.tencent.polaris.metadata.core.MetadataType;
import com.tencent.polaris.metadata.core.TransitiveType;
import com.tencent.polaris.metadata.core.constant.TsfMetadataConstants;
import com.tencent.polaris.metadata.core.manager.MetadataContext;
import com.tencent.polaris.metadata.core.manager.MetadataContextHolder;
import com.tencent.polaris.plugins.connector.consul.service.lane.LaneService;
import com.tencent.polaris.plugins.router.lane.BaseLaneMode;
import com.tencent.polaris.plugins.router.lane.LaneRouterConfig;
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

import static com.tencent.polaris.metadata.core.constant.TsfMetadataConstants.TSF_APPLICATION_ID;
import static com.tencent.polaris.metadata.core.constant.TsfMetadataConstants.TSF_GROUP_ID;
import static com.tencent.polaris.metadata.core.constant.TsfMetadataConstants.TSF_NAMESPACE_ID;

/**
 * LaneRouter in TSF 集成测试1
 *
 * 场景说明：
 * - caller (主调服务) 作为泳道入口，负责根据请求头判断是否需要染色
 * - callee (被调服务) 作为下游服务，接收染色后的请求并路由到对应泳道实例。实例均在泳道内
 */
public class TsfLaneRouterIntegrationTest1 {

    private static final String POLARIS_SERVER_ADDRESS_PROPERTY = "POLARIS_SEVER_ADDRESS";
    private static final String NAMESPACE = "namespace-xxx";
    private static final String CALLER_SERVICE = "CallerService";  // 主调服务，泳道入口
    private static final String CALLEE_SERVICE = "CalleeService";  // 被调服务，下游服务
    private static final String GRAY_LANE = "gray";

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
    private String insCalleeGroupId = "group-id-callee-1";
    private String ruleCalleeGroupId = insCalleeGroupId;

    @Before
    public void setUp() {
        try {
            // 1. 启动 Mock NamingServer
            namingServer = NamingServer.startNamingServer(-1);
            System.setProperty(POLARIS_SERVER_ADDRESS_PROPERTY, String.format("127.0.0.1:%d", namingServer.getPort()));

            // 2. 创建 SDK 上下文和 API
            Configuration configuration = TestUtils.configWithEnvAddress();
            LaneRouterConfig laneRouterConfig = configuration.getConsumer().getServiceRouter().getPluginConfig(ServiceRouterConfig.DEFAULT_ROUTER_LANE, LaneRouterConfig.class);
            laneRouterConfig.setBaseLaneMode(BaseLaneMode.EXCLUDE_ENABLED_LANE_INSTANCE);

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
        meta.put(TSF_GROUP_ID, insCalleeGroupId);
        parameter.setMetadata(meta);
        namingServer.getNamingService().addInstance(calleeServiceKey, new Node("127.0.0.1", 8080), parameter);
        namingServer.getNamingService().addInstance(calleeServiceKey, new Node("127.0.0.1", 8081), parameter);
    }

    /**
     * 设置泳道规则
     * 入口服务（entries）: caller - 负责染色
     * 目标服务（destinations）: callee - 接收染色请求并路由
     */
    private void setupLaneRules() {
        // 构建 gray 泳道规则 - 匹配 header x-lane=gray
        LaneProto.LaneRule grayRule = buildLaneRule(
                "lane-id-1",
                LaneService.TSF_LANE_GROUP_NAME,
                GRAY_LANE,
                "user",
                "aaa",
                Arrays.asList(callerGroupId, ruleCalleeGroupId),
                1
        );

        Map<String, String> metadata = new HashMap<>();
        metadata.put(NAMESPACE + "," + callerApplicationId, TsfMetadataConstants.TSF_NAMESPACE_ID + "," + TsfMetadataConstants.TSF_APPLICATION_ID);
        metadata.put(NAMESPACE + "," + ruleCalleeApplicationId, TsfMetadataConstants.TSF_NAMESPACE_ID + "," + TsfMetadataConstants.TSF_APPLICATION_ID);

        // 构建泳道组：入口服务为 caller，目标服务为 callee
        LaneProto.LaneGroup laneGroup = buildLaneGroup(
                "tsf",
                "*",
                "*",   // 入口服务（entries）
                "*",   // 目标服务（destinations）
                metadata,
                Arrays.asList(grayRule)
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
     * 当请求没有标签时，应该路由到基线实例
     */
    @Test
    public void testBaselineRouting() throws InterruptedException {
        // 等待服务发现数据同步
        Thread.sleep(3000);

        // 获取 callee 服务的所有实例
        ServiceInstances serviceInstances = getAllInstances(CALLEE_SERVICE);
        Assert.assertNotNull(serviceInstances);
        Assert.assertEquals(2, serviceInstances.getInstances().size());

        for (int i = 0; i < 10; i++) {
            // 构建路由请求（不设置泳道标签）- caller 调用 callee
            ProcessRoutersRequest request = buildRouterRequest(serviceInstances, null, null);

            // 执行路由
            ProcessRoutersResponse response = routerAPI.processRouters(request);

            // 验证结果：应该返回基线实例
            List<Instance> routedInstances = response.getServiceInstances().getInstances();
            Assert.assertNotNull(routedInstances);
            Assert.assertEquals(0, routedInstances.size());
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
        Assert.assertNotNull(serviceInstances);
        Assert.assertEquals(2, serviceInstances.getInstances().size());

        for (int i = 0; i < 10; i++) {
            // 构建路由请求（不设置泳道标签）- caller 调用 callee
            ProcessRoutersRequest request = buildRouterRequest(serviceInstances, "user", "error");

            // 执行路由
            ProcessRoutersResponse response = routerAPI.processRouters(request);

            // 验证结果：应该返回基线实例
            List<Instance> routedInstances = response.getServiceInstances().getInstances();
            Assert.assertNotNull(routedInstances);
            Assert.assertEquals(0, routedInstances.size());
        }
    }

    /**
     * 测试 gray 泳道路由
     */
    @Test
    public void testGrayLaneRouting() throws InterruptedException {
        // 等待服务发现数据同步
        Thread.sleep(3000);

        // 获取 callee 服务的所有实例
        ServiceInstances serviceInstances = getAllInstances(CALLEE_SERVICE);
        Assert.assertNotNull(serviceInstances);
        Assert.assertEquals(2, serviceInstances.getInstances().size());

        for (int i = 0; i < 10; i++) {
            ProcessRoutersRequest request = buildRouterRequest(serviceInstances, "user", "aaa");

            // 执行路由
            ProcessRoutersResponse response = routerAPI.processRouters(request);

            // 验证结果：应该返回 gray 泳道实例
            List<Instance> routedInstances = response.getServiceInstances().getInstances();
            Assert.assertNotNull(routedInstances);
            Assert.assertEquals(2, routedInstances.size());

            // 验证返回的实例是 gray 泳道实例
            int[] ports = routedInstances.stream().mapToInt(Instance::getPort).sorted().toArray();
            Assert.assertArrayEquals(new int[] {8080, 8081}, ports);

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

    /**
     * 构建带有请求头匹配的泳道规则
     */
    private LaneProto.LaneRule buildLaneRule(String ruleId,
                                        String groupName,
                                        String laneName,
                                        String customKey,
                                        String customValue,
                                        List<String> groupIds,
                                        int priority) {
        // 构建匹配条件
        ModelProto.MatchString matchString = ModelProto.MatchString.newBuilder()
                .setType(ModelProto.MatchString.MatchStringType.EXACT)
                .setValue(StringValue.newBuilder().setValue(customValue).build())
                .build();

        // 构建参数匹配
        RoutingProto.SourceMatch sourceMatch = RoutingProto.SourceMatch.newBuilder()
                .setType(RoutingProto.SourceMatch.Type.CUSTOM)
                .setKey(customKey)
                .setValue(matchString)
                .build();

        // 构建流量匹配规则
        LaneProto.TrafficMatchRule trafficMatchRule = LaneProto.TrafficMatchRule.newBuilder()
                .addArguments(sourceMatch)
                .build();

        // 构建泳道规则
        return LaneProto.LaneRule.newBuilder()
                .setId(ruleId)
                .setName(ruleId)
                .setGroupName(groupName)
                .setEnable(true)
                .setDefaultLabelValue(String.join(",", groupIds))
                .setMatchMode(LaneService.TSF_LANE_MATCH_MODE)
                .setTrafficMatchRule(trafficMatchRule)
                .setLabelKey(TsfMetadataConstants.TSF_GROUP_ID)
                .setPriority(priority)
                .build();
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
                                                Map<String, String> metadata,
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
        builder.putAllMetadata(metadata);

        // 添加规则
        if (rules != null) {
            builder.addAllRules(rules);
        }

        return builder.build();
    }
}