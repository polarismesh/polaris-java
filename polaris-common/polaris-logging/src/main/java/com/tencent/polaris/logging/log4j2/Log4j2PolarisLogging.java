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

package com.tencent.polaris.logging.log4j2;

import com.tencent.polaris.logging.AbstractPolarisLogging;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.status.StatusLogger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

public class Log4j2PolarisLogging extends AbstractPolarisLogging {

    private static final String LOG4J2_LOCATION = "classpath:polaris-log4j2.xml";

    private static final String FILE_PROTOCOL = "file";

    private final String location = getLocation(LOG4J2_LOCATION);

    @Override
    public void loadConfiguration() {
        if (null == location || location.trim().length() == 0) {
            return;
        }

        final LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
        final Configuration contextConfiguration = loggerContext.getConfiguration();

        Configuration configuration = loadConfiguration(loggerContext, location);
        configuration.start();

        // Remove existing appenders/loggers before re-adding to avoid duplicates.
        // removeAppender() only exists on AbstractConfiguration, not the Configuration interface.
        AbstractConfiguration abstractConfig = null;
        if (contextConfiguration instanceof AbstractConfiguration) {
            abstractConfig = (AbstractConfiguration) contextConfiguration;
        } else {
            StatusLogger.getLogger().warn("Log4j2 contextConfiguration ({}) is not an instance of "
                    + "AbstractConfiguration, cannot remove old appenders before re-adding. "
                    + "Repeated loadConfiguration() calls may cause duplicate appenders.",
                    contextConfiguration.getClass().getName());
        }
        Map<String, Appender> appenders = configuration.getAppenders();
        for (Appender appender : appenders.values()) {
            if (abstractConfig != null) {
                abstractConfig.removeAppender(appender.getName());
            }
            contextConfiguration.addAppender(appender);
        }
        Map<String, LoggerConfig> loggers = configuration.getLoggers();
        for (Map.Entry<String, LoggerConfig> entry : loggers.entrySet()) {
            if (abstractConfig != null) {
                contextConfiguration.removeLogger(entry.getKey());
            }
            contextConfiguration.addLogger(entry.getKey(), entry.getValue());
        }
        loggerContext.updateLoggers();
    }

    private Configuration loadConfiguration(LoggerContext loggerContext, String location) {
        try {
            URL url = getResourceUrl(location);
            ConfigurationSource source = getConfigurationSource(url);
            // since log4j 2.7 getConfiguration(LoggerContext loggerContext, ConfigurationSource source)
            return ConfigurationFactory.getInstance().getConfiguration(loggerContext, source);
        } catch (Exception e) {
            throw new IllegalStateException("could not initialize log4j2 logging from " + location, e);
        }
    }

    private ConfigurationSource getConfigurationSource(URL url) throws IOException {
        InputStream stream = url.openStream();
        if (FILE_PROTOCOL.equals(url.getProtocol())) {
            return new ConfigurationSource(stream, getResourceAsFile(url));
        }
        return new ConfigurationSource(stream, url);
    }
}
