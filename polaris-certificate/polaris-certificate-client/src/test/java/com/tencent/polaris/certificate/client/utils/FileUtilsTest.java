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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link FileUtils}.
 *
 * @author Haotian Zhang
 */
public class FileUtilsTest {

    private Path tempDir;

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("polaris-cert-test");
        System.setProperty("polaris.tls.home", tempDir.toString());
        // 重置初始化状态
        FileUtils.isDirInitialized.set(false);
    }

    @After
    public void tearDown() throws IOException {
        // 清理临时文件
        File dir = tempDir.toFile();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        dir.delete();
        System.clearProperty("polaris.tls.home");
        FileUtils.isDirInitialized.set(false);
    }

    @Test
    public void testInitDir_Success() {
        // 执行
        boolean result = FileUtils.initDir();

        // 验证：初始化成功
        assertThat(result).isTrue();
        assertThat(FileUtils.getDirPath()).isEqualTo(tempDir.toString());
    }

    @Test
    public void testIsFileExists_WithExistingFile() throws IOException {
        // 准备：创建一个临时文件
        File tempFile = new File(tempDir.toFile(), "test.txt");
        tempFile.createNewFile();

        // 执行 & 验证
        assertThat(FileUtils.isFileExists(tempFile.getAbsolutePath())).isTrue();
    }

    @Test
    public void testIsFileExists_WithNonExistingFile() {
        // 准备：不存在的文件路径
        String nonExistingPath = tempDir.toString() + File.separator + "non-existing.txt";

        // 执行 & 验证
        assertThat(FileUtils.isFileExists(nonExistingPath)).isFalse();
    }

    @Test
    public void testIsFileExists_WithDirectory() {
        // 准备：传入目录路径

        // 执行 & 验证：目录不被视为文件
        assertThat(FileUtils.isFileExists(tempDir.toString())).isFalse();
    }

    @Test
    public void testSaveFile_Success() {
        // 准备：文件名和内容
        String fileName = tempDir.toString() + File.separator + "save-test.txt";
        byte[] content = "test content".getBytes();

        // 执行
        Path result = FileUtils.saveFile(fileName, content);

        // 验证：文件保存成功
        assertThat(result).isNotNull();
        assertThat(FileUtils.isFileExists(fileName)).isTrue();
    }

    @Test
    public void testSavePemToFile_WithNullPath() {
        // 准备：null路径

        // 执行 & 验证：抛出异常
        assertThatThrownBy(() -> FileUtils.savePemToFile(null, new Object()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
    }

    @Test
    public void testSavePemToFile_WithEmptyPath() {
        // 准备：空字符串路径

        // 执行 & 验证：抛出异常
        assertThatThrownBy(() -> FileUtils.savePemToFile("", new Object()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
    }

    @Test
    public void testSavePemToFile_WithNullPem() {
        // 准备：null PEM对象

        // 执行 & 验证：抛出异常
        assertThatThrownBy(() -> FileUtils.savePemToFile("/tmp/test.pem", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
    }

    @Test
    public void testReadPrivateKeyFromFile_WithNonExistingFile() {
        // 准备：不存在的私钥文件路径
        String nonExistingPath = tempDir.toString() + File.separator + "non-existing.key";

        // 执行
        PrivateKey result = FileUtils.readPrivateKeyFromFile(nonExistingPath);

        // 验证：返回null
        assertThat(result).isNull();
    }

    @Test
    public void testReadPublicKeyFromFile_WithNonExistingFile() {
        // 准备：不存在的公钥文件路径
        String nonExistingPath = tempDir.toString() + File.separator + "non-existing.key";

        // 执行
        PublicKey result = FileUtils.readPublicKeyFromFile(nonExistingPath);

        // 验证：返回null
        assertThat(result).isNull();
    }

    @Test
    public void testSavePemToFile_WithPrivateKey() throws NoSuchAlgorithmException {
        // 准备：生成RSA密钥对
        FileUtils.initDir();
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();
        String filePath = tempDir.toString() + File.separator + "private.key";

        // 执行
        boolean result = FileUtils.savePemToFile(filePath, keyPair.getPrivate());

        // 验证：保存成功
        assertThat(result).isTrue();
        assertThat(FileUtils.isFileExists(filePath)).isTrue();
    }

    @Test
    public void testSavePemToFile_WithPublicKey() throws NoSuchAlgorithmException {
        // 准备：生成RSA密钥对
        FileUtils.initDir();
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();
        String filePath = tempDir.toString() + File.separator + "public.key";

        // 执行
        boolean result = FileUtils.savePemToFile(filePath, keyPair.getPublic());

        // 验证：保存成功
        assertThat(result).isTrue();
        assertThat(FileUtils.isFileExists(filePath)).isTrue();
    }

    @Test
    public void testReadPrivateKeyFromFile_WithValidFile() throws NoSuchAlgorithmException {
        // 准备：生成并保存私钥
        FileUtils.initDir();
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();
        String filePath = tempDir.toString() + File.separator + "private.key";
        FileUtils.savePemToFile(filePath, keyPair.getPrivate());

        // 执行：从文件读取私钥
        PrivateKey result = FileUtils.readPrivateKeyFromFile(filePath);

        // 验证：读取成功
        assertThat(result).isNotNull();
        assertThat(result.getAlgorithm()).isEqualTo("RSA");
    }

    @Test
    public void testReadPublicKeyFromFile_WithValidFile() throws NoSuchAlgorithmException {
        // 准备：生成并保存公钥
        FileUtils.initDir();
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();
        String filePath = tempDir.toString() + File.separator + "public.key";
        FileUtils.savePemToFile(filePath, keyPair.getPublic());

        // 执行：从文件读取公钥
        PublicKey result = FileUtils.readPublicKeyFromFile(filePath);

        // 验证：读取成功
        assertThat(result).isNotNull();
        assertThat(result.getAlgorithm()).isEqualTo("RSA");
    }
}
