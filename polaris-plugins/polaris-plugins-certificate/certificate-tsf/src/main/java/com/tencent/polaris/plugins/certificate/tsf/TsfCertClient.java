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

package com.tencent.polaris.plugins.certificate.tsf;

import com.tencent.polaris.api.utils.CertUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.specification.api.v1.security.IstioCertificateServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.okhttp.OkHttpChannelBuilder;
import io.grpc.stub.MetadataUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 * @author Haotian Zhang
 */
public class TsfCertClient {

    private static final Logger log = LoggerFactory.getLogger(TsfCertClient.class);

    private final String address;

    private X509TrustManager trustManager = null;

    private final ManagedChannel channel;

    private final Metadata customMetadata;

    public TsfCertClient(String address, String certPath, String token) {
        this.address = address;
        this.channel = createConnection(address);
        try {
            if (StringUtils.isNotBlank(certPath)) {
                trustManager = CertUtils.buildTrustManager(certPath);
            }
        } catch (Throwable throwable) {
            log.error("init trust manager failed.", throwable);
        }
        customMetadata = new Metadata();
        if (StringUtils.isNotBlank(token)) {
            if (!StringUtils.startsWith(token, "Bearer ")) {
                token = "Bearer " + token;
            }
            customMetadata.put(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER), token);
        }
        log.info("TsfCertClient init success with address: {}, certPath: {}, token: {}", address, certPath, token);
    }

    /**
     * 创建连接
     *
     * @return Connection对象
     */
    private ManagedChannel createConnection(String address) {
        ManagedChannelBuilder<?> builder = ManagedChannelBuilder.forTarget(address)
                .enableRetry()
                .maxRetryAttempts(3);
        if (trustManager != null) {
            try {
                // 使用TLS证书配置
                setChannelTls(builder);
            } catch (SSLException e) {
                log.error("Failed to set TLS certificates, fallback to plaintext", e);
                builder.usePlaintext();
            }
        } else {
            // 无TLS证书时使用明文
            builder.usePlaintext();
        }
        return ManagedChannelBuilder.forTarget(address)
                .usePlaintext()
                .enableRetry()
                .maxRetryAttempts(3)
                .build();
    }

    /**
     * Set channel tls certificates in {@link ManagedChannelBuilder}
     *
     * @param builder ManagedChannelBuilder
     */
    private void setChannelTls(ManagedChannelBuilder<?> builder)
            throws SSLException {
        String channelBuilderClassName = builder.getClass().getName();

        // ManagedChannelBuilder does not abstract the ssl interface, so it needs to be converted to a
        // concrete implementation
        if (channelBuilderClassName.equals("io.grpc.netty.NettyChannelBuilder")) {
            NettyChannelBuilder nettyBuilder = (NettyChannelBuilder) builder;
            nettyBuilder.sslContext(GrpcSslContexts.forClient().trustManager(trustManager).build());
        } else if (channelBuilderClassName.equals("io.grpc.okhttp.OkHttpChannelBuilder")) {
            OkHttpChannelBuilder okHttpBuilder = (OkHttpChannelBuilder) builder;
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[]{trustManager}, null);
                okHttpBuilder.sslSocketFactory(sslContext.getSocketFactory());
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                throw new SSLException("Could not set SslContext to okHttpBuilder.", e);
            }
        } else {
            throw new SSLException("Unsupported ManagedChannelBuilder: " + channelBuilderClassName);
        }
    }

    public IstioCertificateServiceGrpc.IstioCertificateServiceBlockingStub getIstioCertificateServiceStub() {
        IstioCertificateServiceGrpc.IstioCertificateServiceBlockingStub stub = IstioCertificateServiceGrpc.newBlockingStub(channel);
        return stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(customMetadata));
    }

    /**
     * 关闭gRPC客户端连接
     */
    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdownNow();
        }
    }
}
