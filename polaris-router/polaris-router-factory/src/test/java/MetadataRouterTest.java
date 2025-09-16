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

import static com.tencent.polaris.api.rpc.MetadataFailoverType.METADATAFAILOVERNONE;
import static com.tencent.polaris.api.rpc.MetadataFailoverType.METADATAFAILOVERNOTKEY;
import static com.tencent.polaris.test.common.Consts.NAMESPACE_PRODUCTION;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.GetAllInstancesRequest;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.client.api.BaseEngine;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.client.pojo.Node;
import com.tencent.polaris.discovery.client.api.DefaultConsumerAPI;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import com.tencent.polaris.factory.api.RouterAPIFactory;
import com.tencent.polaris.metadata.core.MetadataContainer;
import com.tencent.polaris.metadata.core.TransitiveType;
import com.tencent.polaris.metadata.core.manager.CalleeMetadataContainerGroup;
import com.tencent.polaris.metadata.core.manager.MetadataContainerGroup;
import com.tencent.polaris.plugins.router.metadata.MetadataRouter;
import com.tencent.polaris.router.api.core.RouterAPI;
import com.tencent.polaris.router.api.rpc.ProcessRoutersRequest;
import com.tencent.polaris.router.api.rpc.ProcessRoutersResponse;
import com.tencent.polaris.test.common.TestUtils;
import com.tencent.polaris.test.mock.discovery.NamingServer;
import com.tencent.polaris.test.mock.discovery.NamingService.InstanceParameter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author starkwen
 * @date 2021/2/25 下午4:56
 */
public class MetadataRouterTest {

    public static final String METADATA_SERVICE = "192000705:65767";
    public static final String METADATA_SERVICE_1 = "192000705:65584";
    private static final String POLARIS_SERVER_ADDRESS_PROPERTY = "POLARIS_SEVER_ADDRESS";
    private static final int TEST_ITERATIONS = 10;
    private NamingServer namingServer;
    private SDKContext sdkContext;
    private RouterAPI routerAPI;
    private ConsumerAPI consumer;

