/*
 * Tencent is pleased to support the open source community by making Polaris available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
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

import static org.assertj.core.api.Assertions.assertThat;

import com.tencent.polaris.factory.config.global.ServerConnectorConfigImpl;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import javax.net.ssl.SSLException;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.Test;

/**
 * @author wallezhang
 */
public class ManagedChannelUtilTest {

    @Test
    public void testSetChannelTls() throws SSLException, URISyntaxException {
        ManagedChannelBuilder<?> builder = ManagedChannelBuilder.forAddress("127.0.0.1", 8091).usePlaintext();
        ManagedChannelUtil.setChannelTls(builder, buildTlsCertificates());
        assertThat(builder.getClass().getName()).isEqualTo("io.grpc.netty.NettyChannelBuilder");
        assertThat(builder).isInstanceOf(NettyChannelBuilder.class).extracting("sslContext")
                .asInstanceOf(InstanceOfAssertFactories.type(SslContext.class)).isNotNull();
    }

    private ChannelTlsCertificates buildTlsCertificates() throws URISyntaxException {
        ServerConnectorConfigImpl connectorConfig = new ServerConnectorConfigImpl();
        connectorConfig.setTrustedCAFile(
                getCurrentPath(ManagedChannelUtilTest.class.getClassLoader().getResource("server.crt")));
        connectorConfig.setCertFile(
                getCurrentPath(ManagedChannelUtilTest.class.getClassLoader().getResource("client.crt")));
        connectorConfig.setKeyFile(
                getCurrentPath(ManagedChannelUtilTest.class.getClassLoader().getResource("client.key")));

        return ChannelTlsCertificates.build(connectorConfig);
    }


    private String getCurrentPath(URL url) throws URISyntaxException {
        return Paths.get(url.toURI()).toString();
    }
}