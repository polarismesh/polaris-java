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

package com.tencent.polaris.certificate.client.utils;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.utils.CertUtils;
import com.tencent.polaris.api.utils.StringUtils;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.pkcs.RSAPrivateKey;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Haotian Zhang
 */
public class FileUtils {

    private static final Logger log = LoggerFactory.getLogger(FileUtils.class);

    public static final AtomicBoolean isDirInitialized = new AtomicBoolean(false);

    private static String dirPath;

    public static String getDirPath() {
        return dirPath;
    }

    public static boolean initDir() {
        isDirInitialized.compareAndSet(false, true);
        dirPath = System.getProperty("polaris.tls.home");
        if (StringUtils.isBlank(dirPath)) {
            String userHome = System.getProperty("user.dir");
            dirPath = userHome + File.separator + "polaris" + File.separator + "tls";
        }
        log.info("Certificate directory path {} begin init.", dirPath);
        try {
            File persistDirFile = new File(dirPath);
            if (!persistDirFile.exists() && !persistDirFile.mkdirs()) {
                log.warn("fail to create dir {}", dirPath);
            }
            //检查文件夹是否具备写权限
            if (!Files.isWritable(FileSystems.getDefault().getPath(dirPath))) {
                log.warn("fail to check permission for dir {}", dirPath);
            }
            log.info("Directory write permission verified: {}", FileUtils.getDirPath());
            return true;
        } catch (Throwable e) {
            log.warn("fail to check permission for dir {}", dirPath, e);
        }
        return false;
    }

    /**
     * 保存PEM到文件
     *
     * @param filePath 目标文件路径
     * @param pem      PEM对象
     * @return 是否保存成功
     */
    public static boolean savePemToFile(String filePath, Object pem) {
        if (StringUtils.isBlank(filePath) || pem == null) {
            throw new IllegalArgumentException("Pem object and file path cannot be null or empty");
        }

        try {
            String pemContent = CertUtils.convertPemToString(pem);
            return saveFile(filePath, pemContent.getBytes()) != null;
        } catch (PolarisException e) {
            log.error("Failed to save pem {} to file path {}", pem, filePath, e);
            return false;
        }
    }

    public static Path saveFile(String fileName, byte[] content) {
        String lockFileName = fileName + ".lock";
        Path persistPath = FileSystems.getDefault().getPath(fileName);
        File persistFile = new File(fileName);
        File persistLockFile = new File(lockFileName);
        try {
            if (!persistLockFile.exists()) {
                if (!persistLockFile.createNewFile()) {
                    log.warn("lock file {} already exists", persistLockFile.getAbsolutePath());
                }
            }
            try (RandomAccessFile raf = new RandomAccessFile(persistLockFile, "rw");
                 FileChannel channel = raf.getChannel()) {
                FileLock lock = channel.tryLock();
                if (lock == null) {
                    throw new IOException(
                            "fail to lock file " + persistFile.getAbsolutePath() + ", ignore and retry later");
                }
                //执行保存
                try (FileOutputStream outputFile = new FileOutputStream(persistFile)) {
                    outputFile.write(content);
                    outputFile.flush();
                    if (log.isDebugEnabled()) {
                        log.debug("write file {} with content: {} finished.", persistFile.getAbsolutePath(), content);
                    }
                } finally {
                    lock.release();
                }
            }
        } catch (IOException e) {
            log.error("fail to write file {}", persistFile, e);
            return null;
        }
        return persistPath.toAbsolutePath();
    }

    public static PrivateKey readPrivateKeyFromFile(String filePath) {
        if (!isFileExists(filePath)) {
            log.info("private key file {} not exists", filePath);
            return null;
        }
        try {
            // 方法1：使用BouncyCastle的PEM解析器
            PrivateKey key = readPrivateKeyWithBouncyCastle(filePath);
            if (key != null) {
                return key;
            }

            // 方法2：手动解析PEM格式
            key = readPrivateKeyManually(filePath);
            if (key != null) {
                return key;
            }

            // 方法3：尝试DER格式
            key = readPrivateKeyDER(filePath);
            if (key != null) {
                return key;
            }
        } catch (Throwable throwable) {
            log.error("fail to read private key from file {}", filePath, throwable);
            return null;
        }
        return null;
    }

