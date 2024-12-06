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

package com.tencent.polaris.configuration.client.internal;

import com.tencent.polaris.configuration.api.core.ConfigFileGroupMetadata;

import java.util.Objects;

public class DefaultConfigFileGroupMetadata implements ConfigFileGroupMetadata {
    private final String namespace;
    private final String fileGroupName;

    public DefaultConfigFileGroupMetadata(String namespace, String fileGroupName) {
        this.namespace = namespace;
        this.fileGroupName = fileGroupName;
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public String getFileGroupName() {
        return fileGroupName;
    }

    @Override
    public String toString() {
        return "DefaultConfigFileGroupMetadata{" +
                "namespace='" + namespace + '\'' +
                ", fileGroupName='" + fileGroupName + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefaultConfigFileGroupMetadata that = (DefaultConfigFileGroupMetadata) o;
        return Objects.equals(namespace, that.namespace) && Objects.equals(fileGroupName, that.fileGroupName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, fileGroupName);
    }
}
