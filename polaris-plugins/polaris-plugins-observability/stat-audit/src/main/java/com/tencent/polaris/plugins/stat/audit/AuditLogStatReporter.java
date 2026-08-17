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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.polaris.api.config.global.StatReporterConfig;
import com.tencent.polaris.api.config.verify.DefaultValues;
import com.tencent.polaris.api.config.verify.Verifier;
import com.tencent.polaris.api.config.plugin.PluginConfigProvider;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.compose.ServerServiceInfo;
import com.tencent.polaris.api.plugin.stat.ReporterMetaInfo;
import com.tencent.polaris.api.plugin.stat.StatInfo;
import com.tencent.polaris.api.plugin.stat.StatReporter;
import com.tencent.polaris.api.pojo.InstanceGauge;
import com.tencent.polaris.api.pojo.Service;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.logging.LoggingConsts;
import org.slf4j.Logger;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Writes each business service call result to the audit log.
 *
 * @author Yuwei Fu
 */
public class AuditLogStatReporter implements StatReporter, PluginConfigProvider {

    private static final Logger LOG = LoggerFactory.getLogger(AuditLogStatReporter.class);

    private static final Logger AUDIT_LOG = LoggerFactory.getLogger(LoggingConsts.LOGGING_AUDIT);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    private volatile boolean enable;

    void setEnable(boolean enable) {
        this.enable = enable;
    }

    private Set<ServiceKey> sdkInternalServices = Collections.emptySet();

    @Override
    public void init(InitContext ctx) throws PolarisException {
        Set<ServiceKey> internalServices = new HashSet<>();
        internalServices.add(new ServiceKey(DefaultValues.DEFAULT_SYSTEM_NAMESPACE,
                DefaultValues.DEFAULT_BUILTIN_DISCOVER));
        for (ServerServiceInfo serverService : ctx.getServerServices()) {
            ServiceKey serviceKey = serverService.getServiceKey();
            internalServices.add(new ServiceKey(serviceKey.getNamespace(), serviceKey.getService()));
        }
        sdkInternalServices = Collections.unmodifiableSet(internalServices);
    }

    @Override
    public void reportStat(StatInfo statInfo) {
        if (statInfo == null) {
            return;
        }
        InstanceGauge result = statInfo.getRouterGauge();
        if (!shouldReport(result)) {
            return;
        }
        try {
            AUDIT_LOG.info(toJson(result));
        } catch (JsonProcessingException e) {
            LOG.warn("Failed to serialize service call audit record", e);
        }
    }

    boolean shouldReport(InstanceGauge result) {
        return enable && result != null
                && !sdkInternalServices.contains(new ServiceKey(result.getNamespace(), result.getService()));
    }

    String toJson(InstanceGauge result) throws JsonProcessingException {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("timestamp", TIMESTAMP_FORMATTER.format(ZonedDateTime.now()));

        Service callerService = result.getCallerService();
        record.put("caller_namespace", callerService == null ? null : callerService.getNamespace());
        record.put("caller_service", callerService == null ? null : callerService.getService());
        record.put("caller_ip", result.getCallerIp());

        record.put("callee_namespace", result.getNamespace());
        record.put("callee_service", result.getService());
        record.put("callee_host", result.getHost() == null ? null : result.getHost() + ":" + result.getPort());
        record.put("instance_id", result.getInstanceId());
        record.put("method", result.getMethod());
        record.put("ret_code", result.getRetCode());
        record.put("ret_status", result.getRetStatus() == null ? null : result.getRetStatus().name());
        record.put("delay", result.getDelay());
        record.put("request_count", 1);
        return MAPPER.writeValueAsString(record);
    }

    @Override
    public ReporterMetaInfo metaInfo() {
        return ReporterMetaInfo.builder().build();
    }

    @Override
    public String getName() {
        return StatReporterConfig.DEFAULT_REPORTER_AUDIT_LOG;
    }

    @Override
    public PluginType getType() {
        return PluginTypes.STAT_REPORTER.getBaseType();
    }

    @Override
    public void postContextInit(Extensions extensions) throws PolarisException {
        AuditLogConfig config = extensions.getConfiguration().getGlobal().getStatReporter()
                .getPluginConfig(getName(), AuditLogConfig.class);
        enable = config != null && config.isEnable();
    }

    @Override
    public Class<? extends Verifier> getPluginConfigClazz() {
        return AuditLogConfig.class;
    }

    @Override
    public void destroy() {
    }
}
