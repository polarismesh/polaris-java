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

import com.tencent.polaris.api.config.global.ServerConnectorConfig;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.logging.LoggerFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.annotation.Nullable;
import javax.net.ssl.SSLException;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import org.slf4j.Logger;

/**
 * Grpc channel certificate info
 *
 * @author wallezhang
 */
public class ChannelTlsCertificates {

    private static final Logger LOG = LoggerFactory.getLogger(ChannelTlsCertificates.class);

    private final X509TrustManager trustManager;
    private final X509KeyManager keyManager;

    public ChannelTlsCertificates(X509TrustManager trustManager, X509KeyManager keyManager) {
        this.trustManager = trustManager;
        this.keyManager = keyManager;
    }

    /**
     * Build channel tls certificate. If no certificate files are configured in the
     * server connector configuration, {@code null} is returned.
     *
     * @param serverConnectorConfig server connector config
     * @return ChannelTlsCertificates
     */
    @Nullable
    public static ChannelTlsCertificates build(ServerConnectorConfig serverConnectorConfig) {
        try {
            X509TrustManager trustManager = null;
            X509KeyManager keyManager = null;
            if (hasTrustedCertificates(serverConnectorConfig)) {
                trustManager = X509ManagerUtil.buildTrustManager(
                        readCertificateFileBytes(serverConnectorConfig.getTrustedCertificate()));
            }

            if (hasClientCertificates(serverConnectorConfig)) {
                keyManager = X509ManagerUtil.buildKeyManager(
                        readCertificateFileBytes(serverConnectorConfig.getClientCertificate()),
                        readCertificateFileBytes(serverConnectorConfig.getClientKey()));
            }

            if (trustManager == null && keyManager == null) {
                return null;
            }
            return new ChannelTlsCertificates(trustManager, keyManager);
        } catch (SSLException e) {
            LOG.error("Build X.509 key/trust manager error. Return null.", e);
        }

        return null;
    }

    private static boolean hasTrustedCertificates(ServerConnectorConfig serverConnectorConfig) {
        return StringUtils.isNotEmpty(serverConnectorConfig.getTrustedCertificate());
    }

    private static boolean hasClientCertificates(ServerConnectorConfig serverConnectorConfig) {
        String clientKeyChain = serverConnectorConfig.getClientKey();
        String clientCertificate = serverConnectorConfig.getClientCertificate();
        if (StringUtils.isEmpty(clientCertificate) && StringUtils.isEmpty(clientKeyChain)) {
            LOG.debug("The server connector configuration has no client certificates and key chain.");
            return false;
        }

        if (StringUtils.isNotEmpty(clientCertificate) && StringUtils.isEmpty(clientKeyChain)) {
            LOG.warn("The server connector configuration has client certificates but not client key chain");
            return false;
        }

        if (StringUtils.isEmpty(clientCertificate) && StringUtils.isNotEmpty(clientKeyChain)) {
            LOG.warn("The server connector configuration has client key chain but not client certificates");
            return false;
        }

        return true;
    }

    private static byte[] readCertificateFileBytes(String filePath) {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new RuntimeException("Invalid Grpc tls certificate path: " + path);
        }
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException("Error reading certificate file: " + path, e);
        }
    }

    public X509TrustManager getTrustManager() {
        return trustManager;
    }

    public X509KeyManager getKeyManager() {
        return keyManager;
    }
}