    @Before
    public void before() {
        try {
            namingServer = NamingServer.startNamingServer(-1);
            System.setProperty(POLARIS_SERVER_ADDRESS_PROPERTY, String.format("127.0.0.1:%d", namingServer.getPort()));
            Configuration configuration = TestUtils.configWithEnvAddress();
            sdkContext = SDKContext.initContextByConfig(configuration);
            routerAPI = RouterAPIFactory.createRouterAPIByContext(sdkContext);
            consumer = DiscoveryAPIFactory.createConsumerAPIByContext(sdkContext);

        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        /**
         * 该服务下有四个实例
         * 1. 127.0.0.1:80 不健康 Env-set:1-0
         * 2. 127.0.0.1:70 健康 Env-set:1-0
         * 3. 127.0.0.1:100 健康 Env-set:1-0
         * 4. 127.0.0.1:90 健康
         */
        ServiceKey serviceKey = new ServiceKey(NAMESPACE_PRODUCTION, METADATA_SERVICE);
        InstanceParameter parameter = new InstanceParameter();
        parameter.setWeight(100);
        parameter.setHealthy(false);
        parameter.setIsolated(false);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("Env-set", "1-0");
        parameter.setMetadata(metadata);
        namingServer.getNamingService().addInstance(serviceKey, new Node("127.0.0.1", 80), parameter);
        parameter.setHealthy(true);
        namingServer.getNamingService().addInstance(serviceKey, new Node("127.0.0.1", 70), parameter);
        namingServer.getNamingService().addInstance(serviceKey, new Node("127.0.0.1", 100), parameter);
        parameter.setMetadata(null);
        namingServer.getNamingService().addInstance(serviceKey, new Node("127.0.0.1", 90), parameter);
        /**
         * 该服务下有两个实例
         * 1. 127.0.0.1:80 不健康 Env-set:1-0
         * 2. 127.0.0.1:81 不健康
         */
        ServiceKey serviceKey1 = new ServiceKey(NAMESPACE_PRODUCTION, METADATA_SERVICE_1);
        parameter.setMetadata(metadata);
        parameter.setHealthy(false);
        namingServer.getNamingService().addInstance(serviceKey1, new Node("127.0.0.1", 80), parameter);
        parameter.setMetadata(null);
        namingServer.getNamingService().addInstance(serviceKey1, new Node("127.0.0.1", 81), parameter);
    }

    @After
    public void after() {
        if (null != namingServer) {
            namingServer.terminate();
            System.clearProperty(POLARIS_SERVER_ADDRESS_PROPERTY);
        }
        if (null != sdkContext) {
            sdkContext.destroy();
        }
        if (null != routerAPI) {
            ((BaseEngine) routerAPI).destroy();
        }
        if (null != consumer) {
            consumer.destroy();
        }
    }

    @Test
    public void testNormalScene() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 该服务下有四个实例，其中有两个满足metadata路由(分别为port70，100),80 90端口不满足
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            // 获取该服务下所有实例
            GetAllInstancesRequest getAllInstancesRequest = new GetAllInstancesRequest();
            getAllInstancesRequest.setNamespace(NAMESPACE_PRODUCTION);
            getAllInstancesRequest.setService(METADATA_SERVICE);
            InstancesResponse instancesResponse = consumer.getAllInstances(getAllInstancesRequest);
            ServiceInstances serviceInstances = instancesResponse.getServiceInstances();

            // 服务路由
            ProcessRoutersRequest processRouterRequest = new ProcessRoutersRequest();
            processRouterRequest.setNamespace(NAMESPACE_PRODUCTION);
            processRouterRequest.setService(METADATA_SERVICE);
            ServiceInfo serviceInfo = new ServiceInfo();
            serviceInfo.setNamespace(NAMESPACE_PRODUCTION);
            serviceInfo.setService(METADATA_SERVICE);
            processRouterRequest.setSourceService(serviceInfo);
            MetadataContainerGroup metadataContainerGroup = new CalleeMetadataContainerGroup(
                    "X-Polaris-Metadata-Transitive-");
            MetadataContainer metadataContainer = metadataContainerGroup.getCustomMetadataContainer();
            metadataContainer.putMetadataMapValue(MetadataRouter.ROUTER_TYPE_METADATA, MetadataRouter.KEY_METADATA_KEYS,
                    "Env-set", TransitiveType.NONE);
            metadataContainer.putMetadataStringValue("Env-set", "1-0", TransitiveType.NONE);
            processRouterRequest.setDstInstances(serviceInstances);
            processRouterRequest.setMetadataContainerGroup(metadataContainerGroup);
            ProcessRoutersResponse processRoutersResponse = routerAPI.processRouters(processRouterRequest);
            List<Instance> instanceList = processRoutersResponse.getServiceInstances().getInstances();
            Assert.assertEquals(2, instanceList.size());
            int[] ports = instanceList.stream().mapToInt(Instance::getPort).sorted().toArray();
            Assert.assertArrayEquals(new int[]{70, 100}, ports);
        }
    }

    @Test
    public void testFailoverNoneScene() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 该服务下有四个实例，只有一个80端口实例满足metadata路由，但是不健康，降级策略采用默认不降级
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            // 获取该服务下所有实例
            GetAllInstancesRequest getAllInstancesRequest = new GetAllInstancesRequest();
            getAllInstancesRequest.setNamespace(NAMESPACE_PRODUCTION);
            getAllInstancesRequest.setService(METADATA_SERVICE);
            InstancesResponse instancesResponse = consumer.getAllInstances(getAllInstancesRequest);
            ServiceInstances serviceInstances = instancesResponse.getServiceInstances();
            // 服务路由
            ProcessRoutersRequest processRouterRequest = new ProcessRoutersRequest();
            // 设置metadata路由的降级策略为不降级
            processRouterRequest.setMetadataFailoverType(METADATAFAILOVERNONE);
            processRouterRequest.setNamespace(NAMESPACE_PRODUCTION);
            processRouterRequest.setService(METADATA_SERVICE);
            ServiceInfo serviceInfo = new ServiceInfo();
            serviceInfo.setNamespace(NAMESPACE_PRODUCTION);
            serviceInfo.setService(METADATA_SERVICE);
            processRouterRequest.setSourceService(serviceInfo);
            MetadataContainerGroup metadataContainerGroup = new CalleeMetadataContainerGroup(
                    "X-Polaris-Metadata-Transitive-");
            MetadataContainer metadataContainer = metadataContainerGroup.getCustomMetadataContainer();
            metadataContainer.putMetadataMapValue(MetadataRouter.ROUTER_TYPE_METADATA, MetadataRouter.KEY_METADATA_KEYS,
                    "Env-set", TransitiveType.NONE);
            metadataContainer.putMetadataStringValue("Env-set", "1-1", TransitiveType.NONE);
            processRouterRequest.setDstInstances(serviceInstances);
            processRouterRequest.setMetadataContainerGroup(metadataContainerGroup);
            Assert.assertThrows(PolarisException.class, () -> {
                // 这里应该抛出异常
                ProcessRoutersResponse processRoutersResponse = routerAPI.processRouters(processRouterRequest);
                List<Instance> instanceList = processRoutersResponse.getServiceInstances().getInstances();
            });
        }
    }

    @Test
    // metadata路由的降级策略默认为all
    public void testFailoverAllScene() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 该服务下有四个实例，只有一个80端口实例满足metadata路由，但是不健康，降级策略采用all
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            // 获取该服务下所有实例
            GetAllInstancesRequest getAllInstancesRequest = new GetAllInstancesRequest();
            getAllInstancesRequest.setNamespace(NAMESPACE_PRODUCTION);
            getAllInstancesRequest.setService(METADATA_SERVICE);
            InstancesResponse instancesResponse = consumer.getAllInstances(getAllInstancesRequest);
            ServiceInstances serviceInstances = instancesResponse.getServiceInstances();
            // 服务路由
            ProcessRoutersRequest processRouterRequest = new ProcessRoutersRequest();
            processRouterRequest.setNamespace(NAMESPACE_PRODUCTION);
            processRouterRequest.setService(METADATA_SERVICE);
            ServiceInfo serviceInfo = new ServiceInfo();
            serviceInfo.setNamespace(NAMESPACE_PRODUCTION);
            serviceInfo.setService(METADATA_SERVICE);
            processRouterRequest.setSourceService(serviceInfo);
            MetadataContainerGroup metadataContainerGroup = new CalleeMetadataContainerGroup(
                    "X-Polaris-Metadata-Transitive-");
            MetadataContainer metadataContainer = metadataContainerGroup.getCustomMetadataContainer();
            metadataContainer.putMetadataMapValue(MetadataRouter.ROUTER_TYPE_METADATA, MetadataRouter.KEY_METADATA_KEYS,
                    "Env-set", TransitiveType.NONE);
            metadataContainer.putMetadataStringValue("Env-set", "1-1", TransitiveType.NONE);
            processRouterRequest.setDstInstances(serviceInstances);
            processRouterRequest.setMetadataContainerGroup(metadataContainerGroup);
            ProcessRoutersResponse processRoutersResponse = routerAPI.processRouters(processRouterRequest);
            List<Instance> instanceList = processRoutersResponse.getServiceInstances().getInstances();
            // 这里返回3个是因为不健康的实例在RecoverRouter中被过滤掉了
            Assert.assertEquals(3, instanceList.size());
            int[] ports = instanceList.stream().mapToInt(Instance::getPort).sorted().toArray();
            Assert.assertArrayEquals(new int[]{70, 90, 100}, ports);
        }
    }

    @Test
    public void testFailoverNotKeyScene() {
        // 传入Env-set:1-1 ,应该返回第4个实例，因为前三个都包含Env-set这个key
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 该服务下有四个实例，只有一个80端口实例满足metadata路由，但是不健康，降级策略采用默认不降级
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            // 获取该服务下所有实例
            GetAllInstancesRequest getAllInstancesRequest = new GetAllInstancesRequest();
            getAllInstancesRequest.setNamespace(NAMESPACE_PRODUCTION);
            getAllInstancesRequest.setService(METADATA_SERVICE);
            InstancesResponse instancesResponse = consumer.getAllInstances(getAllInstancesRequest);
            ServiceInstances serviceInstances = instancesResponse.getServiceInstances();
            // 服务路由
            ProcessRoutersRequest processRouterRequest = new ProcessRoutersRequest();
            processRouterRequest.setNamespace(NAMESPACE_PRODUCTION);
            processRouterRequest.setService(METADATA_SERVICE);
            processRouterRequest.setMetadataFailoverType(METADATAFAILOVERNOTKEY);
            ServiceInfo serviceInfo = new ServiceInfo();
            serviceInfo.setNamespace(NAMESPACE_PRODUCTION);
            serviceInfo.setService(METADATA_SERVICE);
            processRouterRequest.setSourceService(serviceInfo);
            MetadataContainerGroup metadataContainerGroup = new CalleeMetadataContainerGroup(
                    "X-Polaris-Metadata-Transitive-");
            MetadataContainer metadataContainer = metadataContainerGroup.getCustomMetadataContainer();
            metadataContainer.putMetadataMapValue(MetadataRouter.ROUTER_TYPE_METADATA, MetadataRouter.KEY_METADATA_KEYS,
                    "Env-set", TransitiveType.NONE);
            metadataContainer.putMetadataStringValue("Env-set", "1-1", TransitiveType.NONE);
            processRouterRequest.setDstInstances(serviceInstances);
            processRouterRequest.setMetadataContainerGroup(metadataContainerGroup);
            ProcessRoutersResponse processRoutersResponse = routerAPI.processRouters(processRouterRequest);
            List<Instance> instanceList = processRoutersResponse.getServiceInstances().getInstances();
            Assert.assertEquals(1, instanceList.size());
            Assert.assertEquals(90, instanceList.get(0).getPort());
            Assert.assertEquals("127.0.0.1", instanceList.get(0).getHost());
        }
    }

    @Test
    public void testFailoverNotKeyScene2() {
        // 传入Env-set:1-1 ,应该返回第二个实例，因为这个实例不包含Env-set这个key
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            // 获取该服务下所有实例
            GetAllInstancesRequest getAllInstancesRequest = new GetAllInstancesRequest();
            getAllInstancesRequest.setNamespace(NAMESPACE_PRODUCTION);
            getAllInstancesRequest.setService(METADATA_SERVICE_1);
            InstancesResponse instancesResponse = consumer.getAllInstances(getAllInstancesRequest);
            ServiceInstances serviceInstances = instancesResponse.getServiceInstances();
            // 服务路由
            ProcessRoutersRequest processRouterRequest = new ProcessRoutersRequest();
            processRouterRequest.setNamespace(NAMESPACE_PRODUCTION);
            processRouterRequest.setService(METADATA_SERVICE_1);
            processRouterRequest.setMetadataFailoverType(METADATAFAILOVERNOTKEY);
            ServiceInfo serviceInfo = new ServiceInfo();
            serviceInfo.setNamespace(NAMESPACE_PRODUCTION);
            serviceInfo.setService(METADATA_SERVICE);
            processRouterRequest.setSourceService(serviceInfo);
            MetadataContainerGroup metadataContainerGroup = new CalleeMetadataContainerGroup(
                    "X-Polaris-Metadata-Transitive-");
            MetadataContainer metadataContainer = metadataContainerGroup.getCustomMetadataContainer();
            metadataContainer.putMetadataMapValue(MetadataRouter.ROUTER_TYPE_METADATA, MetadataRouter.KEY_METADATA_KEYS,
                    "Env-set", TransitiveType.NONE);
            metadataContainer.putMetadataStringValue("Env-set", "1-1", TransitiveType.NONE);
            processRouterRequest.setDstInstances(serviceInstances);
            processRouterRequest.setMetadataContainerGroup(metadataContainerGroup);
            ProcessRoutersResponse processRoutersResponse = routerAPI.processRouters(processRouterRequest);
            List<Instance> instanceList = processRoutersResponse.getServiceInstances().getInstances();
            // metadataRouter仅返回第二个实例。虽然是不健康的，但RecoverRouter触发全死全活，因此路由最终结果还是返回了一个实例
            Assert.assertEquals(1, instanceList.size());
            Assert.assertEquals(81, instanceList.get(0).getPort());
            Assert.assertEquals("127.0.0.1", instanceList.get(0).getHost());
        }
    }
}
