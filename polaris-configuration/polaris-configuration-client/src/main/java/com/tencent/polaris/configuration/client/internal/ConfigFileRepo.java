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

package com.tencent.polaris.configuration.client.internal;

/**
 * @author lepdou 2022-03-02
 */
public interface ConfigFileRepo {

    String getContent();

    String getMd5();

    void addChangeListener(ConfigFileRepoChangeListener listener);

    void removeChangeListener(ConfigFileRepoChangeListener listener);

    /**
     * 当前配置在服务端是否为加密配置。
     *
     * <p>default 返回 false：本地文件、consul 等数据源不涉及服务端加密，只有远端仓库需要覆写。
     *
     * @return 加密配置返回 true
     */
    default boolean isEncrypted() {
        return false;
    }

}
