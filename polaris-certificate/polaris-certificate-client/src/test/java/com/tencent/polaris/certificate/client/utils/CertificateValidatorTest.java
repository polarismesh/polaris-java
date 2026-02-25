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
import com.tencent.polaris.certificate.api.pojo.CsrRequest;
import org.junit.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link CertificateValidator}.
 *
 * @author Haotian Zhang
 */
public class CertificateValidatorTest {

    @Test
    public void testValidateCsrRequest_WithValidRequest() throws NoSuchAlgorithmException {
        // 准备：创建有效的CSR请求
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();
        CsrRequest csrRequest = new CsrRequest("test-common-name", keyPair);

        // 执行 & 验证：不抛出异常
        CertificateValidator.validateCsrRequest(csrRequest);
    }

    @Test
    public void testValidateCsrRequest_WithNullRequest() {
        // 准备：null请求

        // 执行 & 验证：抛出异常
        assertThatThrownBy(() -> CertificateValidator.validateCsrRequest(null))
                .isInstanceOf(PolarisException.class)
                .hasMessageContaining("CsrRequest can not be null");
    }

    @Test
    public void testValidateCsrRequest_WithNullCommonName() throws NoSuchAlgorithmException {
        // 准备：commonName为null的请求
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();
        CsrRequest csrRequest = new CsrRequest(null, keyPair);

        // 执行 & 验证：抛出异常
        assertThatThrownBy(() -> CertificateValidator.validateCsrRequest(csrRequest))
                .isInstanceOf(PolarisException.class)
                .hasMessageContaining("commonName");
    }

    @Test
    public void testValidateCsrRequest_WithEmptyCommonName() throws NoSuchAlgorithmException {
        // 准备：commonName为空字符串的请求
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();
        CsrRequest csrRequest = new CsrRequest("", keyPair);

        // 执行 & 验证：抛出异常
        assertThatThrownBy(() -> CertificateValidator.validateCsrRequest(csrRequest))
                .isInstanceOf(PolarisException.class)
                .hasMessageContaining("commonName");
    }

    @Test
    public void testValidateCsrRequest_WithBlankCommonName() throws NoSuchAlgorithmException {
        // 准备：commonName为空白字符串的请求
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();
        CsrRequest csrRequest = new CsrRequest("   ", keyPair);

        // 执行 & 验证：抛出异常
        assertThatThrownBy(() -> CertificateValidator.validateCsrRequest(csrRequest))
                .isInstanceOf(PolarisException.class)
                .hasMessageContaining("commonName");
    }

    @Test
    public void testValidateCsrRequest_WithNullKeyPair() {
        // 准备：keyPair为null的请求
        CsrRequest csrRequest = new CsrRequest("test-common-name", null);

        // 执行 & 验证：抛出异常
        assertThatThrownBy(() -> CertificateValidator.validateCsrRequest(csrRequest))
                .isInstanceOf(PolarisException.class)
                .hasMessageContaining("keyPair");
    }
}
