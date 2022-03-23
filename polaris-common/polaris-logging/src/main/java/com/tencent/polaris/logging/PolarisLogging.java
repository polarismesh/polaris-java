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

package com.tencent.polaris.logging;

import com.tencent.polaris.logging.log4j.Log4jPolarisLogging;
import com.tencent.polaris.logging.log4j2.Log4j2PolarisLogging;
import com.tencent.polaris.logging.logback.LogbackPolarisLogging;
import org.slf4j.Logger;

public class PolarisLogging {

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(PolarisLogging.class);
    
    public static final String LOGBACK_CLASSIC_LOGGER = "ch.qos.logback.classic.Logger";

    public static final String LOG4J2_CLASSIC_LOGGER = "org.apache.logging.log4j.LogManager";

    private AbstractPolarisLogging polarisLogging;

    private boolean isLogback = false;

    private PolarisLogging() {
        try {
            Class.forName(LOGBACK_CLASSIC_LOGGER);
            polarisLogging = new LogbackPolarisLogging();
            isLogback = true;
        } catch (ClassNotFoundException e) {
            try {
                Class.forName(LOG4J2_CLASSIC_LOGGER);
                polarisLogging = new Log4j2PolarisLogging();
            } catch (ClassNotFoundException e1) {
                polarisLogging = new Log4jPolarisLogging();
            }
        }
    }

    private static class PolarisLoggingInstance {

        private static final PolarisLogging INSTANCE = new PolarisLogging();
    }

    public static PolarisLogging getInstance() {
        return PolarisLoggingInstance.INSTANCE;
    }

    /**
     * Load logging Configuration.
     */
    public void loadConfiguration() {
        try {
            polarisLogging.loadConfiguration();
        } catch (Throwable t) {
            if (isLogback) {
                LOGGER.warn("fail to load logback configuration: {}", t.getMessage());
            } else {
                LOGGER.warn("fail to load log4j configuration: {}", t.getMessage());
            }
        }
    }
}
