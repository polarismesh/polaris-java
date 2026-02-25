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

package com.tencent.polaris.certificate.client.flow;

import com.tencent.polaris.api.plugin.certificate.CertFileKey;
import com.tencent.polaris.certificate.api.flow.CertificateFlow;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link DefaultCertificateFlow}.
 *
 * @author Haotian Zhang
 */
public class DefaultCertificateFlowTest {

    @Test
    public void testGetName() {
        // 准备：创建DefaultCertificateFlow实例
        DefaultCertificateFlow flow = new DefaultCertificateFlow();

        // 执行 & 验证：获取默认流程名称
        assertThat(flow.getName()).isEqualTo("default");
    }

    @Test
    public void testGetPemFileMap_InitiallyEmpty() {
        // 准备：创建DefaultCertificateFlow实例
        DefaultCertificateFlow flow = new DefaultCertificateFlow();

        // 执行 & 验证：初始pemFileMap为空
        assertThat(flow.getPemFileMap()).isNotNull();
        assertThat(flow.getPemFileMap()).isEmpty();
    }

    @Test
    public void testDestroy_WithoutScheduler() {
        // 准备：创建DefaultCertificateFlow实例（无scheduler）
        DefaultCertificateFlow flow = new DefaultCertificateFlow();

        // 执行 & 验证：destroy不抛出异常
        flow.destroy();
    }

    @Test
    public void testImplementsCertificateFlow() {
        // 准备：创建DefaultCertificateFlow实例
        DefaultCertificateFlow flow = new DefaultCertificateFlow();

        // 执行 & 验证：是CertificateFlow的实例
        assertThat(flow).isInstanceOf(CertificateFlow.class);
    }

    @Test
    public void testGetPemFileMap_ReturnsSameInstance() {
        // 准备：创建DefaultCertificateFlow实例
        DefaultCertificateFlow flow = new DefaultCertificateFlow();

        // 执行 & 验证：多次调用返回同一个Map实例
        assertThat(flow.getPemFileMap()).isSameAs(flow.getPemFileMap());
    }
}
