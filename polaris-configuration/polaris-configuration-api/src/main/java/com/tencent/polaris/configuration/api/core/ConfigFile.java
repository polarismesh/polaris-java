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

    String getMd5();

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

    /**
     * 该配置文件在服务端是否为加密配置。
     *
     * <p>供上层框架（如 Spring Cloud Tencent）在**首次加载**时就能判断敏感性：加密标记原本只出现在
     * 变更事件携带的插件层对象上，首次加载拿不到，导致上层只能靠开关粗粒度兜底。
     *
     * <p>取值来自服务端下发的响应对象，是逐文件真值。注意加密过滤器会在**请求**前把该标记置为 true
     * 用于声明客户端支持加密，因此只有响应侧对象的该字段可信。
     *
     * <p>default 实现返回 false：本接口有外部实现者，加密能力是可选特性，未覆写即视为非加密。
     *
     * @return 加密配置返回 true
     */
    default boolean isEncrypted() {
        return false;
    }

}
