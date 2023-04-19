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
import com.tencent.polaris.api.utils.CollectionUtils;
import java.util.HashMap;
import java.util.Map;

public class BeforeResponse extends AttachmentBaseEntity {

    private final RetStatus retStatus;

    private final Instance selectedInstance;
    
    private final Map<Capability, Object> capabilityResponses = new HashMap<>();

    public BeforeResponse(RetStatus retStatus, Instance selectedInstance, Map<Capability, Object> capabilityResponses) {
        this.retStatus = retStatus;
        this.selectedInstance = selectedInstance;
        if (!CollectionUtils.isEmpty(capabilityResponses)) {
            this.capabilityResponses.putAll(capabilityResponses);
        }
    }

    public RetStatus getRetStatus() {
        return retStatus;
    }


    public Instance getSelectedInstance() {
        return selectedInstance;
    }
    @SuppressWarnings("unchecked")
    public <T> T getCapabilityResponse(Capability capability) {
        if (capabilityResponses.containsKey(capability)) {
            return (T)capabilityResponses.get(capability);
        }
        return null;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private RetStatus retStatus;

        private final Map<Capability, Object> capabilityResponses = new HashMap<>();

        private Instance selectedInstance;

        public Builder setRetStatus(RetStatus retStatus) {
            this.retStatus = retStatus;
            return this;
        }

        public Builder putCapabilityResponse(Capability capability, Object response) {
            capabilityResponses.put(capability, response);
            return this;
        }

        public Builder setSelectedInstance(Instance selectedInstance) {
            this.selectedInstance = selectedInstance;
            return this;
        }

        public BeforeResponse build() {
            return new BeforeResponse(retStatus, selectedInstance, capabilityResponses);
        }
    }
}
