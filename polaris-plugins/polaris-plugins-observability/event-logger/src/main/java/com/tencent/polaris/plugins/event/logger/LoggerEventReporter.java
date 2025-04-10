/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.polaris.plugins.event.logger;

import com.tencent.polaris.api.config.plugin.DefaultPlugins;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.event.BaseEvent;
import com.tencent.polaris.api.plugin.event.EventReporter;
import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static com.tencent.polaris.logging.LoggingConsts.LOGGING_EVENT;

/**
 * @author Haotian Zhang
 */
public class LoggerEventReporter implements EventReporter {

    private static final Logger EVENT_LOG = LoggerFactory.getLogger(LOGGING_EVENT);
    private static final Logger LOG = LoggerFactory.getLogger(LoggerEventReporter.class);

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean reportEvent(BaseEvent flowEvent) {
        try {
            EVENT_LOG.info(flowEvent.convertMessage());
            return true;
        } catch (Throwable throwable) {
            LOG.warn("Failed to log flow event. {}", flowEvent, throwable);
            return false;
        }
    }

    @Override
    public String getName() {
        return DefaultPlugins.LOGGER_EVENT_REPORTER_TYPE;
    }

    @Override
    public PluginType getType() {
        return PluginTypes.EVENT_REPORTER.getBaseType();
    }

    @Override
    public void init(InitContext ctx) throws PolarisException {

    }

    @Override
    public void postContextInit(Extensions ctx) throws PolarisException {

    }

    @Override
    public void destroy() {

    }
}