    /**
     * 使用BouncyCastle解析PEM格式私钥
     */
    private static PrivateKey readPrivateKeyWithBouncyCastle(String filePath) {
        try (PEMParser pemParser = new PEMParser(new FileReader(filePath))) {
            Object object = pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();

            if (object instanceof PEMKeyPair) {
                PEMKeyPair keyPair = (PEMKeyPair) object;
                return converter.getPrivateKey(keyPair.getPrivateKeyInfo());
            } else if (object instanceof PrivateKeyInfo) {
                PrivateKeyInfo privateKeyInfo = (PrivateKeyInfo) object;
                return converter.getPrivateKey(privateKeyInfo);
            }
        } catch (Exception e) {
            log.debug("BouncyCastle解析失败，尝试其他方法: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 手动解析PEM格式
     */
    private static PrivateKey readPrivateKeyManually(String filePath) throws Exception {
        String content = new String(Files.readAllBytes(Paths.get(filePath)));

        // 移除PEM头和尾
        String privateKeyPEM = content
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replace("-----BEGIN ENCRYPTED PRIVATE KEY-----", "")
                .replace("-----END ENCRYPTED PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);

        try {
            // 尝试PKCS8格式
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            log.debug("PKCS8格式解析失败，尝试PKCS1格式");

            // 尝试PKCS1格式
            return readPKCS1PrivateKey(encoded);
        }
    }

    /**
     * 解析PKCS1格式的RSA私钥
     */
    private static PrivateKey readPKCS1PrivateKey(byte[] encoded) throws Exception {
        ASN1Sequence sequence = ASN1Sequence.getInstance(encoded);
        RSAPrivateKey rsaPrivateKey = RSAPrivateKey.getInstance(sequence);

        RSAPrivateCrtKeySpec keySpec = new RSAPrivateCrtKeySpec(
                rsaPrivateKey.getModulus(),
                rsaPrivateKey.getPublicExponent(),
                rsaPrivateKey.getPrivateExponent(),
                rsaPrivateKey.getPrime1(),
                rsaPrivateKey.getPrime2(),
                rsaPrivateKey.getExponent1(),
                rsaPrivateKey.getExponent2(),
                rsaPrivateKey.getCoefficient()
        );

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    /**
     * 直接读取DER格式
     */
    private static PrivateKey readPrivateKeyDER(String filePath) throws Exception {
        byte[] keyBytes = Files.readAllBytes(Paths.get(filePath));
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    public static PublicKey readPublicKeyFromFile(String filePath) {
        if (!isFileExists(filePath)) {
            log.info("public key file {} not exists", filePath);
            return null;
        }
        try {
            // 读取文件内容
            byte[] keyBytes = Files.readAllBytes(Paths.get(filePath));

            // 处理PEM格式的公钥（如果文件是PEM格式）
            String keyContent = new String(keyBytes);
            if (keyContent.contains("-----BEGIN PUBLIC KEY-----")) {
                keyContent = keyContent
                        .replace("-----BEGIN PUBLIC KEY-----", "")
                        .replace("-----END PUBLIC KEY-----", "")
                        .replaceAll("\\s", "");
                keyBytes = Base64.getDecoder().decode(keyContent);
            }

            // 生成PublicKey
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(keySpec);
        } catch (Throwable throwable) {
            log.error("fail to read public key from file {}", filePath, throwable);
            return null;
        }
    }

    public static boolean isFileExists(String filePath) {
        try {
            Path path = Paths.get(filePath);
            return Files.exists(path) && !Files.isDirectory(path);
        } catch (Throwable e) {
            log.warn("fail to check file {}", filePath, e);
            return false;
        }
    }
}
