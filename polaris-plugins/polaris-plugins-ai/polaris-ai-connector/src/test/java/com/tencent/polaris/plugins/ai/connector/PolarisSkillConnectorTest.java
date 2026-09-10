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

package com.tencent.polaris.plugins.ai.connector;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.config.global.ClusterType;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.ValueContext;
import com.tencent.polaris.api.plugin.compose.ServerServiceInfo;
import com.tencent.polaris.api.plugin.skill.SkillGetRequest;
import com.tencent.polaris.client.pojo.Node;
import com.tencent.polaris.factory.config.global.ClusterConfigImpl;
import com.tencent.polaris.factory.config.skill.SkillConfigImpl;
import com.tencent.polaris.factory.config.skill.SkillConnectorConfigImpl;
import com.tencent.polaris.plugins.connector.grpc.ConnectionManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link PolarisSkillConnector}.
 *
 * @author Fishtail Fu
 */
@RunWith(MockitoJUnitRunner.class)
public class PolarisSkillConnectorTest {

    private static final String SKILL_ADDRESS = "127.0.0.1:8094";

    private static final int SKILL_PORT = 8094;

    @Mock
    private InitContext initContext;

    @Mock
    private Configuration configuration;

    private PolarisSkillConnector connector;

    @Before
    public void setUp() throws PolarisException {
        SkillConnectorConfigImpl skillConnector = new SkillConnectorConfigImpl();
        skillConnector.setAddresses(Collections.singletonList(SKILL_ADDRESS));
        skillConnector.setConnectTimeout(1000L);
        skillConnector.setServerSwitchInterval(600000L);
        skillConnector.setProtocol("grpc");
        SkillConfigImpl skillConfig = new SkillConfigImpl();
        skillConfig.setServerConnector(skillConnector);
        ValueContext valueContext = new ValueContext();
        valueContext.setClientId("skill-connector-test");
        ClusterConfigImpl discoverCluster = new ClusterConfigImpl();
        discoverCluster.setNamespace("Polaris");
        discoverCluster.setService("polaris.discover");
        discoverCluster.setRouters(Collections.singletonList("metadataRouter"));
        discoverCluster.setLbPolicy("weightedRandom");
        ServerServiceInfo discoverService = new ServerServiceInfo(ClusterType.SERVICE_DISCOVER_CLUSTER,
                discoverCluster);
        when(initContext.getConfig()).thenReturn(configuration);
        when(initContext.getValueContext()).thenReturn(valueContext);
        when(initContext.getServerServices()).thenReturn(Collections.singletonList(discoverService));
        when(configuration.getSkill()).thenReturn(skillConfig);
        connector = new PolarisSkillConnector();
    }

    @After
    public void tearDown() {
        if (connector != null) {
            connector.destroy();
        }
    }

    /**
     * 测试 discover 系统服务存在时仍把 Skill 地址挂到 BUILTIN 集群。
     * 测试目的：Skill 选址不得被 polaris.discover 覆盖。
     * 测试场景：InitContext 已有 SERVICE_DISCOVER_CLUSTER，skill.addresses 为 8094。
     * 验证内容：BUILTIN 节点端口为 8094，discover 集群仍绑定系统服务。
     */
    @Test
    public void testInitKeepsSkillAddressWhenDiscoverClusterPresent() throws Exception {
        // Arrange
        connector.init(initContext);
        ConnectionManager connectionManager = getPrivateField(connector, "connectionManager");
        Map<ClusterType, Object> serverAddresses = getPrivateField(connectionManager, "serverAddresses");

        // Act
        Object builtinCluster = serverAddresses.get(ClusterType.BUILTIN_CLUSTER);
        List<Node> builtinNodes = getPrivateField(builtinCluster, "nodes");
        Object discoverCluster = serverAddresses.get(ClusterType.SERVICE_DISCOVER_CLUSTER);
        Object discoverService = getPrivateField(discoverCluster, "serverServiceInfo");

        // Assert
        assertThat(builtinNodes).hasSize(1);
        assertThat(builtinNodes.get(0).getPort()).isEqualTo(SKILL_PORT);
        assertThat(discoverService).isNotNull();
    }

    /**
     * 测试 RPC 取连接走 BUILTIN 而不是 discover。
     * 测试目的：配了 discover 集群时 GetSkill 仍打 Skill 地址对应的内置集群。
     * 测试场景：mock ConnectionManager，discover 系统服务已注入。
     * 验证内容：getConnection 的 clusterType 为 BUILTIN_CLUSTER。
     */
    @Test
    public void testGetSkillUsesBuiltinClusterWhenDiscoverClusterPresent() throws Exception {
        // Arrange
        connector.init(initContext);
        ConnectionManager realManager = getPrivateField(connector, "connectionManager");
        ConnectionManager mockManager = mock(ConnectionManager.class);
        when(mockManager.getConnection(anyString(), eq(ClusterType.BUILTIN_CLUSTER)))
                .thenThrow(new PolarisException(ErrorCode.NETWORK_ERROR, "skill endpoint down"));
        setPrivateField(connector, "connectionManager", mockManager);
        realManager.destroy();
        SkillGetRequest request = new SkillGetRequest();
        request.setNamespace("default");
        request.setName("weather");

        // Act & Assert
        assertThatThrownBy(() -> connector.getSkill(request))
                .isInstanceOf(PolarisException.class);
        verify(mockManager).getConnection("GetSkill", ClusterType.BUILTIN_CLUSTER);
    }

    @SuppressWarnings("unchecked")
    private static <T> T getPrivateField(Object object, String fieldName)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        T result = (T) field.get(object);
        return result;
    }

    private static void setPrivateField(Object object, String fieldName, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(object, value);
    }
}
