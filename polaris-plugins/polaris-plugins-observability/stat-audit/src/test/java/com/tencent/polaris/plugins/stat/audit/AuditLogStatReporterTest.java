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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.config.global.ClusterType;
import com.tencent.polaris.api.config.global.StatReporterConfig;
import com.tencent.polaris.api.plugin.Supplier;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.common.ValueContext;
import com.tencent.polaris.api.plugin.compose.ServerServiceInfo;
import com.tencent.polaris.api.plugin.stat.StatInfo;
import com.tencent.polaris.api.plugin.stat.StatReporter;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import com.tencent.polaris.client.api.ServiceCallResultListener;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.factory.config.consumer.ConsumerConfigImpl;
import com.tencent.polaris.factory.config.global.ClusterConfigImpl;
import com.tencent.polaris.plugins.stat.common.api.ServiceCallStatCollector;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.ServiceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Test for {@link AuditLogStatReporter}.
 *
 * @author Yuwei Fu
 */
public class AuditLogStatReporterTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Test purpose: verify the reporter converts a service call to the expected single-line JSON.
     * Test scenario: construct a result with caller, callee, return code, and duration.
     * Verification: all audit fields are correct and the output contains no line breaks.
     */
    @Test
    public void testToJson() throws Exception {
        AuditLogStatReporter reporter = newReporter(true, Collections.emptyList());

        ServiceCallResult result = serviceCallResult("Production", "user-service");
        result.setHost("10.0.2.10");
        result.setPort(8080);
        result.setMethod("/api/user/get");
        result.setRetCode(200);
        result.setRetStatus(RetStatus.RetSuccess);
        result.setDelay(50);
        result.setCallerService(new ServiceKey("Production", "order-service"));
        result.setCallerIp("10.0.1.5");

        String json = reporter.toJson(result);
        JsonNode node = MAPPER.readTree(json);

        assertThat(node.get("timestamp").isTextual()).isTrue();
        assertThat(node.get("caller_namespace").asText()).isEqualTo("Production");
        assertThat(node.get("caller_service").asText()).isEqualTo("order-service");
        assertThat(node.get("caller_ip").asText()).isEqualTo("10.0.1.5");
        assertThat(node.get("callee_namespace").asText()).isEqualTo("Production");
        assertThat(node.get("callee_service").asText()).isEqualTo("user-service");
        assertThat(node.get("callee_host").asText()).isEqualTo("10.0.2.10:8080");
        assertThat(node.get("instance_id").asText()).isEmpty();
        assertThat(node.get("method").asText()).isEqualTo("/api/user/get");
        assertThat(node.get("ret_code").asInt()).isEqualTo(200);
        assertThat(node.get("ret_status").asText()).isEqualTo("RetSuccess");
        assertThat(node.get("delay").asLong()).isEqualTo(50);
        assertThat(node.get("request_count").asInt()).isEqualTo(1);
        assertThat(json).doesNotContain("\n", "\r");
    }

    /**
     * Test purpose: verify the reporter ignores business calls when audit logging is disabled.
     * Test scenario: initialize the reporter with the audit switch disabled.
     * Verification: an ordinary business service call is rejected.
     */
    @Test
    public void testDisabled() {
        AuditLogStatReporter reporter = newReporter(false, Collections.emptyList());
        assertThat(reporter.shouldReport(serviceCallResult("Production", "user-service"))).isFalse();
    }

    /**
     * Test purpose: verify SDK internal calls are not written to the audit log.
     * Test scenario: construct builtin, configured system, and business service calls.
     * Verification: system services are filtered while the business service is accepted.
     */
    @Test
    public void testSdkInternalServicesSkipped() {
        ClusterConfigImpl clusterConfig = new ClusterConfigImpl();
        clusterConfig.setNamespace("System");
        clusterConfig.setService("custom-discover");
        clusterConfig.setRouters(Collections.emptyList());
        ServerServiceInfo serverServiceInfo = new ServerServiceInfo(
                ClusterType.SERVICE_DISCOVER_CLUSTER, clusterConfig);

        AuditLogStatReporter reporter = newReporter(true, Collections.singletonList(serverServiceInfo));

        assertThat(reporter.shouldReport(serviceCallResult("Polaris", "polaris.builtin"))).isFalse();
        assertThat(reporter.shouldReport(serviceCallResult("System", "custom-discover"))).isFalse();
        assertThat(reporter.shouldReport(serviceCallResult("Production", "user-service"))).isTrue();
    }

    /**
     * Test purpose: verify the reporter only consumes service call statistics from StatInfo.
     * Test scenario: pass null, an empty StatInfo, and a valid routerGauge.
     * Verification: missing data is ignored and valid data is handled safely.
     */
    @Test
    public void testReportStat() {
        AuditLogStatReporter reporter = newReporter(true, Collections.emptyList());

        assertThatCode(() -> reporter.reportStat(null)).doesNotThrowAnyException();
        assertThatCode(() -> reporter.reportStat(new StatInfo())).doesNotThrowAnyException();

        StatInfo statInfo = new StatInfo();
        statInfo.setRouterGauge(serviceCallResult("Production", "user-service"));
        assertThatCode(() -> reporter.reportStat(statInfo)).doesNotThrowAnyException();
    }

    /**
     * Test purpose: verify the complete audit statistics path is exposed through SPI.
     * Test scenario: load StatReporter and ServiceCallResultListener implementations.
     * Verification: both the audit reporter and the common statistics collector are available.
     */
    @Test
    public void testStatReporterSpi() {
        AuditLogStatReporter auditReporter = null;
        for (StatReporter reporter : ServiceLoader.load(StatReporter.class)) {
            if (reporter instanceof AuditLogStatReporter) {
                auditReporter = (AuditLogStatReporter) reporter;
                break;
            }
        }

        assertThat(auditReporter).isNotNull();
        assertThat(auditReporter.getName()).isEqualTo(StatReporterConfig.DEFAULT_REPORTER_AUDIT_LOG);
        assertThat(auditReporter.getType()).isEqualTo(PluginTypes.STAT_REPORTER.getBaseType());

        boolean collectorLoaded = false;
        for (ServiceCallResultListener listener : ServiceLoader.load(ServiceCallResultListener.class)) {
            if (listener instanceof ServiceCallStatCollector) {
                collectorLoaded = true;
                break;
            }
        }
        assertThat(collectorLoaded).isTrue();
    }

    private static AuditLogStatReporter newReporter(boolean enable,
            Collection<ServerServiceInfo> serverServices) {
        AuditLogStatReporter reporter = new AuditLogStatReporter();
        reporter.init(initContext(serverServices));
        reporter.setEnable(enable);
        return reporter;
    }

    private static ServiceCallResult serviceCallResult(String namespace, String service) {
        ServiceCallResult result = new ServiceCallResult();
        result.setNamespace(namespace);
        result.setService(service);
        return result;
    }

    private static InitContext initContext(Collection<ServerServiceInfo> serverServices) {
        ConfigurationImpl configuration = new ConfigurationImpl();
        configuration.setConsumer(new ConsumerConfigImpl());
        return new InitContext() {
            @Override
            public Configuration getConfig() {
                return configuration;
            }

            @Override
            public Supplier getPlugins() {
                return null;
            }

            @Override
            public ValueContext getValueContext() {
                return new ValueContext();
            }

            @Override
            public Collection<ServerServiceInfo> getServerServices() {
                return serverServices;
            }
        };
    }
}
