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

package com.tencent.polaris.plugins.stat.common.model;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class StatRevisionMetric extends StatMetric {
    private final AtomicLong revision;

    public StatRevisionMetric(String metricName, Map<String, String> labels, Long signature, long revision) {
        super(metricName, labels, signature);
        this.revision = new AtomicLong(revision);
    }

    public long getRevision() {
        return revision.get();
    }

    public void setRevision(long revision) {
        this.revision.set(revision);
    }
}
