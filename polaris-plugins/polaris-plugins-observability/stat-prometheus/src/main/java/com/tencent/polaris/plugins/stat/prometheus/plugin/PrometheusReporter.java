/*
 * Tencent is pleased to support the open source community by making Polaris available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.polaris.plugins.stat.prometheus.plugin;

import com.tencent.polaris.api.config.global.StatReporterConfig;
import com.tencent.polaris.api.config.plugin.PluginConfigProvider;
import com.tencent.polaris.api.config.verify.Verifier;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.stat.StatInfo;
import com.tencent.polaris.api.plugin.stat.StatReporter;
import com.tencent.polaris.plugins.stat.common.model.StatInfoHandler;
import com.tencent.polaris.plugins.stat.prometheus.handler.PrometheusPushHandler;
import com.tencent.polaris.plugins.stat.prometheus.handler.PrometheusPushHandlerConfig;
import com.tencent.polaris.plugins.stat.prometheus.handler.ServiceDiscoveryProvider;

public class PrometheusReporter implements StatReporter, PluginConfigProvider {

    public static final String PUSH_DEFAULT_JOB_NAME = "polaris-client";

    private StatInfoHandler statInfoHandler;

    @Override
    public void init(InitContext initContext) throws PolarisException {
    }

    @Override
    public void postContextInit(Extensions extensions) throws PolarisException {
        if (extensions.getConfiguration().getGlobal().getStatReporter().isEnable()) {
            PrometheusPushHandlerConfig config = extensions.getConfiguration()
                    .getGlobal()
                    .getStatReporter()
                    .getPluginConfig(getName(), PrometheusPushHandlerConfig.class);
            ServiceDiscoveryProvider provider = new ServiceDiscoveryProvider(extensions, config);
            statInfoHandler = new PrometheusPushHandler(extensions.getValueContext().getHost(),
                    config, provider, PUSH_DEFAULT_JOB_NAME);
        }
    }

    @Override
    public void reportStat(StatInfo statInfo) {
        if (null != statInfoHandler && null != statInfo) {
            statInfoHandler.handle(statInfo);
        }
    }

    @Override
    public String getName() {
        return StatReporterConfig.DEFAULT_REPORTER_PROMETHEUS;
    }

    @Override
    public Class<? extends Verifier> getPluginConfigClazz() {
        return PrometheusPushHandlerConfig.class;
    }

    @Override
    public PluginType getType() {
        return PluginTypes.STAT_REPORTER.getBaseType();
    }

    @Override
    public void destroy() {
        if (null != statInfoHandler) {
            statInfoHandler.stopHandle();
            statInfoHandler = null;
        }
    }
}
