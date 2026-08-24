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

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

import static org.junit.Assert.assertArrayEquals;

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
}
