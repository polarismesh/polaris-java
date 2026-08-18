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

package com.tencent.polaris.plugins.connector.composite;

import com.tencent.polaris.api.config.plugin.DefaultPlugins;
import com.tencent.polaris.api.plugin.server.ClientEventHandler;
import com.tencent.polaris.plugins.connector.common.DestroyableServerConnector;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.Collections;

/**
 * Test for {@link CompositeConnector}.
 *
 * @author evelynwei
 */
@RunWith(MockitoJUnitRunner.class)
public class CompositeConnectorTest {

    /**
     * 测试目的：组合连接器将客户端事件流委托给内部 gRPC 连接器。
     * 测试场景：组合中包含一个 gRPC 连接器。
     * 验证内容：返回同一流句柄并透传 handler。
     */
    @Test
    public void testWatchClientEventsDelegatesToGrpcConnector() throws Exception {
        CompositeConnector connector = new CompositeConnector();
        DestroyableServerConnector grpcConnector = Mockito.mock(DestroyableServerConnector.class);
        ClientEventHandler handler = Mockito.mock(ClientEventHandler.class);
        AutoCloseable stream = Mockito.mock(AutoCloseable.class);
        Mockito.when(grpcConnector.getName()).thenReturn(DefaultPlugins.SERVER_CONNECTOR_GRPC);
        Mockito.when(grpcConnector.watchClientEvents(handler)).thenReturn(stream);
        setServerConnectors(connector, grpcConnector);

        AutoCloseable actual = connector.watchClientEvents(handler);

        Assertions.assertThat(actual).isSameAs(stream);
        Mockito.verify(grpcConnector).watchClientEvents(handler);
    }

    private void setServerConnectors(CompositeConnector connector, DestroyableServerConnector grpcConnector)
            throws Exception {
        Field field = CompositeConnector.class.getDeclaredField("serverConnectors");
        field.setAccessible(true);
        field.set(connector, Collections.singletonList(grpcConnector));
    }
}
