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

package com.tencent.polaris.api.pojo;

import com.tencent.polaris.api.utils.CollectionUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class SourceService extends ServiceInfo {

    private final Set<RouteArgument> arguments = new HashSet<>();

    public Set<RouteArgument> getArguments() {
        return arguments;
    }

    public void appendArguments(RouteArgument argument) {
        arguments.add(argument);
    }

    public void setArguments(Set<RouteArgument> arguments) {
        if (!CollectionUtils.isEmpty(arguments)) {
            this.arguments.addAll(arguments);
        }
    }

    public Map<String, String> getLabels() {
        if (CollectionUtils.isEmpty(arguments)) {
            return Optional.ofNullable(super.getMetadata()).orElse(Collections.emptyMap());
        }

        Map<String, String> labels = new HashMap<>();
        arguments.forEach(entry -> entry.toLabel(labels));
        return labels;
    }

    /**
     * use {@link SourceService#setArguments(Set)} to replace {@link SourceService#setMetadata(Map)}
     *
     * @param metadata
     */
    @Deprecated
    @Override
    public void setMetadata(Map<String, String> metadata) {
        metadata.forEach((key, value) -> appendArguments(RouteArgument.fromLabel(key, value)));
    }

    @Deprecated
    @Override
    public Map<String, String> getMetadata() {
        return getLabels();
    }
}
