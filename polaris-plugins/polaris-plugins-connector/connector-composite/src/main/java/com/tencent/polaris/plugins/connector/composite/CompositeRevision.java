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

package com.tencent.polaris.plugins.connector.composite;

import com.google.common.collect.Lists;
import com.tencent.polaris.api.utils.StringUtils;
import java.util.List;

import static com.tencent.polaris.api.config.plugin.DefaultPlugins.SERVER_CONNECTOR_CONSUL;
import static com.tencent.polaris.api.config.plugin.DefaultPlugins.SERVER_CONNECTOR_GRPC;
import static com.tencent.polaris.api.config.plugin.DefaultPlugins.SERVER_CONNECTOR_NACOS;

/**
 * Revision handler for multi-discovery server.
 *
 * @author Haotian Zhang
 */
public class CompositeRevision {

    private static final List<String> ORDER_LIST = Lists.newArrayList(SERVER_CONNECTOR_GRPC, SERVER_CONNECTOR_CONSUL, SERVER_CONNECTOR_NACOS);

    private static final String BIG_SEPARATOR = ";";

    private static final String LIL_SEPARATOR = ":";

    private final String[] content = new String[ORDER_LIST.size()];

    /**
     * Set revision of corresponding server connector by name.
     *
     * @param name name of server connector
     * @param revision revision
     */
    public void setRevision(String name, String revision) {
        if (ORDER_LIST.contains(name)) {
            content[ORDER_LIST.indexOf(name)] = revision;
        }
    }

    /**
     * Generate composite revision string.
     *
     * @return revision
     */
    public String getCompositeRevisionString() {
        StringBuilder revision = new StringBuilder();
        for (int i = 0; i < ORDER_LIST.size(); i++) {
            if (StringUtils.isNotBlank(content[i])) {
                revision.append(ORDER_LIST.get(i));
                revision.append(LIL_SEPARATOR);
                revision.append(content[i]);
                revision.append(BIG_SEPARATOR);
            }
        }
        return revision.toString();
    }
}
