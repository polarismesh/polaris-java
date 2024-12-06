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

package com.tencent.polaris.plugins.connector.composite;

import com.tencent.polaris.api.utils.StringUtils;

import static com.tencent.polaris.plugins.connector.common.constant.ConnectorConstant.ORDER_LIST;

/**
 * Revision handler for multi-discovery server.
 *
 * @author Haotian Zhang
 */
public class CompositeRevision {

    private static final String BIG_SEPARATOR = ";";

    private static final String LIL_SEPARATOR = ":";

    private final String[] content = new String[ORDER_LIST.size()];

    /**
     * Set revision of corresponding server connector by name.
     *
     * @param name     name of server connector
     * @param revision revision
     */
    public void setRevision(String name, String revision) {
        if (ORDER_LIST.contains(name)) {
            content[ORDER_LIST.indexOf(name)] = revision;
        }
    }

    public String getRevision(String name) {
        if (ORDER_LIST.contains(name)) {
            return content[ORDER_LIST.indexOf(name)];
        }
        return "";
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

    public static CompositeRevision of(String revision) {
        CompositeRevision compositeRevision = new CompositeRevision();
        String[] bigs = revision.split(BIG_SEPARATOR);
        for (String big : bigs) {
            if (StringUtils.isNotBlank(big)) {
                String[] lils = big.split(LIL_SEPARATOR);
                if (lils.length == 2) {
                    compositeRevision.content[ORDER_LIST.indexOf(lils[0])] = lils[1];
                }
            }
        }
        return compositeRevision;
    }
}
