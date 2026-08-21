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

package com.tencent.polaris.configuration.client.internal;

import com.google.gson.annotations.SerializedName;

/**
 * 服务端 PUSH 下发的配置生效查询指令，snake_case 与服务端配置中心一致。
 *
 * @author evelynwei
 */
class ClientEventQuery {

    @SerializedName("kind")
    private String kind;

    @SerializedName("config")
    private QueryConfig config;

    String getKind() {
        return kind;
    }

    QueryConfig getConfig() {
        return config;
    }

    /**
     * 查询目标配置文件三元组。
     */
    static class QueryConfig {

        @SerializedName("namespace")
        private String namespace;

        @SerializedName("group")
        private String group;

        @SerializedName("file_name")
        private String fileName;

        String getNamespace() {
            return namespace;
        }

        String getGroup() {
            return group;
        }

        String getFileName() {
            return fileName;
        }
    }
}
