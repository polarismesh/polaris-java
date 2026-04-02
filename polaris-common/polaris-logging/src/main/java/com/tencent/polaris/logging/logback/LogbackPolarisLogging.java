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

package com.tencent.polaris.logging.logback;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.LogbackException;
import com.tencent.polaris.logging.AbstractPolarisLogging;
import com.tencent.polaris.logging.LoggingConsts;
import org.slf4j.LoggerFactory;

import java.net.URL;

public class LogbackPolarisLogging extends AbstractPolarisLogging {

    private static final String LOGBACK_LOCATION = "classpath:polaris-logback.xml";

    /**
     * Logger names defined in polaris-logback.xml. Used to clean up existing
     * appenders before reloading so that repeated calls to
     * {@link #loadConfiguration()} replace appenders instead of duplicating them.
     */
    private static final String[] POLARIS_LOGGER_NAMES = {
            LoggingConsts.LOGGING_POLARIS_PACKAGE,
            LoggingConsts.LOGGING_UPDATE_EVENT,
            LoggingConsts.LOGGING_UPDATE_EVENT_ASYNC,
            LoggingConsts.LOGGING_CIRCUIT_BREAKER,
            LoggingConsts.LOGGING_HEALTHCHECK_EVENT,
            LoggingConsts.LOGGING_LOSSLESS_EVENT,
            LoggingConsts.LOGGING_HEARTBEAT_RECORD,
            LoggingConsts.LOGGING_EVENT,
    };

    @Override
    public void loadConfiguration() {
        String location = getLocation(LOGBACK_LOCATION);
        if (null == location || location.trim().length() == 0) {
            return;
        }

        try {
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            cleanupPolarisLoggers(loggerContext);
            // Fix issue: https://github.com/Tencent/spring-cloud-tencent/issues/1099
            URL url = getResourceUrl(location);
            final String urlString = url.toString();
            if (urlString.endsWith("xml")) {
                PolarisJoranConfigurator configurator = new PolarisJoranConfigurator();
                configurator.setContext(loggerContext);
                configurator.doPolarisConfigure(url);
            } else {
                throw new LogbackException("Unexpected filename extension of file [" + url + "]. Should be"
                        + " either .groovy or .xml");
            }
        } catch (Exception e) {
            throw new IllegalStateException("could not initialize logback logging from " + location, e);
        }
    }

    /**
     * Detach and stop all appenders from Polaris loggers so that a subsequent
     * {@code doConfigure} call replaces them instead of adding duplicates.
     */
    private static void cleanupPolarisLoggers(LoggerContext loggerContext) {
        for (String loggerName : POLARIS_LOGGER_NAMES) {
            Logger logger = loggerContext.exists(loggerName);
            if (logger != null) {
                logger.detachAndStopAllAppenders();
            }
        }
    }

}
