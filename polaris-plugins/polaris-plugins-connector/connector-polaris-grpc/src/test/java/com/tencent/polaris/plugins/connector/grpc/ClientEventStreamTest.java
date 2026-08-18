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

package com.tencent.polaris.plugins.connector.grpc;

import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.plugin.server.ClientEventHandler;
import com.tencent.polaris.factory.config.global.ServerConnectorConfigImpl;
import com.tencent.polaris.specification.api.v1.service.manage.ClientProto.ClientEvent;
import com.tencent.polaris.specification.api.v1.service.manage.PolarisGRPCGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;

/**
 * Test for {@link ClientEventStream}.
 *
 * @author evelynwei
 */
@RunWith(MockitoJUnitRunner.class)
public class ClientEventStreamTest {

    @Mock
    private GrpcConnector grpcConnector;

    @Mock
    private Connection connection;

    @Mock
    private ClientEventHandler handler;

    @Mock
    private StreamObserver<ClientEvent> requestObserver;

    @Mock
    private ManagedChannel channel;

    @Mock
    private ServerConnectorConfigImpl connectorConfig;

    private ClientEventStream stream;

    @Before
    public void setUp() throws Exception {
        Mockito.when(grpcConnector.getClientInstanceId()).thenReturn("client-id");
        stream = new ClientEventStream(grpcConnector, connection, handler);
        setRequestObserver(stream, requestObserver);
    }

    @After
    public void tearDown() {
        stream.closeStream(false);
    }

    /**
     * 测试目的：PUSH 处理结果按原 index 回传 ACK。
     * 测试场景：handler 正常返回 JSON。
     * 验证内容：ACK 类型、index、clientId 和 content 正确。
     */
    @Test
    public void testPushReturnsAckWithSameIndex() {
        Mockito.when(handler.onPush(7L, "query")).thenReturn("{\"applied\":true}");

        stream.onNext(push(7L, "query"));

        ArgumentCaptor<ClientEvent> captor = ArgumentCaptor.forClass(ClientEvent.class);
        Mockito.verify(requestObserver, Mockito.timeout(2000)).onNext(captor.capture());
        ClientEvent ack = captor.getValue();
        Assertions.assertThat(ack.getType()).isEqualTo(ClientEvent.ClientEventType.ACK);
        Assertions.assertThat(ack.getIndex()).isEqualTo(7L);
        Assertions.assertThat(ack.getClientId()).isEqualTo("client-id");
        Assertions.assertThat(ack.getContent()).isEqualTo("{\"applied\":true}");
        Mockito.verify(grpcConnector).noteClientEventSuccess();
    }

    /**
     * 测试目的：建流后立即发送 WATCH 首帧自证身份。
     * 测试场景：gRPC stub 成功建立双向流。
     * 验证内容：首帧类型为 WATCH，clientId 与 ReportClient 一致。
     */
    @Test
    public void testStartSendsWatchFrame() {
        PolarisGRPCGrpc.PolarisGRPCStub stub = Mockito.mock(PolarisGRPCGrpc.PolarisGRPCStub.class);
        Mockito.when(connection.getChannel()).thenReturn(channel);
        Mockito.when(grpcConnector.getConnectorConfig()).thenReturn(connectorConfig);
        Mockito.when(connectorConfig.getToken()).thenReturn("token");
        Mockito.when(stub.watchClientEvents(stream)).thenReturn(requestObserver);
        try (MockedStatic<PolarisGRPCGrpc> grpc = Mockito.mockStatic(PolarisGRPCGrpc.class);
                MockedStatic<GrpcUtil> grpcUtil = Mockito.mockStatic(GrpcUtil.class)) {
            grpc.when(() -> PolarisGRPCGrpc.newStub(channel)).thenReturn(stub);
            grpcUtil.when(() -> GrpcUtil.attachAccessToken("token", stub)).thenReturn(stub);

            stream.start();

            ArgumentCaptor<ClientEvent> captor = ArgumentCaptor.forClass(ClientEvent.class);
            Mockito.verify(requestObserver).onNext(captor.capture());
            Assertions.assertThat(captor.getValue().getType()).isEqualTo(ClientEvent.ClientEventType.WATCH);
            Assertions.assertThat(captor.getValue().getClientId()).isEqualTo("client-id");
        }
    }

    /**
     * 测试目的：handler 异常时仍回降级 ACK。
     * 测试场景：handler 抛运行时异常。
     * 验证内容：相同 index 收到 internal_error。
     */
    @Test
    public void testHandlerFailureReturnsFallbackAck() {
        Mockito.when(handler.onPush(8L, "query")).thenThrow(new RuntimeException("boom"));

        stream.onNext(push(8L, "query"));

        ArgumentCaptor<ClientEvent> captor = ArgumentCaptor.forClass(ClientEvent.class);
        Mockito.verify(requestObserver, Mockito.timeout(2000)).onNext(captor.capture());
        Assertions.assertThat(captor.getValue().getIndex()).isEqualTo(8L);
        Assertions.assertThat(captor.getValue().getContent())
                .isEqualTo("{\"applied\":false,\"reason\":\"internal_error\"}");
    }

    /**
     * 测试目的：NOT_FOUND 启动竞态不污染共享连接健康状态。
     * 测试场景：事件流返回 NOT_FOUND。
     * 验证内容：记录失败并重建，但不 reportFail。
     */
    @Test
    public void testNotFoundDoesNotReportConnectionFailure() {
        Throwable error = Status.NOT_FOUND.asRuntimeException();

        stream.onError(error);

        Mockito.verify(grpcConnector).noteClientEventFailure(error);
        Mockito.verify(grpcConnector).retryClientEventStream(stream);
        Mockito.verify(connection, Mockito.never()).reportFail(Mockito.any(ErrorCode.class));
    }

    /**
     * 测试目的：明确网络错误仍上报共享连接故障。
     * 测试场景：事件流返回 UNAVAILABLE。
     * 验证内容：reportFail(NETWORK_ERROR) 且触发重建。
     */
    @Test
    public void testUnavailableReportsConnectionFailure() {
        Throwable error = Status.UNAVAILABLE.asRuntimeException();

        stream.onError(error);

        Mockito.verify(connection).reportFail(ErrorCode.NETWORK_ERROR);
        Mockito.verify(grpcConnector).retryClientEventStream(stream);
    }

    /**
     * 测试目的：关流幂等释放连接。
     * 测试场景：连续关闭两次。
     * 验证内容：连接只释放一次。
     */
    @Test
    public void testCloseReleasesConnectionOnce() {
        stream.closeStream(false);
        stream.closeStream(false);

        Mockito.verify(connection).release(GrpcUtil.OP_KEY_WATCH_CLIENT_EVENTS);
    }

    private ClientEvent push(long index, String content) {
        return ClientEvent.newBuilder()
                .setType(ClientEvent.ClientEventType.PUSH)
                .setIndex(index)
                .setContent(content)
                .build();
    }

    private void setRequestObserver(ClientEventStream target, StreamObserver<ClientEvent> observer) throws Exception {
        Field field = ClientEventStream.class.getDeclaredField("requestObserver");
        field.setAccessible(true);
        field.set(target, observer);
    }
}
