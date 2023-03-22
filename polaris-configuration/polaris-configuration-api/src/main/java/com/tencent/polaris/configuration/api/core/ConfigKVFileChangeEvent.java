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

import java.util.Map;
import java.util.Set;

/**
 * @author lepdou 2022-03-01
 */
public class ConfigKVFileChangeEvent {

    private final Map<String, ConfigPropertyChangeInfo> propertyChangeInfos;

    public ConfigKVFileChangeEvent(Map<String, ConfigPropertyChangeInfo> changeInfos) {
        this.propertyChangeInfos = changeInfos;
    }

    public Set<String> changedKeys() {
        return propertyChangeInfos.keySet();
    }

    public ConfigPropertyChangeInfo getChangeInfo(String propertyKey) {
        return propertyChangeInfos.get(propertyKey);
    }

    public boolean isChanged(String propertyKey) {
        return propertyChangeInfos.containsKey(propertyKey);
    }

    public String getPropertyOldValue(String propertyKey) {
        ConfigPropertyChangeInfo changeInfo = propertyChangeInfos.get(propertyKey);
        if (changeInfo != null) {
            return changeInfo.getOldValue();
        }
        return null;
    }

    public String getPropertyNewValue(String propertyKey) {
        ConfigPropertyChangeInfo changeInfo = propertyChangeInfos.get(propertyKey);
        if (changeInfo != null) {
            return changeInfo.getNewValue();
        }
        return null;
    }

    public ChangeType getPropertiesChangeType(String propertyKey) {
        ConfigPropertyChangeInfo changeInfo = propertyChangeInfos.get(propertyKey);
        if (changeInfo != null) {
            return changeInfo.getChangeType();
        }
        return ChangeType.NOT_CHANGED;
    }

}
