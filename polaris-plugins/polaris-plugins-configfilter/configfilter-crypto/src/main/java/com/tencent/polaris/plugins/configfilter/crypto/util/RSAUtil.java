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

package com.tencent.polaris.plugins.configfilter.crypto.util;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * @author fabian4
 * @date 2023/6/14
 */
public class RSAUtil {

//RSAUtil    /**
//     * 生成RSA密钥对
//     */
//    public static KeyPair generateRsaKeyPair() {
//        KeyPairGenerator keyPairGenerator;
//        try {
//            keyPairGenerator = KeyPairGenerator.getInstance("RSA");
//        } catch (NoSuchAlgorithmException e) {
//            throw new PolarisException(ErrorCode.RSA_KEY_GENERATE_ERROR, e.getMessage());
//        }
//        keyPairGenerator.initialize(1024);
//        return keyPairGenerator.generateKeyPair();
//    }
//
//    /**
//     * RSA加密
//     *
//     * @param content   需要加密的内容
//     * @param publicKey 公钥
//     */
//    public static byte[] encrypt(byte[] content, PublicKey publicKey) {
//        try {
//            Cipher cipher = Cipher.getInstance("RSA");
//            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
//            return cipher.doFinal(content);
//        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException |
//                 BadPaddingException e) {
//            throw new PolarisException(ErrorCode.RSA_ENCRYPT_ERROR, e.getMessage());
//        }
//    }
//
//    /**
//     * RSA解密
//     *
//     * @param content    待解密内容
//     * @param privateKey 私钥
//     */
//    public static byte[] decrypt(byte[] content, PrivateKey privateKey) {
//        try {
//            Cipher cipher = Cipher.getInstance("RSA");
//            cipher.init(Cipher.DECRYPT_MODE, privateKey);
//            return cipher.doFinal(content);
//        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | IllegalBlockSizeException |
//                 BadPaddingException e) {
//            throw new PolarisException(ErrorCode.RSA_DECRYPT_ERROR, e.getMessage());
//        }
//    }

    public static void main(String[] args) throws Exception {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
        keyPairGen.initialize(1024, new SecureRandom());
        KeyPair keyPair = keyPairGen.generateKeyPair();

        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate(); // 得到私钥

        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic(); // 得到公钥

    }
}
