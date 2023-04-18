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

package com.tencent.polaris.assembly.api.pojo;

import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.circuitbreak.api.pojo.CheckResult;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResponse;
import com.tencent.polaris.router.api.rpc.ProcessRoutersResponse;

public class BeforeResponse extends AttachmentBaseEntity {

    private final RetStatus retStatus;

    private final CheckResult checkResult;

    private final InstancesResponse instancesResponse;

    private final ProcessRoutersResponse processRoutersResponse;

    private final Instance selectedInstance;

    private final QuotaResponse quotaResponse;

    public BeforeResponse(RetStatus retStatus, CheckResult checkResult,
            InstancesResponse instancesResponse,
            ProcessRoutersResponse processRoutersResponse, Instance selectedInstance,
            QuotaResponse quotaResponse) {
        this.retStatus = retStatus;
        this.checkResult = checkResult;
        this.instancesResponse = instancesResponse;
        this.processRoutersResponse = processRoutersResponse;
        this.selectedInstance = selectedInstance;
        this.quotaResponse = quotaResponse;
    }

    public RetStatus getRetStatus() {
        return retStatus;
    }

    public CheckResult getCheckResult() {
        return checkResult;
    }

    public InstancesResponse getInstancesResponse() {
        return instancesResponse;
    }

    public ProcessRoutersResponse getProcessRoutersResponse() {
        return processRoutersResponse;
    }

    public Instance getSelectedInstance() {
        return selectedInstance;
    }

    public QuotaResponse getQuotaResponse() {
        return quotaResponse;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private RetStatus retStatus;

        private CheckResult checkResult;

        private InstancesResponse instancesResponse;

        private ProcessRoutersResponse processRoutersResponse;

        private Instance selectedInstance;

        private QuotaResponse quotaResponse;

        public Builder setRetStatus(RetStatus retStatus) {
            this.retStatus = retStatus;
            return this;
        }

        public Builder setCheckResult(CheckResult checkResult) {
            this.checkResult = checkResult;
            return this;
        }

        public Builder setInstancesResponse(InstancesResponse instancesResponse) {
            this.instancesResponse = instancesResponse;
            return this;
        }

        public Builder setProcessRoutersResponse(ProcessRoutersResponse processRoutersResponse) {
            this.processRoutersResponse = processRoutersResponse;
            return this;
        }

        public Builder setSelectedInstance(Instance selectedInstance) {
            this.selectedInstance = selectedInstance;
            return this;
        }

        public Builder setQuotaResponse(QuotaResponse quotaResponse) {
            this.quotaResponse = quotaResponse;
            return this;
        }

        public BeforeResponse build() {
            return new BeforeResponse(
                    retStatus, checkResult, instancesResponse, processRoutersResponse, selectedInstance, quotaResponse);
        }
    }
}
