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

package com.tencent.polaris.plugins.configfilter.util;

import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Arrays;
import java.util.Base64;

/**
 * @author fabian4
 * @date 2023/6/14
 */
public class AESUtil {

    /**
     * 生成AES密钥
     */
    public static byte[] generateAesKey() {
        KeyGenerator keyGenerator;
        try {
            keyGenerator = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            throw new PolarisException(ErrorCode.AES_KEY_GENERATE_ERROR, e.getMessage());
        }
        SecureRandom secureRandom = new SecureRandom();
        keyGenerator.init(128, secureRandom);
        SecretKey secretKey = keyGenerator.generateKey();
        return secretKey.getEncoded();
    }

    /**
     * AES加密
     *
     * @param content  需要加密的内容
     * @param password 加密密码
     */
    public static String encrypt(String content, byte[] password) {
        SecretKeySpec key = new SecretKeySpec(password, "AES");
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] iv = new byte[cipher.getBlockSize()];
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, key, ivParameterSpec);
            byte[] paddingPlaintext = pkcs7Padding(content.getBytes(), cipher.getBlockSize());
            byte[] bytes = cipher.doFinal(paddingPlaintext);
            return Base64.getEncoder().encodeToString(bytes);
        } catch (IllegalBlockSizeException | BadPaddingException | NoSuchPaddingException | NoSuchAlgorithmException |
                 InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new PolarisException(ErrorCode.AES_ENCRYPT_ERROR, e.getMessage());
        }
    }

    /**
     * AES解密
     *
     * @param content  待解密内容
     * @param password 解密密钥
     */
    public static String decrypt(String content, byte[] password) {
        SecretKeySpec key = new SecretKeySpec(password, "AES");
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] iv = new byte[cipher.getBlockSize()];
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, key, ivParameterSpec);
            byte[] paddingPlaintext = cipher.doFinal(Base64.getDecoder().decode(content));
            return Arrays.toString(pkcs7UnPadding(paddingPlaintext));
        } catch (IllegalBlockSizeException | BadPaddingException | NoSuchPaddingException | NoSuchAlgorithmException |
                 InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new PolarisException(ErrorCode.AES_DECRYPT_ERROR, e.getMessage());
        }
    }

    private static byte[] pkcs7Padding(byte[] data, int blockSize) {
        int paddingLength = blockSize - (data.length % blockSize);
        byte[] paddingData = new byte[data.length + paddingLength];
        System.arraycopy(data, 0, paddingData, 0, data.length);
        for (int i = data.length; i < paddingData.length; i++) {
            paddingData[i] = (byte) paddingLength;
        }
        return paddingData;
    }

    private static byte[] pkcs7UnPadding(byte[] data) {
        int paddingLength = data[data.length - 1];
        return paddingLength == 0 ? data : subBytes(data, 0, data.length - paddingLength);
    }

    private static byte[] subBytes(byte[] data, int offset, int length) {
        byte[] result = new byte[length];
        System.arraycopy(data, offset, result, 0, length);
        return result;
    }

    public static void main(String[] args) throws Exception {

        String dataKey = "UAKx/VOOOHPunAOCTxsh4A==";
        String text = "XMzmtqRuLRr3naTKWbuJXA==";

        Security.addProvider(new BouncyCastleProvider());
        SecretKeySpec key = new SecretKeySpec(Base64.getDecoder().decode(dataKey), "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        byte[] iv = new byte[cipher.getBlockSize()];
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, key, ivParameterSpec);
        byte[] paddingPlaintext = cipher.doFinal(Base64.getDecoder().decode(text));
        System.out.println(Arrays.toString(pkcs7UnPadding(paddingPlaintext)));
    }


}
