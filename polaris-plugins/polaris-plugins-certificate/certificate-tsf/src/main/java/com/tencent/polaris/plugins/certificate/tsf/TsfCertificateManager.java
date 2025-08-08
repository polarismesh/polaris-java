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

import com.tencent.polaris.api.config.plugin.DefaultPlugins;
import com.tencent.polaris.api.config.plugin.PluginConfigProvider;
import com.tencent.polaris.api.config.verify.Verifier;
import com.tencent.polaris.api.control.Destroyable;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.certificate.CertificateManager;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.utils.CertUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.specification.api.v1.security.IstioCertificateServiceGrpc;
import com.tencent.polaris.specification.api.v1.security.TsfIstioCertificateProto;
import io.grpc.Deadline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Haotian Zhang
 */
public class TsfCertificateManager extends Destroyable implements CertificateManager, PluginConfigProvider {

    private static final Logger log = LoggerFactory.getLogger(TsfCertificateManager.class);

    private String address;

    private String certPath;

    private String token;

    @Override
    public List<X509Certificate> signCertificate(String csr, long validityDuration) {
        List<X509Certificate> certificateList = new ArrayList<>();
        if (StringUtils.isBlank(address)) {
            log.warn("tsf certificate manager address is empty, skip sign certificate");
            return certificateList;
        }
        TsfCertClient certClient = new TsfCertClient(address, certPath, token);
        try {
            IstioCertificateServiceGrpc.IstioCertificateServiceBlockingStub stub = certClient.getIstioCertificateServiceStub();

            TsfIstioCertificateProto.IstioCertificateRequest.Builder requestBuilder = TsfIstioCertificateProto.IstioCertificateRequest.newBuilder();
            requestBuilder.setCsr(csr);
            requestBuilder.setValidityDuration(validityDuration / 1000);

            if (log.isDebugEnabled()) {
                log.debug("Begin create certificates with validity duration: {} seconds", validityDuration / 1000);
            }

            TsfIstioCertificateProto.IstioCertificateResponse response =
                    stub.withDeadline(Deadline.after(10, TimeUnit.SECONDS)).createCertificate(requestBuilder.build());
            response.getCertChainList().forEach(cert -> {
                if (log.isDebugEnabled()) {
                    log.debug("Certificate: {}", cert);
                }
                certificateList.add(CertUtils.convertToX509Certificate(cert));
            });
        } catch (Throwable throwable) {
            log.warn("create certificate failed.", throwable);
        } finally {
            certClient.shutdown();
        }
        return certificateList;
    }

    @Override
    public String getName() {
        return DefaultPlugins.TSF_CERTIFICATE_MANAGER;
    }

    @Override
    public Class<? extends Verifier> getPluginConfigClazz() {
        return TsfCertificateManagerConfig.class;
    }

    @Override
    public PluginType getType() {
        return PluginTypes.CERTIFICATE_MANAGER.getBaseType();
    }

    @Override
    public void init(InitContext ctx) throws PolarisException {

    }

    @Override
    public void postContextInit(Extensions ctx) throws PolarisException {
        address = ctx.getConfiguration().getGlobal().getCertificate()
                .getPluginConfig(getName(), TsfCertificateManagerConfig.class).getAddress();
        certPath = ctx.getConfiguration().getGlobal().getCertificate()
                .getPluginConfig(getName(), TsfCertificateManagerConfig.class).getCertPath();
        token = ctx.getConfiguration().getGlobal().getCertificate()
                .getPluginConfig(getName(), TsfCertificateManagerConfig.class).getToken();
    }
}
