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

package com.tencent.polaris.logging.logback;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.ContextInitializer;
import com.tencent.polaris.logging.AbstractPolarisLogging;
import org.slf4j.LoggerFactory;

public class LogbackPolarisLogging extends AbstractPolarisLogging {

    private static final String LOGBACK_LOCATION = "classpath:polaris-logback.xml";

    @Override
    public void loadConfiguration() {
        String location = getLocation(LOGBACK_LOCATION);
        if (null == location || location.trim().length() == 0) {
            return;
        }

        try {
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            new ContextInitializer(loggerContext).configureByResource(getResourceUrl(location));
        } catch (Exception e) {
            throw new IllegalStateException("could not initialize logback logging from " + location, e);
        }
    }

}
