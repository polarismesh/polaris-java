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

package com.tencent.polaris.encrypt.util;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author fabian4
 * @date 2023/6/14
 */
public class RSAUtilTest {

    @Test
    public void testRsa() {
        KeyPair keyPair = RSAUtil.generateRsaKeyPair();
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();
        byte[] content = "test content".getBytes();
        byte[] encrypted = RSAUtil.encrypt(content, publicKey);
        byte[] decrypted = RSAUtil.decrypt(encrypted, privateKey);
        assertArrayEquals(content, decrypted);
    }

    /**
     * 测试目的：PKCS1 公钥编解码往返后仍能 RSA 加解密。
     * 测试场景：公钥按 PKCS1 DER+base64 编码后再解析。
     * 验证内容：密文可用原私钥还原明文。
     */
    @Test
    public void testEncryptToBase64WithPkcs1PublicKey() {
        KeyPair keyPair = RSAUtil.generateRsaKeyPair();
        byte[] content = "P123456789012345".getBytes();
        String pkcs1PublicKey = RSAUtil.toPkcs1PublicKeyBase64(keyPair.getPublic());
        String cipherText = RSAUtil.encryptToBase64(content, pkcs1PublicKey);
        byte[] decrypted = RSAUtil.decrypt(Base64.getDecoder().decode(cipherText), keyPair.getPrivate());
        assertArrayEquals(content, decrypted);
    }

    /**
     * 测试目的：WatchClientEvents 下发的 Base64(PEM X.509) 公钥可加密。
     * 测试场景：SPKI PEM 再包一层 Base64，行尾使用 CRLF。
     * 验证内容：密文可用原私钥还原明文。
     */
    @Test
    public void testEncryptToBase64WithPemPublicKeyWrappedInBase64() {
        KeyPair keyPair = RSAUtil.generateRsaKeyPair();
        byte[] content = "P123456789012345".getBytes();
        String body = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        String pem = "-----BEGIN PUBLIC KEY-----\r\n" + body + "\r\n-----END PUBLIC KEY-----\r\n";
        String wrapped = Base64.getEncoder().encodeToString(pem.getBytes(StandardCharsets.US_ASCII));
        String cipherText = RSAUtil.encryptToBase64(content, wrapped);
        byte[] decrypted = RSAUtil.decrypt(Base64.getDecoder().decode(cipherText), keyPair.getPrivate());
        assertArrayEquals(content, decrypted);
    }

    /**
     * 测试目的：真实 PUSH 中的 Base64(PEM) 公钥可解析（unknown tag 13 回归）。
     * 测试场景：服务端 maintain 下发的 public_key 样本。
     * 验证内容：parseRsaPublicKey 不抛异常。
     */
    @Test
    public void testParseRsaPublicKeyFromWatchClientEventsSample() {
        String publicKey = "LS0tLS1CRUdJTiBQVUJMSUMgS0VZLS0tLS0KTUlJQklqQU5CZ2txaGtpRzl3MEJBUUVGQUFPQ0FROEFNSUlCQ2dLQ0FRRUFzUXVDdEw1bWEzdXI0K3lnVWtFUApraUc0UnZaRVhkVldwNFg0blNmOG9lcmgvY0RZSjlXRVNLMThpRm1INGpNSmI5bGovWi9ua0s5Q2ZWTGV2T2lZCnp0eUdaSmtLQnhGQStSNjR3TUJFWFdzRTJCb2cyR0xodmdHTlBzUmF6emlqRUNhbC8vTTdaRCtDc1pPM01YY1cKenNsY2pSMjlpMFZQbmZMMWlGQWI3b3hJT1p0b0RjMTVvZklwZDN1VTlMSklicVM5KzEwWnAwTk1YRkRWYzdvegpCUU5pVnI3RGxjc0JxR0JNMnBLQmdVeWk0MERISEp1djJuWUhNdEFqb2R0OVdFelpKQmNidVVnVkVza3V4WGxUClJyRXhJRUVwK3l4T3ppbDhtS2gwbzNZejFkKzFPUlpDZlhOMHF2TEpsOFFFNDMrSmx6Smduc09UVlN5N1RjUkkKQXdJREFRQUIKLS0tLS1FTkQgUFVCTElDIEtFWS0tLS0tCg==";
        assertNotNull(RSAUtil.parseRsaPublicKey(publicKey));
    }
}
