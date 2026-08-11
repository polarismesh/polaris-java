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

package com.tencent.polaris.factory.config.global;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link ClientReporterConfigImpl}.
 *
 * @author fishtailfu
 */
public class ClientReporterConfigImplTest {

    private ClientReporterConfigImpl config;

    @Before
    public void setUp() {
        config = new ClientReporterConfigImpl();
    }

    @Test
    public void testIsEnableDefaultFalse() {
        assertThat(config.isEnable()).isFalse();
    }

    @Test
    public void testSetEnable() {
        config.setEnable(true);
        assertThat(config.isEnable()).isTrue();
    }

    @Test
    public void testVerifyEnableNullThrows() {
        assertThatThrownBy(() -> config.verify())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("clientReporter.enable");
    }

    @Test
    public void testVerifyEnableValid() {
        config.setEnable(false);
        assertThatCode(() -> config.verify()).doesNotThrowAnyException();
    }

    @Test
    public void testSetDefaultFromNull() {
        ClientReporterConfigImpl defaults = new ClientReporterConfigImpl();
        defaults.setEnable(true);
        config.setDefault(defaults);

        assertThat(config.isEnable()).isTrue();
    }

    @Test
    public void testSetDefaultDoesNotOverrideExisting() {
        config.setEnable(false);
        ClientReporterConfigImpl defaults = new ClientReporterConfigImpl();
        defaults.setEnable(true);
        config.setDefault(defaults);

        assertThat(config.isEnable()).isFalse();
    }

    @Test
    public void testSetDefaultNullObject() {
        config.setDefault(null);
        assertThat(config.isEnable()).isFalse();
    }

    @Test
    public void testToString() {
        config.setEnable(true);
        assertThat(config.toString()).contains("enable=true");
    }
}
