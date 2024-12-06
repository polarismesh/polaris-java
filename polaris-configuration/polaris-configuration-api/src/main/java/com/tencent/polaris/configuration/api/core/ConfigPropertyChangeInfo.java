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

package com.tencent.polaris.configuration.api.core;

/**
 * @author lepdou 2022-03-01
 */
public class ConfigPropertyChangeInfo {

    private final String     propertyName;
    private final Object     oldValue;
    private final Object     newValue;
    private final ChangeType changeType;

    public ConfigPropertyChangeInfo(String propertyName,
                                    Object oldValue, Object newValue,
                                    ChangeType changeType) {
        this.propertyName = propertyName;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.changeType = changeType;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public Object getNewValue() {
        return newValue;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    @Override
    public String toString() {
        return "ConfigPropertyChangeInfo{" +
               ", propertyName='" + propertyName + '\'' +
               ", oldValue='" + oldValue + '\'' +
               ", newValue='" + newValue + '\'' +
               ", changeType=" + changeType +
               '}';
    }
}
