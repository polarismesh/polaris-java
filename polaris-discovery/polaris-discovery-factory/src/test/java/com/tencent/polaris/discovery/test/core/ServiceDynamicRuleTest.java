package com.tencent.polaris.discovery.test.core;

import static com.tencent.polaris.test.common.Consts.NAMESPACE_PRODUCTION;
import static com.tencent.polaris.test.common.TestUtils.SERVER_ADDRESS_ENV;

import com.google.protobuf.StringValue;
import com.google.protobuf.UInt32Value;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.RouteArgument;
import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.pojo.SourceService;
import com.tencent.polaris.api.rpc.GetInstancesRequest;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.client.pb.ModelProto.MatchString;
import com.tencent.polaris.client.pb.ModelProto.MatchString.MatchStringType;
import com.tencent.polaris.client.pb.RoutingProto.Destination;
import com.tencent.polaris.client.pb.RoutingProto.Route;
import com.tencent.polaris.client.pb.RoutingProto.Routing;
import com.tencent.polaris.client.pb.RoutingProto.Source;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import com.tencent.polaris.test.common.TestUtils;
import com.tencent.polaris.test.mock.discovery.NamingServer;
import com.tencent.polaris.test.mock.discovery.NamingService.InstanceParameter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * 北极星页面中配置的动态路由规则
 * 在上下游服务埋自定义的Metadata数据，这里不同于yaml中的Metadata
 */
public class ServiceDynamicRuleTest {

    public static final String RULE_ROUTER_SERVICE = "tdocs.manage.pay.trpc";
    public static final int MATCH_META_COUNT = 2;
    private NamingServer namingServer;

    @Before
    public void before() {
        try {
            namingServer = NamingServer.startNamingServer(-1);
            System.setProperty(SERVER_ADDRESS_ENV, String.format("127.0.0.1:%d", namingServer.getPort()));
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        ServiceKey serviceKey = new ServiceKey(NAMESPACE_PRODUCTION, RULE_ROUTER_SERVICE);
        InstanceParameter parameter = new InstanceParameter();
        parameter.setWeight(100);
        parameter.setHealthy(true);
        parameter.setIsolated(false);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("env", "base");
        parameter.setMetadata(metadata);
        namingServer.getNamingService().batchAddInstances(serviceKey, 10001, MATCH_META_COUNT, parameter);
        parameter.setMetadata(null);
        namingServer.getNamingService().batchAddInstances(serviceKey, 10010, 8, parameter);
        Map<String, MatchString> data = new HashMap<>();
        data.put("env", MatchString.newBuilder()
                .setType(MatchStringType.EXACT)
                .setValue(StringValue.newBuilder()
                        .setValue("base").build())
                .build());
        Map<String, MatchString> srcData = new HashMap<>();
        srcData.put("uid", MatchString.newBuilder()
                .setType(MatchStringType.EXACT)
                .setValue(StringValue.newBuilder()
                        .setValue("144115217417489762").build())
                .build());
        Routing routing = Routing.newBuilder()
                .addInbounds(Route.newBuilder().addDestinations(Destination.newBuilder().putAllMetadata(data).setWeight(
                        UInt32Value.newBuilder().setValue(100).build()).build())
                        .addSources(Source.newBuilder().setNamespace(StringValue.newBuilder().setValue("*").build())
                                .setService(
                                        StringValue.newBuilder().setValue("*").build()).
                                        putAllMetadata(srcData).build()).build())
                .build();
        namingServer.getNamingService().setRouting(serviceKey, routing);
    }

    @After
    public void after() {
        if (null != namingServer) {
            namingServer.terminate();
        }
    }

    @Test
    public void testServiceDynamicRule() {
        GetInstancesRequest getInstancesRequest = new GetInstancesRequest();
        getInstancesRequest.setNamespace(NAMESPACE_PRODUCTION);
        getInstancesRequest.setService(RULE_ROUTER_SERVICE);

        Map<String, String> map = new HashMap<>();
        map.put("uid", "144115217417489762");

        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.setNamespace(NAMESPACE_PRODUCTION);
        serviceInfo.setService(RULE_ROUTER_SERVICE);
        serviceInfo.setMetadata(map);
        // 设置主调方服务信息 即 Metadata等规则信息
        getInstancesRequest.setServiceInfo(serviceInfo);
        Configuration configuration = TestUtils.configWithEnvAddress();
        try (ConsumerAPI consumerAPI = DiscoveryAPIFactory.createConsumerAPIByConfig(configuration)) {
            InstancesResponse oneInstance = consumerAPI.getInstances(getInstancesRequest);
            Assert.assertEquals(MATCH_META_COUNT, oneInstance.getInstances().length);
        }
    }

    @Test
    public void testServiceDynamicRuleByRouterArgument() {
        GetInstancesRequest getInstancesRequest = new GetInstancesRequest();
        getInstancesRequest.setNamespace(NAMESPACE_PRODUCTION);
        getInstancesRequest.setService(RULE_ROUTER_SERVICE);

        SourceService serviceInfo = new SourceService();
        serviceInfo.setNamespace(NAMESPACE_PRODUCTION);
        serviceInfo.setService(RULE_ROUTER_SERVICE);
        serviceInfo.appendArguments(RouteArgument.buildCustom("uid", "144115217417489762"));
        // 设置主调方服务信息 即 Metadata等规则信息
        getInstancesRequest.setSourceService(serviceInfo);
        Configuration configuration = TestUtils.configWithEnvAddress();
        try (ConsumerAPI consumerAPI = DiscoveryAPIFactory.createConsumerAPIByConfig(configuration)) {
            InstancesResponse oneInstance = consumerAPI.getInstances(getInstancesRequest);
            Assert.assertEquals(MATCH_META_COUNT, oneInstance.getInstances().length);
        }
    }
}