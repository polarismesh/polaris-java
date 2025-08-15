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

package com.tencent.polaris.api.utils;

import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

import static com.tencent.polaris.api.exception.ErrorCode.INTERNAL_ERROR;

/**
 * Utils for cert.
 *
 * @author Haotian Zhang
 */
public final class CertUtils {

    private static final Logger log = LoggerFactory.getLogger(CertUtils.class);

    private CertUtils() {

    }

    /**
     * 打印X509Certificate的详细信息
     *
     * @param certificate 需要打印的证书对象
     */
    public static void printCertificateInfo(X509Certificate certificate) {
        if (certificate == null) {
            log.warn("Certificate is null");
            return;
        }
        log.info("Certificate [Subject: {}, Issuer: {}, Serial Number: {}, Valid From {} to {}, Signature Algorithm: {}, Public Key Algorithm: {}]",
                certificate.getSubjectX500Principal(),
                certificate.getIssuerX500Principal(),
                certificate.getSerialNumber(),
                certificate.getNotBefore(),
                certificate.getNotAfter(),
                certificate.getSigAlgName(),
                certificate.getPublicKey().getAlgorithm()
        );
    }

    /**
     * 判断X509Certificate是否是根证书
     *
     * @param certificate 需要判断的证书对象
     * @return 如果是根证书返回true，否则返回false
     */
    public static boolean isRootCertificate(X509Certificate certificate) {
        if (certificate == null) {
            log.warn("Certificate is null");
            return false;
        }
        return certificate.getIssuerX500Principal().equals(certificate.getSubjectX500Principal());
    }

    public static X509Certificate convertToX509Certificate(String pemCertificateStr) {
        try (InputStream in = new ByteArrayInputStream(pemCertificateStr.getBytes(StandardCharsets.UTF_8))) {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) factory.generateCertificate(in);
        } catch (CertificateException | IOException e) {
            log.error("Failed to decode certificate", e);
            throw new PolarisException(INTERNAL_ERROR, "Failed to decode certificate", e);
        }
    }

    public static String convertPemToString(Object pem) {
        if (!(pem instanceof X509Certificate) && !(pem instanceof PrivateKey) && !(pem instanceof PublicKey)) {
            throw new IllegalArgumentException("Pem object must be X509Certificate, PrivateKey or PublicKey");
        }

        try {
            StringWriter writer = new StringWriter();
            JcaPEMWriter pemWriter = new JcaPEMWriter(writer);
            pemWriter.writeObject(pem);
            pemWriter.close();

            return writer.toString();
        } catch (IOException e) {
            log.error("Failed to encode pem", e);
            throw new PolarisException(ErrorCode.INTERNAL_ERROR, "Failed to encode pem", e);
        }
    }

    public static String convertCSRToString(PKCS10CertificationRequest csr) {
        try {
            String pemCSR = "-----BEGIN CERTIFICATE REQUEST-----\n" + Base64.getEncoder().encodeToString(csr.getEncoded()) + "\n-----END CERTIFICATE REQUEST-----";
            if (log.isDebugEnabled()) {
                log.debug("CSR content: {}", pemCSR);
            }
            return pemCSR;
        } catch (IOException e) {
            log.error("get CSR byte array failed.", e);
            throw new PolarisException(ErrorCode.INTERNAL_ERROR, "get CSR byte array failed.", e);
        }
    }

    public static byte[] readCertificateFileBytes(String filePath) {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new PolarisException(ErrorCode.INTERNAL_ERROR, "Invalid Grpc tls certificate path: " + path);
        }
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new PolarisException(ErrorCode.INTERNAL_ERROR, "Error reading certificate file: " + path, e);
        }
    }

    public static X509TrustManager buildTrustManager(String filePath) throws SSLException {
        return CertUtils.buildTrustManager(CertUtils.readCertificateFileBytes(filePath));
    }

    /**
     * Build new X.509 trust manager.
     *
     * @param trustedCertificates trusted certificates
     * @return X509TrustManager
     * @throws SSLException SSLException
     */
    public static X509TrustManager buildTrustManager(byte[] trustedCertificates) throws SSLException {
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null);

            try (ByteArrayInputStream is = new ByteArrayInputStream(trustedCertificates)) {
                CertificateFactory factory = CertificateFactory.getInstance("X.509");
                int i = 0;
                while (is.available() > 0) {
                    X509Certificate x509Certificate = (X509Certificate) factory.generateCertificate(is);
                    keyStore.setCertificateEntry("grpc_cert_" + i, x509Certificate);
                    i++;
                }
            }

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            return (X509TrustManager) trustManagerFactory.getTrustManagers()[0];
        } catch (CertificateException | KeyStoreException | IOException | NoSuchAlgorithmException e) {
            throw new SSLException("Build X.509 trust manager error", e);
        }
    }
}
