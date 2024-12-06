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

package com.tencent.polaris.api.plugin;

import java.util.Objects;

/**
 * 插件类型定义
 */
public class PluginType implements Comparable<PluginType> {

    /**
     * 插件的类型
     */
    private final Class<? extends Plugin> clazz;

    /**
     * 插件等级，决定初始化顺序
     */
    private final int level;

    public PluginType(Class<? extends Plugin> clazz, int level) {
        this.clazz = clazz;
        this.level = level;
    }

    public Class<? extends Plugin> getClazz() {
        return clazz;
    }

    public int getLevel() {
        return level;
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PluginType that = (PluginType) o;
        return level == that.level &&
                Objects.equals(clazz, that.clazz);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clazz, level);
    }

    @Override
    public int compareTo(PluginType o) {
        return level - o.getLevel();
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public String toString() {
        return "PluginType{" +
                "clazz=" + clazz.getCanonicalName() +
                ", level=" + level +
                '}';
    }
}
