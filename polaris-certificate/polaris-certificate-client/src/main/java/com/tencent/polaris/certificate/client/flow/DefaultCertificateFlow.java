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

package com.tencent.polaris.certificate.client.flow;

import com.tencent.polaris.api.config.global.CertificateConfig;
import com.tencent.polaris.api.config.global.FlowConfig;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.certificate.CertFile;
import com.tencent.polaris.api.plugin.certificate.CertFileKey;
import com.tencent.polaris.api.plugin.certificate.CertificateManager;
import com.tencent.polaris.api.utils.CertUtils;
import com.tencent.polaris.certificate.api.flow.CertificateFlow;
import com.tencent.polaris.certificate.api.pojo.CsrRequest;
import com.tencent.polaris.certificate.client.utils.CertificateValidator;
import com.tencent.polaris.certificate.client.utils.FileUtils;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.client.util.NamedThreadFactory;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DefaultCertificateFlow implements CertificateFlow {

    private static final Logger log = LoggerFactory.getLogger(DefaultCertificateFlow.class);

    private static KeyPair localKeyPair;

    private static List<X509Certificate> localCertificateList;

    private final Map<CertFileKey, CertFile> pemFileMap = new ConcurrentHashMap<>();

    private CertificateConfig certificateConfig;

    private CertificateManager certificateManager;

    private ScheduledExecutorService scheduler;

    private long expiryTime;

    @Override
    public String getName() {
        return FlowConfig.DEFAULT_FLOW_NAME;
    }

    @Override
    public void setSDKContext(SDKContext sdkContext) {
        this.certificateConfig = sdkContext.getConfig().getGlobal().getCertificate();
        this.certificateManager = sdkContext.getExtensions().getCertificateManager();
        if (certificateConfig.isEnable()) {
            initCertificate();
        }
    }

    @Override
    public Map<CertFileKey, CertFile> getPemFileMap() {
        return pemFileMap;
    }

    /**
     * 初始化证书管理
     */
    private void initCertificate() {
        if (FileUtils.initDir()) {
            Security.addProvider(new BouncyCastleProvider());
            // Initialize key pair.
            initKeyPair();

            // Initialize certificate.
            try {
                initCert();
            } catch (Throwable throwable) {
                log.error("init cert failed", throwable);
            }

            log.info("Certificate file {} init finished.", FileUtils.getDirPath());
        }
    }

    /**
     * 初始化秘钥对
     */
    private void initKeyPair() {
        localKeyPair = generateKeyPair();
    }

    /**
     * 初始化证书
     */
    private void initCert() {
        CsrRequest csrRequest = new CsrRequest(certificateConfig.getCommonName(), localKeyPair);
        CertificateValidator.validateCsrRequest(csrRequest);
        localCertificateList = signCertificateWithAutoRenew(generateCSR(csrRequest));
    }

    /**
     * 获取秘钥对
     *
     * @return
     */
    private KeyPair generateKeyPair() {
        if (localKeyPair != null) {
            return localKeyPair;
        }
        CertFile privateKeyFile = pemFileMap.computeIfAbsent(CertFileKey.PrivateKeyFile,
                key -> new CertFile(FileUtils.getDirPath() + File.separator + "private.key", CertFile.Type.KEY));
        CertFile publicKeyFile = pemFileMap.computeIfAbsent(CertFileKey.PublicKeyFile,
                key -> new CertFile(FileUtils.getDirPath() + File.separator + "public.key", CertFile.Type.KEY));

        PrivateKey privateKey = FileUtils.readPrivateKeyFromFile(pemFileMap.get(CertFileKey.PrivateKeyFile).getPath());
        PublicKey publicKey = FileUtils.readPublicKeyFromFile(pemFileMap.get(CertFileKey.PublicKeyFile).getPath());
        if (privateKey != null && publicKey != null) {
            // private key
            privateKeyFile.setOriginalObject(privateKey);
            privateKeyFile.setContent(privateKey.getEncoded());
            // public key
            publicKeyFile.setOriginalObject(publicKey);
            publicKeyFile.setContent(publicKey.getEncoded());
            return new KeyPair(publicKey, privateKey);
        }
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair keyPair = keyGen.generateKeyPair();

            // private key
            privateKey = keyPair.getPrivate();
            privateKeyFile.setOriginalObject(privateKey);
            privateKeyFile.setContent(privateKey.getEncoded());
            FileUtils.savePemToFile(privateKeyFile.getPath(), privateKey);
            // public key
            publicKey = keyPair.getPublic();
            publicKeyFile.setOriginalObject(publicKey);
            publicKeyFile.setContent(publicKey.getEncoded());
            FileUtils.savePemToFile(publicKeyFile.getPath(), publicKey);
            return keyPair;
        } catch (NoSuchAlgorithmException e) {
            throw new PolarisException(ErrorCode.NOT_SUPPORT, "No such algorithm: RSA");
        }
    }

    /**
     * 生成CSR
     *
     * @param csrRequest
     * @return
     */
    private PKCS10CertificationRequest generateCSR(CsrRequest csrRequest) {
        try {
            // 生成Subject
            X500Name subject = new X500Name("CN=" + csrRequest.getCommonName());

            // 创建CSR构造器
            JcaPKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(subject, csrRequest.getKeyPair().getPublic());

            // 创建签名器
            JcaContentSignerBuilder csBuilder = new JcaContentSignerBuilder("SHA256withRSA");
            csBuilder.setProvider("BC");
            ContentSigner signer = csBuilder.build(csrRequest.getKeyPair().getPrivate());

            return p10Builder.build(signer);
        } catch (OperatorCreationException e) {
            throw new PolarisException(ErrorCode.INTERNAL_ERROR, "generate CSR failed.", e);
        }
    }

    /**
     * 签发证书并自动续期
     *
     * @param csr
     * @return
     */
    private List<X509Certificate> signCertificateWithAutoRenew(PKCS10CertificationRequest csr) {
        List<X509Certificate> certificateList = signCertificate(csr);
        for (X509Certificate certificate : certificateList) {
            if (!CertUtils.isRootCertificate(certificate)) {
                watchCertificateExpired(csr);
            }
        }
        return certificateList;
    }

    /**
     * 签发证书
     *
     * @param csr
     * @return
     */
    private List<X509Certificate> signCertificate(PKCS10CertificationRequest csr) {
        List<X509Certificate> certificateList = Collections.emptyList();
        if (!certificateConfig.isEnable()) {
            log.warn("Certificate is not enabled, skip sign certificate.");
            return certificateList;
        }
        if (certificateManager == null) {
            log.warn("Certificate manager is null, skip sign certificate.");
            return certificateList;
        }
        String pemCSR = CertUtils.convertCSRToString(csr);
        certificateList = certificateManager.signCertificate(pemCSR, certificateConfig.getValidityDuration());
        for (X509Certificate certificate : certificateList) {
            if (certificateList.size() == 2) {
                CertFile certFile;
                if (CertUtils.isRootCertificate(certificate)) {
                    certFile = pemFileMap.computeIfAbsent(CertFileKey.PemTrustStoreCertPath,
                            certFileKey -> new CertFile(FileUtils.getDirPath() + File.separator + "truststore.crt", CertFile.Type.CERT));
                } else {
                    expiryTime = certificate.getNotAfter().getTime();
                    certFile = pemFileMap.computeIfAbsent(CertFileKey.PemKeyStoreCertPath,
                            certFileKey -> new CertFile(FileUtils.getDirPath() + File.separator + "keystore.crt", CertFile.Type.CERT));
                }
                FileUtils.savePemToFile(certFile.getPath(), certificate);
                certFile.setOriginalObject(certFile);
                try {
                    certFile.setContent(certificate.getEncoded());
                } catch (CertificateEncodingException e) {
                    log.warn("encode certificate failed.", e);
                }
            }
            if (log.isDebugEnabled()) {
                CertUtils.printCertificateInfo(certificate);
            }
        }
        return certificateList;

    }

    /**
     * 监听证书过期
     *
     * @param csr
     */
    private void watchCertificateExpired(PKCS10CertificationRequest csr) {
        if (scheduler == null) {
            NamedThreadFactory threadFactory = new NamedThreadFactory("certificate-expired-watch-thread");
            scheduler = Executors.newSingleThreadScheduledExecutor(threadFactory);
        }

        scheduler.scheduleWithFixedDelay(() -> {
            long currentTime = System.currentTimeMillis();
            if (log.isDebugEnabled()) {
                log.debug("certificate expiry time: {}, current time: {}, time left: {}", expiryTime, currentTime, expiryTime - currentTime);
            }
            if (expiryTime - currentTime <= certificateConfig.getRefreshBefore()) {
                // 如果即将过期，立即触发回调
                try {
                    log.info("certificate is about to expire, renew certificate. expiry time: {}, current time: {}", expiryTime, currentTime);
                    signCertificate(csr);
                } catch (Throwable throwable) {
                    log.error("failed to renew certificate.", throwable);
                }
            }
        }, certificateConfig.getWatchInterval(), certificateConfig.getWatchInterval(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void destroy() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }
}
