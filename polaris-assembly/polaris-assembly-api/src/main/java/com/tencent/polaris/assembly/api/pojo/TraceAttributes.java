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

package com.tencent.polaris.assembly.api.pojo;

import java.util.Map;

public class TraceAttributes {

    /**
     * The location to put attributes
     */
    public enum AttributeLocation {
        SPAN,
        BAGGAGE
    }

    private Map<String, String> attributes;

    private AttributeLocation attributeLocation;

    private Object otScope;

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public AttributeLocation getAttributeLocation() {
        return attributeLocation;
    }

    public void setAttributeLocation(AttributeLocation attributeLocation) {
        this.attributeLocation = attributeLocation;
    }

    public Object getOtScope() {
        return otScope;
    }

    public void setOtScope(Object otScope) {
        this.otScope = otScope;
    }
}
