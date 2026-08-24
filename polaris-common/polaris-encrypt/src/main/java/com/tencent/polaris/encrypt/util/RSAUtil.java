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

import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.pkcs.RSAPublicKey;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

/**
 * @author fabian4, Haotian Zhang
 */
public class RSAUtil {

    /**
     * 生成RSA密钥对
     */
    public static KeyPair generateRsaKeyPair() {
        KeyPairGenerator keyPairGenerator;
        try {
            keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new PolarisException(ErrorCode.RSA_KEY_GENERATE_ERROR, e.getMessage());
        }
        keyPairGenerator.initialize(1024);
        return keyPairGenerator.generateKeyPair();
    }

    /**
     * RSA加密
     *
     * @param content   需要加密的内容
     * @param publicKey 公钥
     */
    public static byte[] encrypt(byte[] content, PublicKey publicKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return cipher.doFinal(content);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException |
                 BadPaddingException e) {
            throw new PolarisException(ErrorCode.RSA_ENCRYPT_ERROR, e.getMessage());
        }
    }

    /**
     * RSA解密
     *
     * @param content    待解密内容
     * @param privateKey 私钥
     */
    public static byte[] decrypt(byte[] content, PrivateKey privateKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return cipher.doFinal(content);
        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | IllegalBlockSizeException |
                 BadPaddingException e) {
            throw new PolarisException(ErrorCode.RSA_DECRYPT_ERROR, e.getMessage());
        }
    }

    /**
     * Encode the RSA public key as PKCS1 DER then Base64.
     *
     * @param publicKey RSA public key
     * @return PKCS1 public key in Base64
     */
    public static String toPkcs1PublicKeyBase64(PublicKey publicKey) {
        String encoded;
        try {
            SubjectPublicKeyInfo spkInfo = SubjectPublicKeyInfo.getInstance(publicKey.getEncoded());
            ASN1Primitive primitive = spkInfo.parsePublicKey();
            encoded = Base64.getEncoder().encodeToString(primitive.getEncoded());
        } catch (IOException | RuntimeException e) {
            throw new PolarisException(ErrorCode.RSA_KEY_GENERATE_ERROR, e.getMessage());
        }
        return encoded;
    }

    /**
     * Parse a PKCS1 DER Base64 RSA public key.
     *
     * @param pkcs1PublicKey PKCS1 public key in Base64
     * @return RSA public key
     */
    public static PublicKey parsePkcs1PublicKey(String pkcs1PublicKey) {
        PublicKey publicKey;
        try {
            byte[] decoded = Base64.getDecoder().decode(pkcs1PublicKey);
            RSAPublicKey rsaPublicKey = RSAPublicKey.getInstance(decoded);
            RSAPublicKeySpec keySpec = new RSAPublicKeySpec(rsaPublicKey.getModulus(),
                    rsaPublicKey.getPublicExponent());
            publicKey = KeyFactory.getInstance("RSA").generatePublic(keySpec);
        } catch (RuntimeException | InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new PolarisException(ErrorCode.RSA_ENCRYPT_ERROR, e.getMessage());
        }
        return publicKey;
    }

    /**
     * Encrypt plaintext with a PKCS1 public key and return Base64 ciphertext.
     *
     * @param content plaintext
     * @param pkcs1PublicKey PKCS1 public key in Base64
     * @return RSA ciphertext in Base64
     */
    public static String encryptToBase64(byte[] content, String pkcs1PublicKey) {
        PublicKey publicKey = parsePkcs1PublicKey(pkcs1PublicKey);
        byte[] encrypted = encrypt(content, publicKey);
        return Base64.getEncoder().encodeToString(encrypted);
    }
}
