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

package com.tencent.polaris.client.remote;

import com.tencent.polaris.api.config.consumer.LoadBalanceConfig;
import com.tencent.polaris.api.config.consumer.ServiceRouterConfig;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.impl.PluginManager;
import com.tencent.polaris.api.plugin.loadbalance.LoadBalancer;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.client.flow.BaseFlow;
import com.tencent.polaris.client.pojo.Node;
import com.tencent.polaris.plugins.loadbalancer.roundrobin.WeightedRoundRobinBalance;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Test for {@link ServiceAddressRepository}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class ServiceAddressRepositoryTest {

    private static MockedStatic<BaseFlow> mockedBaseFlow;

    @Mock
    private Extensions extensions;

    @Mock
    private PluginManager pluginManager;

    @Mock
    private static Instance mockInstance;

    private final ServiceKey remoteCluster = new ServiceKey("test-namespace", "test-service");
    private final String clientId = "test-client";

    @BeforeClass
    public static void beforeClass() {
        mockedBaseFlow = Mockito.mockStatic(BaseFlow.class);
    }

    @Before
    public void setUp() {
        when(mockInstance.getHost()).thenReturn("1.2.3.4");
        when(mockInstance.getPort()).thenReturn(8080);
        // Use WeightedRandomBalance as the LoadBalancer
        LoadBalancer weightedRandomBalance = new WeightedRoundRobinBalance();
        when(extensions.getPlugins()).thenReturn(pluginManager);
        when(pluginManager.getPlugin(any(), any())).thenReturn(weightedRandomBalance);
        mockedBaseFlow.when(() -> BaseFlow.processLoadBalance(
                        any(), any(), any(), any()))
                .thenCallRealMethod();

    }

    @AfterClass
    public static void AfterClass() {
        if (mockedBaseFlow != null) {
            mockedBaseFlow.close();
        }
    }

    @Test
    public void testConstructorWithDefaultParams() {
        List<String> addresses = Arrays.asList("host1:8080", "host2:9090");
        ServiceAddressRepository repository = new ServiceAddressRepository(
                addresses, clientId, extensions, remoteCluster);

        assertNotNull(repository);
        assertEquals(2, repository.getNodes().size());
        assertEquals(ServiceRouterConfig.DEFAULT_ROUTER_METADATA, repository.getRouters().get(0));
        assertEquals(ServiceRouterConfig.DEFAULT_ROUTER_NEARBY, repository.getRouters().get(1));
        assertEquals("http", repository.getProtocol());
    }

    @Test
    public void testConstructorWithCustomParams() {
        List<String> addresses = Arrays.asList("host1:8080", "host2:9090");
        List<String> routers = Arrays.asList("custom-router1", "custom-router2");
        String lbPolicy = LoadBalanceConfig.LOAD_BALANCE_WEIGHTED_ROUND_ROBIN;
        String protocol = "grpc";

        ServiceAddressRepository repository = new ServiceAddressRepository(
                addresses, clientId, extensions, remoteCluster, routers, lbPolicy, protocol);

        assertNotNull(repository);
        assertEquals(2, repository.getNodes().size());
        assertEquals("custom-router1", repository.getRouters().get(0));
        assertEquals("custom-router2", repository.getRouters().get(1));
        assertEquals(LoadBalanceConfig.LOAD_BALANCE_WEIGHTED_ROUND_ROBIN, repository.getLbPolicy());
        assertEquals("grpc", repository.getProtocol());
    }

    @Test
    public void testConstructorWithEmptyAddresses() {
        ServiceAddressRepository repository = new ServiceAddressRepository(
                null, clientId, extensions, remoteCluster);

        assertNotNull(repository);
        assertTrue(repository.getNodes().isEmpty());
    }

    @Test
    public void testConstructorWithInvalidAddresses() {
        List<String> addresses = Arrays.asList("host1", "host2:", ":8080", "host3:invalid", "");
        ServiceAddressRepository repository = new ServiceAddressRepository(
                addresses, clientId, extensions, remoteCluster);

        assertNotNull(repository);
        assertTrue(repository.getNodes().isEmpty());
    }

    @Test
    public void testGetServiceAddressNodeWithLocalNodes() throws PolarisException {
        List<String> addresses = Arrays.asList("host1:8080", "host2:9090", "host3:7070");
        ServiceAddressRepository repository = new ServiceAddressRepository(
                addresses, clientId, extensions, remoteCluster);

        // First call
        Node node1 = repository.getServiceAddressNode();
        assertEquals("host1", node1.getHost());
        assertEquals(8080, node1.getPort());

        // Second call - should round robin
        Node node2 = repository.getServiceAddressNode();
        assertEquals("host2", node2.getHost());
        assertEquals(9090, node2.getPort());

        // Third call
        Node node3 = repository.getServiceAddressNode();
        assertEquals("host3", node3.getHost());
        assertEquals(7070, node3.getPort());

        // Fourth call - should wrap around
        Node node4 = repository.getServiceAddressNode();
        assertEquals("host1", node4.getHost());
        assertEquals(8080, node4.getPort());
    }

    @Test
    public void testGetServiceAddressNodeWithEmptyNodes() throws PolarisException {
        ServiceAddressRepository repository = new ServiceAddressRepository(
                Collections.emptyList(), clientId, extensions, remoteCluster);

        mockedBaseFlow.when(() -> BaseFlow.commonGetOneInstance(
                        any(), any(), anyList(), anyString(), anyString(), anyString()))
                .thenReturn(mockInstance);

        Node node = repository.getServiceAddressNode();
        assertEquals("1.2.3.4", node.getHost());
        assertEquals(8080, node.getPort());
    }

    @Test(expected = PolarisException.class)
    public void testGetServiceAddressNodeWithDiscoveryFailure() throws PolarisException {
        ServiceAddressRepository repository = new ServiceAddressRepository(
                Collections.emptyList(), clientId, extensions, remoteCluster);

        mockedBaseFlow.when(() -> BaseFlow.commonGetOneInstance(
                        any(), any(), anyList(), anyString(), anyString(), anyString()))
                .thenThrow(new PolarisException(ErrorCode.INSTANCE_NOT_FOUND, "Discovery failed"));

        repository.getServiceAddressNode();
    }

    @Test
    public void testGetServiceAddress() throws PolarisException {

        List<String> addresses = Arrays.asList("host1:8080", "host2:9090");
        ServiceAddressRepository repository = new ServiceAddressRepository(
                addresses, clientId, extensions, remoteCluster);

        String address1 = repository.getServiceAddress();
        assertTrue(address1.equals("host1:8080") || address1.equals("host2:9090"));

        String address2 = repository.getServiceAddress();
        assertNotEquals(address1, address2); // Should be different due to round robin
    }

    @Test
    public void testGetServiceAddressWithDiscovery() throws PolarisException {
        ServiceAddressRepository repository = new ServiceAddressRepository(
                Collections.emptyList(), clientId, extensions, remoteCluster);

        mockedBaseFlow.when(() -> BaseFlow.commonGetOneInstance(
                        any(), any(), anyList(), anyString(), anyString(), anyString()))
                .thenReturn(mockInstance);

        String address = repository.getServiceAddress();
        assertEquals("1.2.3.4:8080", address);
    }
}
