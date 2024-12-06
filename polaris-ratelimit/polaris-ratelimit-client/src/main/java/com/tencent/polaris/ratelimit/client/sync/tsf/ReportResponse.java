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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ReportResponse {
    @JsonProperty
    @Expose
    private List<Limit> limits;

    public ReportResponse(List<Limit> limits) {
        this.limits = limits;
    }

    public ReportResponse() {
    }

    @Override
    public String toString() {
        return "ReportResponse{" + "limits=" + limits + '}';
    }

    public List<Limit> getLimits() {
        return limits;
    }

    public void setLimits(List<Limit> limits) {
        this.limits = limits;
    }

    public static class Limit {
        @JsonProperty("rule_id")
        @Expose
        @SerializedName("rule_id")
        private String ruleId = "";

        @JsonProperty("rate")
        @Expose
        @SerializedName("rate")
        private int quota = 0;

        public Limit() {
        }

        public Limit(String ruleId, int quota) {
            this.ruleId = ruleId;
            this.quota = quota;
        }

        public String getRuleId() {
            return ruleId;
        }

        public void setRuleId(String ruleId) {
            this.ruleId = ruleId;
        }

        public int getQuota() {
            return quota;
        }

        public void setQuota(int quota) {
            this.quota = quota;
        }

        @Override
        public String toString() {
            return "Limit{" + "ruleId='" + ruleId + '\'' + ", quota=" + quota + '}';
        }
    }
}
