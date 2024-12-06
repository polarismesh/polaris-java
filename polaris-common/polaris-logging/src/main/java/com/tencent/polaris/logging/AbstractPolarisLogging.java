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

package com.tencent.polaris.logging;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public abstract class AbstractPolarisLogging {

    static {
        String loggingPath = System.getProperty(LoggingConsts.LOGGING_PATH_PROPERTY);
        if (null == loggingPath || loggingPath.trim().length() == 0) {
            String userHome = System.getProperty("user.dir");
            System.setProperty(LoggingConsts.LOGGING_PATH_PROPERTY,
                    userHome + File.separator + "polaris" + File.separator + "logs");
        }
    }

    protected String getLocation(String defaultLocation) {
        String location = System.getProperty(LoggingConsts.LOGGING_CONFIG_PROPERTY);
        if (null == location || location.trim().length() == 0) {
            return defaultLocation;
        }
        return location;
    }

    private static final String CLASSPATH_PREFIX = "classpath:";

    /**
     * Returns the URL of the resource on the classpath.
     *
     * @param resource The resource to find
     * @return The resource
     * @throws IOException If the resource cannot be found or read
     */
    public static URL getResourceUrl(String resource) throws IOException {
        if (resource.startsWith(CLASSPATH_PREFIX)) {
            String path = resource.substring(CLASSPATH_PREFIX.length());

            ClassLoader classLoader = AbstractPolarisLogging.class.getClassLoader();

            URL url = (classLoader != null ? classLoader.getResource(path) : ClassLoader.getSystemResource(path));
            if (url == null) {
                throw new FileNotFoundException("Resource [" + resource + "] does not exist");
            }

            return url;
        }

        try {
            return new URL(resource);
        } catch (MalformedURLException ex) {
            return new File(resource).toURI().toURL();
        }
    }

    /**
     * Returns a resource on the classpath as a File object.
     *
     * @param url The resource url to find
     * @return The resource
     */
    public static File getResourceAsFile(URL url) {
        return new File(url.getFile());
    }

    /**
     * Load logging configuration.
     */
    public abstract void loadConfiguration();
}
