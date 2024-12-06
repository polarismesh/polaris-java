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

import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;

import java.util.Objects;

/**
 * @author lepdou 2022-03-01
 */
public class DefaultConfigFileMetadata implements ConfigFileMetadata {

    private final String namespace;
    private final String fileGroup;
    private final String fileName;

    public DefaultConfigFileMetadata(String namespace, String fileGroup, String fileName) {
        this.namespace = namespace;
        this.fileGroup = fileGroup;
        this.fileName = fileName;
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public String getFileGroup() {
        return fileGroup;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, fileGroup, fileName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultConfigFileMetadata that = (DefaultConfigFileMetadata) o;
        return namespace.equals(that.namespace) &&
               fileGroup.equals(that.fileGroup) &&
               fileName.equals(that.fileName);
    }

    @Override
    public String toString() {
        return "ConfigFile{" +
               "namespace='" + namespace + '\'' +
               ", fileGroup='" + fileGroup + '\'' +
               ", fileName='" + fileName + '\'' +
               '}';
    }
}
