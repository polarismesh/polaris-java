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

package com.tencent.polaris.plugins.stat.audit;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.config.global.StatReporterConfig;
import com.tencent.polaris.factory.ConfigAPIFactory;
import com.tencent.polaris.factory.config.global.StatReporterConfigImpl;
import org.junit.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for the default audit log configuration.
 *
 * @author Yuwei Fu
 */
public class AuditLogConfigLoadTest {

    /**
     * Test purpose: verify the auditLog section in the default configuration can be loaded.
     * Test scenario: load conf/default-config.yml.
     * Verification: audit logging is disabled and its format is JSON by default.
     */
    @Test
    public void testDefaultConfigLoadsAuditLog() {
        InputStream inputStream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("conf/default-config.yml");
        assertThat(inputStream).isNotNull();

        Configuration configuration = ConfigAPIFactory.loadConfig(inputStream);
        StatReporterConfigImpl statReporter = (StatReporterConfigImpl)
                configuration.getGlobal().getStatReporter();
        AuditLogConfig auditLogConfig = statReporter.getPluginConfig(
                StatReporterConfig.DEFAULT_REPORTER_AUDIT_LOG, AuditLogConfig.class);
        assertThat(auditLogConfig).isNotNull();
        assertThat(auditLogConfig.isEnable()).isFalse();
        assertThat(auditLogConfig.getFormat()).isEqualTo("json");
    }
}
