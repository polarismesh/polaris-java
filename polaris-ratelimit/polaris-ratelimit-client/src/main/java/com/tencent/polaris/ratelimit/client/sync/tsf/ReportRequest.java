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

package com.tencent.polaris.ratelimit.client.sync.tsf;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ReportRequest {

    @Expose
    @JsonProperty
    private List<RuleStatics> rates;

    public ReportRequest(List<RuleStatics> rates) {
        this.rates = rates;
    }

    public ReportRequest() {
    }

    @Override
    public String toString() {
        return "ReportRequest{" +
                "rates=" + rates +
                '}';
    }

    public List<RuleStatics> getRates() {
        return rates;
    }

    public void setRates(List<RuleStatics> rates) {
        this.rates = rates;
    }

    @JsonInclude
    public static class RuleStatics {

        @JsonProperty("rule_id")
        @Expose
        @SerializedName("rule_id")
        String ruleId = "";

        @JsonProperty
        @Expose
        long pass = 0;

        @JsonProperty
        @Expose
        long block = 0;

        public RuleStatics() {
        }

        public RuleStatics(String ruleId, long pass, long block) {
            this.ruleId = ruleId;
            this.pass = pass;
            this.block = block;
        }

        @Override
        public String toString() {
            return "RuleStatics{" +
                    "ruleId='" + ruleId + '\'' +
                    ", pass=" + pass +
                    ", block=" + block +
                    '}';
        }
    }
}
