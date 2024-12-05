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

package com.tencent.polaris.encrypt.util;

import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.utils.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;

/**
 * @author fabian4, Haotian Zhang
 */
public class AESUtil {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * 生成AES128密钥
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
     * 生成AES256密钥
     *
     * @param seed 随机数种子
     */
    public static byte[] generateAesKey(String seed) {
        KeyGenerator keyGenerator;
        try {
            keyGenerator = KeyGenerator.getInstance("AES");
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
            secureRandom.setSeed(SHA256.encode(seed));
            keyGenerator.init(256, secureRandom);
        } catch (NoSuchAlgorithmException e) {
            throw new PolarisException(ErrorCode.AES_KEY_GENERATE_ERROR, e.getMessage());
        }
        SecretKey secretKey = keyGenerator.generateKey();
        return secretKey.getEncoded();
    }

    /**
     * AES加密，AES/CBC/PKCS7Padding
     *
     * @param content  需要加密的内容
     * @param password 加密密码
     */
    public static String encrypt(String content, byte[] password) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
            byte[] iv = new byte[cipher.getBlockSize()];
            System.arraycopy(password, 0, iv, 0, cipher.getBlockSize());
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(password, "AES"), new IvParameterSpec(iv));
            byte[] bytes = cipher.doFinal(content.getBytes());
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            throw new PolarisException(ErrorCode.AES_ENCRYPT_ERROR, e.getMessage(), e);
        }
    }

    /**
     * AES加密，AES/ECB/PKCS7Padding
     *
     * @param content  明文
     * @param password 密钥
     * @return 密文
     */
    public static String encrypt(String content, String password) {
        if (StringUtils.isBlank(password)) {
            throw new PolarisException(ErrorCode.AES_ENCRYPT_ERROR, "Password not found.");
        }
        try {
            byte[] enCodeFormat = generateAesKey(password);
            // 根据给定的字节数组构造一个密钥。enCodeFormat：密钥内容；"AES"：与给定的密钥内容相关联的密钥算法的名称
            SecretKeySpec skSpec = new SecretKeySpec(enCodeFormat, "AES");
            // 创建一个实现指定转换的 Cipher对象，该转换由指定的提供程序提供。
            // "AES/ECB/PKCS7Padding"：转换的名称；"BC"：提供程序的名称
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS7Padding", "BC");
            // 初始化cipher：加密模式
            cipher.init(Cipher.ENCRYPT_MODE, skSpec);
            byte[] byteContent = content.getBytes(StandardCharsets.UTF_8);
            byte[] cryptograph = cipher.doFinal(byteContent);
            byte[] enryptedContent = org.bouncycastle.util.encoders.Base64.encode(cryptograph);
            return new String(enryptedContent);
        } catch (Exception e) {
            throw new PolarisException(ErrorCode.AES_ENCRYPT_ERROR, "Failed encrypt.", e);
        }
    }

    /**
     * AES解密，AES/CBC/PKCS7Padding
     *
     * @param content  待解密内容
     * @param password 解密密钥
     */
    public static String decrypt(String content, byte[] password) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
            byte[] iv = new byte[cipher.getBlockSize()];
            System.arraycopy(password, 0, iv, 0, cipher.getBlockSize());
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(password, "AES"), new IvParameterSpec(iv));
            byte[] paddingPlaintext = cipher.doFinal(Base64.getDecoder().decode(content));
            return new String(paddingPlaintext);
        } catch (Exception e) {
            throw new PolarisException(ErrorCode.AES_DECRYPT_ERROR, e.getMessage(), e);
        }
    }

    /**
     * AES解密，AES/ECB/PKCS7Padding
     *
     * @param encryptedContent 密文
     * @param password         密钥
     * @return 明文
     */
    public static String decrypt(String encryptedContent, String password) {
        if (StringUtils.isBlank(password)) {
            throw new PolarisException(ErrorCode.AES_DECRYPT_ERROR, "Password not found.");
        }
        try {
            byte[] enCodeFormat = generateAesKey(password);
            // 根据给定的字节数组构造一个密钥。enCodeFormat：密钥内容；"AES"：与给定的密钥内容相关联的密钥算法的名称
            SecretKeySpec skSpec = new SecretKeySpec(enCodeFormat, "AES");
            // 创建一个实现指定转换的 Cipher对象，该转换由指定的提供程序提供。
            // "AES/ECB/PKCS7Padding"：转换的名称；"BC"：提供程序的名称
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS7Padding", "BC");
            // 初始化cipher：解密模式
            cipher.init(Cipher.DECRYPT_MODE, skSpec);
            byte[] result = cipher.doFinal(org.bouncycastle.util.encoders.Base64.decode(encryptedContent.getBytes(StandardCharsets.UTF_8)));
            return new String(result);
        } catch (Exception e) {
            throw new PolarisException(ErrorCode.AES_DECRYPT_ERROR, "Failed decrypt.", e);
        }
    }

}
