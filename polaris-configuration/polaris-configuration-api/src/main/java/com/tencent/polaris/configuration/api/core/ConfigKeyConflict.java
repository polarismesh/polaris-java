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

/**
 * 同名 key 冲突项，承载某个被监听配置文件中与目标 key 同名、但来源不同的值。
 * 仅包含冲突文件坐标与该 key 的值，不含冲突文件完整内容。
 *
 * @author evelynwei
 */
public class ConfigKeyConflict {

    /**
     * 冲突文件命名空间。
     */
    private final String namespace;

    /**
     * 冲突文件分组。
     */
    private final String group;

    /**
     * 冲突文件名。
     */
    private final String fileName;

    /**
     * 该 key 在冲突文件中的值。
     */
    private final String value;

    public ConfigKeyConflict(String namespace, String group, String fileName, String value) {
        this.namespace = namespace;
        this.group = group;
        this.fileName = fileName;
        this.value = value;
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

    public String getValue() {
        return value;
    }
}
