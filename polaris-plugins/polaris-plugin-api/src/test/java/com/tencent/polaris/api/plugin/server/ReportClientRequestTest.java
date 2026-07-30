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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.tencent.polaris.api.plugin.server;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link ReportClientRequest} config fields.
 *
 * @author polaris
 */
public class ReportClientRequestTest {

    private ReportClientRequest request;

    @Before
    public void setUp() {
        request = new ReportClientRequest();
    }

    @Test
    public void testConfigEnabledDefaultNull() {
        assertThat(request.getConfigEnabled()).isNull();
    }

    @Test
    public void testSetConfigEnabled() {
        request.setConfigEnabled(Boolean.TRUE);
        assertThat(request.getConfigEnabled()).isTrue();
    }

    @Test
    public void testConfigMetadataDefaultNull() {
        assertThat(request.getConfigMetadata()).isNull();
    }

    @Test
    public void testSetConfigMetadata() {
        String metadata = "{\"config_watch\":[]}";
        request.setConfigMetadata(metadata);
        assertThat(request.getConfigMetadata()).isEqualTo(metadata);
    }

    @Test
    public void testToStringContainsConfigFields() {
        request.setConfigEnabled(true);
        request.setConfigMetadata("{\"config_watch\":[]}");

        String str = request.toString();
        assertThat(str).contains("configEnabled=true");
        assertThat(str).contains("configMetadata");
    }
}
