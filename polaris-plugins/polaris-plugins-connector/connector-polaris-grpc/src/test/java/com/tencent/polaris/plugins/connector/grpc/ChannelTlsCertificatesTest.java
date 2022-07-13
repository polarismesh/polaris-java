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
import java.io.File;
import java.security.Key;
import javax.net.ssl.X509TrustManager;
import org.junit.Before;
import org.junit.Test;

/**
 * @author wallezhang
 */
public class ChannelTlsCertificatesTest {

    private ServerConnectorConfigImpl serverConnectorConfig;

    @Before
    public void setUp() throws Exception {
        serverConnectorConfig = new ServerConnectorConfigImpl();
    }

    @Test
    public void testHasNoCert() {
        ChannelTlsCertificates tlsCertificates = ChannelTlsCertificates.build(serverConnectorConfig);
        assertThat(tlsCertificates).isNull();
    }

    @Test
    public void testHasNoTrustedCert() {
        serverConnectorConfig.setCertFile(
                ChannelTlsCertificatesTest.class.getResource(File.separator + "client.crt").getFile());
        serverConnectorConfig.setKeyFile(
                ChannelTlsCertificatesTest.class.getResource(File.separator + "client.key").getFile());
        ChannelTlsCertificates tlsCertificates = ChannelTlsCertificates.build(serverConnectorConfig);
        assertThat(tlsCertificates).isNotNull();
        assertThat(tlsCertificates.getTrustManager()).isNull();
        assertThat(tlsCertificates.getKeyManager()).isNotNull()
                .extracting(x509KeyManager -> x509KeyManager.getPrivateKey(".0.grpc_client_trusted")).isNotNull()
                .extracting(Key::getAlgorithm).isEqualTo("RSA");
    }

    @Test
    public void testHasNoCertFile() {
        serverConnectorConfig.setTrustedCAFile(
                ChannelTlsCertificatesTest.class.getResource(File.separator + "server.crt").getFile());
        serverConnectorConfig.setKeyFile(
                ChannelTlsCertificatesTest.class.getResource(File.separator + "client.key").getFile());
        ChannelTlsCertificates tlsCertificates = ChannelTlsCertificates.build(serverConnectorConfig);
        assertThat(tlsCertificates).isNotNull().extracting(ChannelTlsCertificates::getTrustManager).isNotNull()
                .extracting(X509TrustManager::getAcceptedIssuers)
                .extracting(x509Certificates -> x509Certificates.length).isEqualTo(1);
        assertThat(tlsCertificates.getKeyManager()).isNull();
    }

    @Test
    public void testHasNoKeyFile() {
        serverConnectorConfig.setTrustedCAFile(
                ChannelTlsCertificatesTest.class.getResource(File.separator + "server.crt").getFile());
        serverConnectorConfig.setCertFile(
                ChannelTlsCertificatesTest.class.getResource(File.separator + "client.crt").getFile());
        ChannelTlsCertificates tlsCertificates = ChannelTlsCertificates.build(serverConnectorConfig);
        assertThat(tlsCertificates).isNotNull().extracting(ChannelTlsCertificates::getTrustManager).isNotNull()
                .extracting(X509TrustManager::getAcceptedIssuers)
                .extracting(x509Certificates -> x509Certificates.length).isEqualTo(1);
        assertThat(tlsCertificates.getKeyManager()).isNull();
    }
}