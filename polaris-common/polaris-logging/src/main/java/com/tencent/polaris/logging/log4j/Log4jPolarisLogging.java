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

package com.tencent.polaris.logging.log4j;

import com.tencent.polaris.logging.AbstractPolarisLogging;
import java.net.URL;
import org.apache.log4j.xml.DOMConfigurator;

public class Log4jPolarisLogging extends AbstractPolarisLogging {

    private static final String LOG4J2_LOCATION = "classpath:polaris-log4j.xml";

    private static final String FILE_PROTOCOL = "file";

    private static final String LOGGER_PREFIX = "com.tencent.polaris";

    private final String location = getLocation(LOG4J2_LOCATION);

    @Override
    public void loadConfiguration() {
        if (null == location || location.trim().length() == 0) {
            return;
        }
        URL url = loadConfiguration(location);
        DOMConfigurator.configure(url);
    }

    private URL loadConfiguration(String location) {
        try {
            return getResourceUrl(location);
        } catch (Exception e) {
            throw new IllegalStateException("could not initialize log4j logging from " + location, e);
        }
    }
}
