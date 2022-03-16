/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.polaris.logging.log4j2;

import com.tencent.polaris.logging.AbstractPolarisLogging;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.LoggerConfig;

public class Log4j2PolarisLogging extends AbstractPolarisLogging {

    private static final String LOG4J2_LOCATION = "classpath:polaris-log4j2.xml";

    private static final String FILE_PROTOCOL = "file";

    private static final String LOGGER_PREFIX = "com.tencent.polaris";

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

        // append loggers and appenders to contextConfiguration
        Map<String, Appender> appenders = configuration.getAppenders();
        for (Appender appender : appenders.values()) {
            contextConfiguration.addAppender(appender);
        }
        Map<String, LoggerConfig> loggers = configuration.getLoggers();
        for (String name : loggers.keySet()) {
            if (name.startsWith(LOGGER_PREFIX)) {
                contextConfiguration.addLogger(name, loggers.get(name));
            }
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
