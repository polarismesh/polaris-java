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

package com.tencent.polaris.certificate.client.core;

import com.tencent.polaris.certificate.api.core.CertificateAPI;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link DefaultCertificateAPI}.
 *
 * @author Haotian Zhang
 */
public class DefaultCertificateAPITest {

    @Test
    public void testImplementsCertificateAPI() {
        // 验证：DefaultCertificateAPI实现了CertificateAPI接口
        assertThat(CertificateAPI.class.isAssignableFrom(DefaultCertificateAPI.class)).isTrue();
    }
}
