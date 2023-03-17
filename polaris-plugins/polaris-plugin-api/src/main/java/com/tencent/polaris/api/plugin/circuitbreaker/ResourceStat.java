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

package com.tencent.polaris.api.plugin.circuitbreaker;

import com.tencent.polaris.api.plugin.circuitbreaker.entity.Resource;
import com.tencent.polaris.api.pojo.RetStatus;

public class ResourceStat {

    private final Resource resource;

    private final int retCode;

    private final long delay;

    private final RetStatus retStatus;

    public ResourceStat(Resource resource, int retCode, long delay, RetStatus retStatus) {
        this.resource = resource;
        this.retCode = retCode;
        this.delay = delay;
        this.retStatus = retStatus;
    }

    public ResourceStat(Resource resource, int retCode, long delay) {
        this(resource, retCode, delay, RetStatus.RetUnknown);
    }

    public Resource getResource() {
        return resource;
    }

    public int getRetCode() {
        return retCode;
    }

    public long getDelay() {
        return delay;
    }

    public RetStatus getRetStatus() {
        return retStatus;
    }

    @Override
    public String toString() {
        return "ResourceStat{" +
                "resource=" + resource +
                ", retCode=" + retCode +
                ", delay=" + delay +
                ", retStatus=" + retStatus +
                '}';
    }
}
