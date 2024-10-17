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

package com.tencent.polaris.api.rpc;

import java.util.HashMap;
import java.util.Map;

import com.tencent.polaris.api.pojo.InstanceWeight;

/**
 * 用户态的服务路由因子
 *
 * @author andrewshan
 * @date 2019/8/21
 */
public class Criteria {

    /**
     * 指定负载均衡策略
     */
    private String lbPolicy;
    /**
     * 一致性hash的key
     */
    private String hashKey;

    private Map<String, InstanceWeight> dynamicWeight;

    public String getHashKey() {
        return hashKey;
    }

    public void setHashKey(String hashKey) {
        this.hashKey = hashKey;
    }

    public String getLbPolicy() {
        return lbPolicy;
    }

    public void setLbPolicy(String lbPolicy) {
        this.lbPolicy = lbPolicy;
    }

    public Map<String, InstanceWeight> getDynamicWeight() {
        return dynamicWeight != null ? dynamicWeight : new HashMap<>();
    }

    public void setDynamicWeight(Map<String, InstanceWeight> dynamicWeight) {
        this.dynamicWeight = dynamicWeight;
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public String toString() {
        return "Criteria{" +
                "lbPolicy='" + lbPolicy + '\'' +
                ", hashKey='" + hashKey + '\'' +
                '}';
    }
}
