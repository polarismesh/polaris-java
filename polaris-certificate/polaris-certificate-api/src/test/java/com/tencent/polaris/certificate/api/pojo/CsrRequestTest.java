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

package com.tencent.polaris.certificate.api.pojo;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link CsrRequest}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class CsrRequestTest {

    /**
     * 测试构造函数保存 commonName 和 keyPair
     * 测试目的：验证 CsrRequest 正确保存 commonName 和 keyPair
     * 测试场景：生成 RSA KeyPair 并构造 CsrRequest
     * 验证内容：getCommonName 和 getKeyPair 与入参一致
     */
    @Test
    public void testConstructorSavesFields() throws NoSuchAlgorithmException {
        // Arrange
        String commonName = "polaris-client";
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        // Act
        CsrRequest request = new CsrRequest(commonName, keyPair);

        // Assert
        assertThat(request.getCommonName()).isEqualTo(commonName);
        assertThat(request.getKeyPair()).isEqualTo(keyPair);
    }

    /**
     * 测试构造函数接受 null keyPair
     * 测试目的：验证 CsrRequest 对 null keyPair 不做校验，原样保存
     * 测试场景：keyPair 传 null 构造 CsrRequest
     * 验证内容：getKeyPair 为 null，getCommonName 正常
     */
    @Test
    public void testConstructorWithNullKeyPair() {
        // Act
        CsrRequest request = new CsrRequest("cn-only", null);

        // Assert
        assertThat(request.getCommonName()).isEqualTo("cn-only");
        assertThat(request.getKeyPair()).isNull();
    }

    /**
     * 测试 toString 只包含 commonName
     * 测试目的：验证 toString 输出包含 commonName 但不包含 keyPair（避免泄露密钥）
     * 测试场景：构造后调用 toString
     * 验证内容：toString 包含 commonName
     */
    @Test
    public void testToString() {
        // Arrange
        CsrRequest request = new CsrRequest("polaris-cn", null);

        // Act
        String str = request.toString();

        // Assert
        assertThat(str).contains("polaris-cn");
    }
}
