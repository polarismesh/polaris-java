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

package com.tencent.polaris.api.rpc;

/**
 * 元数据路由的降级类型
 *
 * @author starkwen
 * @date 2021/2/25 下午3:32
 */
public enum MetadataFailoverType {

    /**
     * 默认不降级
     */
    METADATAFAILOVERNONE("metadataFailoverNone"),

    /**
     * 降级返回所有节点
     */
    METADATAFAILOVERALL("metadataFailoverAll"),

    /**
     * 返回不包含元数据路由key的节点
     */
    METADATAFAILOVERNOTKEY("metadataFailoverNoKey");

    String name;

    MetadataFailoverType(String name) {
        this.name = name;
    }


    public String getName() {
        return name;
    }

    public static MetadataFailoverType getByName(String name) {
        for (MetadataFailoverType value : values()) {
            if (value.getName().equals(name)) {
                return value;
            }
        }
        return METADATAFAILOVERNONE;
    }
}
