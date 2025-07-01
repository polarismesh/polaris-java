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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import org.apache.commons.codec.binary.Base64;

/**
 * X.509 manager utils
 *
 * @author wallezhang
 */
public final class X509ManagerUtil {

    private static final String PKCS8_PRIVATE_KEY_BEGIN = "-----BEGIN PRIVATE KEY-----";
    private static final String PKCS8_PRIVATE_KEY_END = "-----END PRIVATE KEY-----";

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

    /**
     * Build new X.509 key manager
     *
     * @return X509KeyManager
     * @throws SSLException SSLException
     */
    public static X509KeyManager buildKeyManager(byte[] clientCertificates, byte[] clientKeyChain) throws SSLException {
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null);

            KeyFactory factory = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(validPrivateKey(clientKeyChain));
            PrivateKey key = factory.generatePrivate(keySpec);

            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            List<Certificate> keyChains = new ArrayList<>();
            try (ByteArrayInputStream is = new ByteArrayInputStream(clientCertificates)) {
                while (is.available() > 0) {
                    keyChains.add(certificateFactory.generateCertificate(is));
                }
            }
            keyStore.setKeyEntry("grpc_client_trusted", key, "".toCharArray(), keyChains.toArray(new Certificate[]{}));

            KeyManagerFactory managerFactory = KeyManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            managerFactory.init(keyStore, "".toCharArray());
            return (X509KeyManager) managerFactory.getKeyManagers()[0];
        } catch (UnrecoverableKeyException | CertificateException | KeyStoreException | IOException |
                NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new SSLException("Build X.509 key manager error", e);
        }
    }

    private static byte[] validPrivateKey(byte[] clientKeyChain) {
        String clientKey = new String(clientKeyChain);
        // Standard pkcs8 rsa private key format, should be decoded before using
        if (clientKey.contains(PKCS8_PRIVATE_KEY_BEGIN) || clientKey.contains(PKCS8_PRIVATE_KEY_END)) {
            clientKey = clientKey.replace(PKCS8_PRIVATE_KEY_BEGIN, "");
            clientKey = clientKey.replace(PKCS8_PRIVATE_KEY_END, "");
            clientKey = clientKey.trim().replaceAll(System.lineSeparator(), "");
            return Base64.decodeBase64(clientKey);
        }
        return clientKeyChain;
    }
}
