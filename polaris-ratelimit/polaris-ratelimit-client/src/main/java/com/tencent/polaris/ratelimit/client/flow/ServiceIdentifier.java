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

package com.tencent.polaris.ratelimit.client.flow;

import java.util.Objects;

public class ServiceIdentifier {

    private final String service;
    private final String namespace;
    private final String labels;

    public ServiceIdentifier(String service, String namespace, String labels) {
        this.service = service;
        this.namespace = namespace;
        this.labels = labels;
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ServiceIdentifier that = (ServiceIdentifier) o;
        return Objects.equals(service, that.service) &&
                Objects.equals(namespace, that.namespace) &&
                Objects.equals(labels, that.labels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(service, namespace, labels);
    }

    @Override
    public String toString() {
        return "ServiceIdentifier{" +
                "service='" + service + '\'' +
                ", namespace='" + namespace + '\'' +
                ", labels='" + labels + '\'' +
                '}';
    }
}
