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

import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.okhttp.OkHttpChannelBuilder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;

/**
 * Grpc managed channel utils
 *
 * @author wallezhang
 */
public final class ManagedChannelUtil {

    /**
     * Set channel tls certificates in {@link ManagedChannelBuilder}
     *
     * @param builder ManagedChannelBuilder
     * @param tlsCertificates tls certificates
     */
    public static void setChannelTls(ManagedChannelBuilder<?> builder, ChannelTlsCertificates tlsCertificates)
            throws SSLException {
        Objects.requireNonNull(tlsCertificates, "Channel tls certificates can't be null");
        String channelBuilderClassName = builder.getClass().getName();

        // ManagedChannelBuilder does not abstract the ssl interface, so it needs to be converted to a
        // concrete implementation
        if (channelBuilderClassName.equals("io.grpc.netty.NettyChannelBuilder")) {
            NettyChannelBuilder nettyBuilder = (NettyChannelBuilder) builder;
            nettyBuilder.sslContext(GrpcSslContexts.forClient().keyManager(tlsCertificates.getKeyManager())
                    .trustManager(tlsCertificates.getTrustManager()).build());
        } else if (channelBuilderClassName.equals("io.grpc.okhttp.OkHttpChannelBuilder")) {
            OkHttpChannelBuilder okHttpBuilder = (OkHttpChannelBuilder) builder;
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                X509KeyManager keyManager = tlsCertificates.getKeyManager();
                sslContext.init(keyManager == null ? null : new KeyManager[]{keyManager},
                        new TrustManager[]{tlsCertificates.getTrustManager()}, null);
                okHttpBuilder.sslSocketFactory(sslContext.getSocketFactory());
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                throw new SSLException("Could not set SslContext to okHttpBuilder.", e);
            }
        } else {
            throw new SSLException("Unsupported ManagedChannelBuilder: " + channelBuilderClassName);
        }
    }
}
