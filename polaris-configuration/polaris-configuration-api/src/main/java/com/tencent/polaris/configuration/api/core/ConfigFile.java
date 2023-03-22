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

package com.tencent.polaris.configuration.api.core;

import java.lang.reflect.Type;

/**
 * @author lepdou 2022-03-01
 */
public interface ConfigFile extends ConfigFileMetadata {

    /**
     * Get the content of the configuration file. If it has not been published, null will be returned
     *
     * @return the content of the configuration file
     */
    String getContent();

    /**
     * Deserialize to json object with given class type by gson. Default value will be returned when content is blank or
     * some error occurred.
     *
     * @param objectType   the type of class
     * @param defaultValue the default value
     * @param <T>
     * @return Deserialize result of json object.
     */
    <T> T asJson(Class<T> objectType, T defaultValue);

    /**
     * Deserialize to json object with given class type by gson. Default value will be returned when content is blank or
     * some error occurred.
     *
     * @param typeOfT      the type of class
     * @param defaultValue the default value
     * @param <T>
     * @return Deserialize result of json object.
     */
    <T> T asJson(Type typeOfT, T defaultValue);


    /**
     * Whether the configuration file contains content. If it has not been published or content is blank string, false
     * will be returned
     *
     * @return Whether the configuration file contains content
     */
    boolean hasContent();

    /**
     * Adding a config file change listener, will trigger a callback when the config file is published
     *
     * @param listener the listener will be added
     */
    void addChangeListener(ConfigFileChangeListener listener);

    /**
     * remove a config file change listener
     *
     * @param listener the listener will be removed
     */
    void removeChangeListener(ConfigFileChangeListener listener);

}
