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

package com.tencent.polaris.api.pojo;

/**
 * Dynamic weight for an instance.
 */
public class InstanceWeight {
    // Instance id
    private String id;
    // Current weight
    private int dynamicWeight;
    // Base weight
    private int baseWeight;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getDynamicWeight() {
        return dynamicWeight;
    }

    public void setDynamicWeight(int dynamicWeight) {
        this.dynamicWeight = dynamicWeight;
    }

    public int getBaseWeight() {
        return baseWeight;
    }

    public void setBaseWeight(int baseWeight) {
        this.baseWeight = baseWeight;
    }

    public boolean isDynamicWeightValid() {
        return dynamicWeight != baseWeight;
    }

    @Override
    public String toString() {
        return "InstanceWeight{" +
                "id='" + id + '\'' +
                ", dynamicWeight=" + dynamicWeight +
                ", baseWeight=" + baseWeight +
                '}';
    }
}
