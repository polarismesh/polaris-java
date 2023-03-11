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


/**
 * @author lepdou 2022-03-01
 */
public class ConfigFileChangeEvent {

    private final ConfigFileMetadata configFileMetadata;
    private final String             oldValue;
    private final String             newValue;
    private final ChangeType         changeType;


    public ConfigFileChangeEvent(ConfigFileMetadata configFileMetadata,
                                 String oldValue, String newValue, ChangeType changeType) {
        this.configFileMetadata = configFileMetadata;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.changeType = changeType;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    @Override
    public String toString() {
        return "ConfigFileChangeEvent{" +
               "configFileMetadata=" + configFileMetadata +
               ", oldValue='" + oldValue + '\'' +
               ", newValue='" + newValue + '\'' +
               ", changeType=" + changeType +
               '}';
    }
}
