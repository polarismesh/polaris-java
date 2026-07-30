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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.tencent.polaris.configuration.client.internal;

/**
 * 配置监听画像中的单个监听项快照。
 *
 * @author polaris
 */
public class ConfigWatchInfo {

    private final String namespace;

    private final String group;

    private final String fileName;

    private final long version;

    private final String md5;

    public ConfigWatchInfo(String namespace, String group, String fileName, long version, String md5) {
        this.namespace = namespace;
        this.group = group;
        this.fileName = fileName;
        this.version = version;
        this.md5 = md5;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getGroup() {
        return group;
    }

    public String getFileName() {
        return fileName;
    }

    public long getVersion() {
        return version;
    }

    public String getMd5() {
        return md5;
    }
}
