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
import com.tencent.polaris.api.exception.ServerCodes;
import com.tencent.polaris.api.exception.ServerErrorResponseException;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.common.ValueContext;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.compose.ServerServiceInfo;
import com.tencent.polaris.api.plugin.skill.SkillDownloadRequest;
import com.tencent.polaris.api.plugin.skill.SkillDownloadResponse;
import com.tencent.polaris.api.plugin.skill.SkillGetRequest;
import com.tencent.polaris.api.plugin.skill.SkillGetResponse;
import com.tencent.polaris.api.plugin.skill.SkillListRequest;
import com.tencent.polaris.api.plugin.skill.SkillListResponse;
import com.tencent.polaris.client.pojo.Node;
import com.tencent.polaris.factory.config.ai.AiConfigImpl;
import com.tencent.polaris.factory.config.global.ClusterConfigImpl;
import com.tencent.polaris.factory.config.skill.SkillConfigImpl;
import com.tencent.polaris.factory.config.skill.SkillConnectorConfigImpl;
import com.tencent.polaris.plugins.connector.grpc.Connection;
import com.tencent.polaris.plugins.connector.grpc.ConnectionManager;
import com.tencent.polaris.specification.api.v1.skill.manage.PolarisSkillGrpc;
import com.tencent.polaris.specification.api.v1.skill.manage.PolarisSkillGRPCService;
import io.grpc.Deadline;
import io.grpc.ManagedChannel;
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
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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

    private Connection lastConnection;

    @Before
    public void setUp() throws PolarisException {
        SkillConnectorConfigImpl skillConnector = new SkillConnectorConfigImpl();
        skillConnector.setAddresses(Collections.singletonList(SKILL_ADDRESS));
        skillConnector.setConnectTimeout(1000L);
        skillConnector.setMessageTimeout(5000L);
        skillConnector.setDownloadTimeout(30000L);
        skillConnector.setServerSwitchInterval(600000L);
        skillConnector.setProtocol("grpc");
        SkillConfigImpl skillConfig = new SkillConfigImpl();
        skillConfig.setServerConnector(skillConnector);
        AiConfigImpl aiConfig = new AiConfigImpl();
        aiConfig.setSkill(skillConfig);
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
        when(configuration.getAi()).thenReturn(aiConfig);
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
     * 测试场景：InitContext 已有 SERVICE_DISCOVER_CLUSTER，ai.skill.addresses 为 8094。
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

    /**
     * 测试目的：插件元数据
     * 测试场景：未 init
     * 验证内容：name/type 正确，destroy 不抛异常
     */
    @Test
    public void testNameTypeAndDestroyWithoutInit() {
        // Act
        connector.destroy();

        // Assert
        assertThat(connector.getName()).isEqualTo("polaris");
        assertThat(connector.getType()).isEqualTo(PluginTypes.SKILL_CONNECTOR.getBaseType());
    }

    /**
     * 测试目的：GetSkill 成功与 NOT_FOUND 返回响应
     * 测试场景：mock blocking stub
     * 验证内容：content 与 code 正确
     */
    @Test
    public void testGetSkillSuccessAndNotFound() throws Exception {
        // Arrange
        PolarisSkillGrpc.PolarisSkillBlockingStub stub = prepareStub();
        SkillGetRequest request = new SkillGetRequest();
        request.setNamespace("default");
        request.setName("weather");
        PolarisSkillGRPCService.GetSkillResponse ok = PolarisSkillGRPCService.GetSkillResponse.newBuilder()
                .setCode(ServerCodes.EXECUTE_SUCCESS)
                .setContent("# skill")
                .build();
        PolarisSkillGRPCService.GetSkillResponse missing = PolarisSkillGRPCService.GetSkillResponse.newBuilder()
                .setCode(ServerCodes.NOT_FOUND_RESOURCE)
                .setInfo("gone")
                .build();
        PolarisSkillGRPCService.GetSkillResponse zero = PolarisSkillGRPCService.GetSkillResponse.newBuilder()
                .setCode(0)
                .setContent("zero")
                .build();
        when(stub.getSkill(any())).thenReturn(ok, missing, zero);

        // Act
        SkillGetResponse success = connector.getSkill(request);
        SkillGetResponse notFound = connector.getSkill(request);
        SkillGetResponse codeZero = connector.getSkill(request);

        // Assert
        assertThat(success.getContent()).isEqualTo("# skill");
        assertThat(notFound.getCode()).isEqualTo(ServerCodes.NOT_FOUND_RESOURCE);
        assertThat(codeZero.getContent()).isEqualTo("zero");
        verify(stub, times(3)).withDeadlineAfter(5000L, TimeUnit.MILLISECONDS);
    }

    /**
     * 测试目的：非预期业务码抛 ServerErrorResponseException
     * 测试场景：code=500000
     * 验证内容：异常类型为 ServerErrorResponseException
     */
    @Test
    public void testGetSkillUnexpectedCode() throws Exception {
        // Arrange
        PolarisSkillGrpc.PolarisSkillBlockingStub stub = prepareStub();
        SkillGetRequest request = new SkillGetRequest();
        request.setNamespace("default");
        request.setName("weather");
        when(stub.getSkill(any())).thenReturn(PolarisSkillGRPCService.GetSkillResponse.newBuilder()
                .setCode(500000)
                .setInfo("boom")
                .build());

        // Act & Assert
        assertThatThrownBy(() -> connector.getSkill(request)).isInstanceOf(ServerErrorResponseException.class);
    }

    /**
     * 测试目的：List/Download 走 BUILTIN 并组装结果
     * 测试场景：list 成功，download 成功，download 错误码
     * 验证内容：total 与 zip 正确，错误码抛异常
     */
    @Test
    public void testListAndDownloadSkill() throws Exception {
        // Arrange
        PolarisSkillGrpc.PolarisSkillBlockingStub stub = prepareStub();
        SkillListRequest listRequest = new SkillListRequest();
        listRequest.setNamespace("default");
        when(stub.getSkillList(any())).thenReturn(PolarisSkillGRPCService.ListSkillsResponse.newBuilder()
                .setCode(ServerCodes.EXECUTE_SUCCESS)
                .setTotal(4)
                .build());
        SkillDownloadRequest downloadRequest = new SkillDownloadRequest();
        downloadRequest.setNamespace("default");
        downloadRequest.setName("weather");
        downloadRequest.setFormat("zip");
        PolarisSkillGRPCService.DownloadSkillResponse frame = PolarisSkillGRPCService.DownloadSkillResponse.newBuilder()
                .setCode(ServerCodes.EXECUTE_SUCCESS)
                .setFilename("weather.zip")
                .setZipChunk(com.google.protobuf.ByteString.copyFromUtf8("ab"))
                .build();
        when(stub.downloadSkill(any())).thenReturn(Collections.singletonList(frame).iterator())
                .thenReturn(Collections.singletonList(PolarisSkillGRPCService.DownloadSkillResponse.newBuilder()
                        .setCode(500000)
                        .setInfo("bad")
                        .build()).iterator());

        // Act
        SkillListResponse listResponse = connector.listSkills(listRequest);
        SkillDownloadResponse downloadResponse = connector.downloadSkill(downloadRequest);

        // Assert
        assertThat(listResponse.getTotal()).isEqualTo(4);
        assertThat(downloadResponse.getFilename()).isEqualTo("weather.zip");
        assertThatThrownBy(() -> connector.downloadSkill(downloadRequest))
                .isInstanceOf(ServerErrorResponseException.class);
        verify(stub).withDeadlineAfter(5000L, TimeUnit.MILLISECONDS);
        verify(stub, times(2)).withDeadlineAfter(30000L, TimeUnit.MILLISECONDS);
    }

    /**
     * 测试目的：markdown 下载继续使用普通消息超时
     * 测试场景：DownloadSkill format=markdown
     * 验证内容：deadline 使用 messageTimeout
     */
    @Test
    public void testMarkdownDownloadUsesMessageTimeout() throws Exception {
        // Arrange
        PolarisSkillGrpc.PolarisSkillBlockingStub stub = prepareStub();
        SkillDownloadRequest request = new SkillDownloadRequest();
        request.setNamespace("default");
        request.setName("weather");
        request.setFormat("markdown");
        PolarisSkillGRPCService.DownloadSkillResponse frame =
                PolarisSkillGRPCService.DownloadSkillResponse.newBuilder()
                        .setCode(ServerCodes.EXECUTE_SUCCESS)
                        .setContent("# weather")
                        .build();
        when(stub.downloadSkill(any())).thenReturn(Collections.singletonList(frame).iterator());

        // Act
        SkillDownloadResponse response = connector.downloadSkill(request);

        // Assert
        assertThat(response.getContent()).isEqualTo("# weather");
        verify(stub).withDeadlineAfter(5000L, TimeUnit.MILLISECONDS);
    }

    /**
     * 测试目的：ListSkills 非预期码抛异常
     * 测试场景：code=500000
     * 验证内容：ServerErrorResponseException
     */
    @Test
    public void testListSkillsUnexpectedCode() throws Exception {
        // Arrange
        PolarisSkillGrpc.PolarisSkillBlockingStub stub = prepareStub();
        when(stub.getSkillList(any())).thenReturn(PolarisSkillGRPCService.ListSkillsResponse.newBuilder()
                .setCode(500000)
                .setInfo("boom")
                .build());

        // Act & Assert
        assertThatThrownBy(() -> connector.listSkills(new SkillListRequest()))
                .isInstanceOf(ServerErrorResponseException.class);
    }

    /**
     * 测试目的：RPC 运行时异常上报失败并包装 NETWORK_ERROR
     * 测试场景：stub.getSkill 抛 RuntimeException
     * 验证内容：connection.reportFail，异常 code 为 NETWORK_ERROR
     */
    @Test
    public void testGetSkillReportsFailOnRuntimeException() throws Exception {
        // Arrange
        PolarisSkillGrpc.PolarisSkillBlockingStub stub = prepareStub();
        when(stub.getSkill(any())).thenThrow(new RuntimeException("rpc down"));
        SkillGetRequest request = new SkillGetRequest();
        request.setNamespace("default");
        request.setName("weather");

        // Act & Assert
        assertThatThrownBy(() -> connector.getSkill(request))
                .isInstanceOf(PolarisException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NETWORK_ERROR);
        verify(lastConnection).reportFail(ErrorCode.NETWORK_ERROR);
        verify(lastConnection).release("GetSkill");
    }

    /**
     * 测试目的：list/download 非 Polar is 异常同样包装
     * 测试场景：stub 抛 RuntimeException
     * 验证内容：NETWORK_ERROR
     */
    @Test
    public void testListAndDownloadWrapRuntimeException() throws Exception {
        // Arrange
        PolarisSkillGrpc.PolarisSkillBlockingStub stub = prepareStub();
        when(stub.getSkillList(any())).thenThrow(new RuntimeException("list down"));
        when(stub.downloadSkill(any())).thenThrow(new RuntimeException("download down"));

        // Act & Assert
        assertThatThrownBy(() -> connector.listSkills(new SkillListRequest()))
                .isInstanceOf(PolarisException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NETWORK_ERROR);
        SkillDownloadRequest downloadRequest = new SkillDownloadRequest();
        downloadRequest.setNamespace("default");
        downloadRequest.setName("weather");
        assertThatThrownBy(() -> connector.downloadSkill(downloadRequest))
                .isInstanceOf(PolarisException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NETWORK_ERROR);
    }

    /**
     * 测试目的：真实 newStub 只挂 header/token，不设置统一 deadline
     * 测试场景：init 后传入 mock Connection
     * 验证内容：返回非空 stub，deadline 由具体 RPC 设置
     */
    @Test
    public void testNewStubOnMockChannel() throws PolarisException {
        // Arrange
        connector.init(initContext);
        Connection connection = mock(Connection.class);
        when(connection.getChannel()).thenReturn(mock(ManagedChannel.class));

        // Act
        PolarisSkillGrpc.PolarisSkillBlockingStub stub = connector.newStub(connection);

        // Assert
        assertThat(stub).isNotNull();
        Deadline deadline = stub.getCallOptions().getDeadline();
        assertThat(deadline).isNull();
    }

    /**
     * 测试目的：非 PolarisException 包装成 NETWORK_ERROR
     * 测试场景：getConnection 抛 RuntimeException
     * 验证内容：Retriable/PolarisException NETWORK_ERROR
     */
    @Test
    public void testGetSkillWrapsUnexpectedThrowable() throws Exception {
        // Arrange
        connector.init(initContext);
        ConnectionManager realManager = getPrivateField(connector, "connectionManager");
        ConnectionManager mockManager = mock(ConnectionManager.class);
        when(mockManager.getConnection(anyString(), eq(ClusterType.BUILTIN_CLUSTER)))
                .thenThrow(new IllegalStateException("broken"));
        setPrivateField(connector, "connectionManager", mockManager);
        realManager.destroy();
        SkillGetRequest request = new SkillGetRequest();
        request.setNamespace("default");
        request.setName("weather");

        // Act & Assert
        assertThatThrownBy(() -> connector.getSkill(request))
                .isInstanceOf(PolarisException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NETWORK_ERROR);
    }

    /**
     * 测试目的：postContextInit 把 Extensions 交给 ConnectionManager
     * 测试场景：init 后调用
     * 验证内容：不抛异常
     */
    @Test
    public void testPostContextInit() throws PolarisException {
        // Arrange
        connector.init(initContext);
        Extensions extensions = mock(Extensions.class);

        // Act
        connector.postContextInit(extensions);

        // Assert
        assertThat(connector.getName()).isEqualTo("polaris");
    }

    private PolarisSkillGrpc.PolarisSkillBlockingStub prepareStub() throws Exception {
        final PolarisSkillGrpc.PolarisSkillBlockingStub stub = mock(PolarisSkillGrpc.PolarisSkillBlockingStub.class);
        when(stub.withDeadlineAfter(anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(stub);
        connector.destroy();
        connector = new PolarisSkillConnector() {
            @Override
            PolarisSkillGrpc.PolarisSkillBlockingStub newStub(Connection connection) {
                return stub;
            }
        };
        connector.init(initContext);
        ConnectionManager realManager = getPrivateField(connector, "connectionManager");
        ConnectionManager mockManager = mock(ConnectionManager.class);
        lastConnection = mock(Connection.class);
        when(mockManager.getConnection(anyString(), eq(ClusterType.BUILTIN_CLUSTER))).thenReturn(lastConnection);
        setPrivateField(connector, "connectionManager", mockManager);
        realManager.destroy();
        return stub;
    }

    @SuppressWarnings("unchecked")
    private static <T> T getPrivateField(Object object, String fieldName)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = findField(object.getClass(), fieldName);
        field.setAccessible(true);
        T result = (T) field.get(object);
        return result;
    }

    private static void setPrivateField(Object object, String fieldName, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = findField(object.getClass(), fieldName);
        field.setAccessible(true);
        field.set(object, value);
    }

    private static Field findField(Class<?> type, String fieldName) throws NoSuchFieldException {
        Class<?> current = type;
        NoSuchFieldException last = null;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException exception) {
                last = exception;
                current = current.getSuperclass();
            }
        }
        throw last;
    }
}
