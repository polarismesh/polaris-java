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

package com.tencent.polaris.api.plugin.cache;

import com.tencent.polaris.api.utils.TrieUtil;
import com.tencent.polaris.specification.api.v1.model.ModelProto;

import static com.tencent.polaris.api.plugin.cache.CacheConstants.API_ID;

/**
 * @author Haotian Zhang
 */
public class FlowCacheUtils {

    public static void saveApiTrie(ModelProto.MatchString matchString, FlowCache flowCache) {
        if (matchString != null && matchString.getType() != ModelProto.MatchString.MatchStringType.REGEX) {
            if (matchString.getType() == ModelProto.MatchString.MatchStringType.EXACT || matchString.getType() == ModelProto.MatchString.MatchStringType.NOT_EQUALS) {
                flowCache.loadPluginCacheObject(API_ID, matchString.getValue().getValue(),
                        path -> TrieUtil.buildSimpleApiTrieNode((String) path));
            } else if (matchString.getType() == ModelProto.MatchString.MatchStringType.IN || matchString.getType() == ModelProto.MatchString.MatchStringType.NOT_IN) {
                String[] apis = matchString.getValue().getValue().split(",");
                for (String api : apis) {
                    flowCache.loadPluginCacheObject(API_ID, api,
                            path -> TrieUtil.buildSimpleApiTrieNode((String) path));
                }
            }
        }
    }
}
